# Amalgamation
A single jar that you can compile against that contains the code of Forge, CraftBukkit and Vanilla

## Remapping / Deobfuscation
This allows for the deobfuscation of artifacts using mappings files. It uses a fork of fabric's
tiny-remapper to remap class files, the trieHarder remapper to map sources
(essentially a find-replace, not great for anything other than intermediary sources (eg. fabric mods)),
the access-widener remapper to remap access widener files (though it wont read the fabric.mod.json to find it)
and a meta-inf patcher to fix signatures and the like, as the class's hash will change after being remapped
Any format recognized by mappings-io will work.
```groovy
def remappedDependencyA
implementation ag.map { // add all remapped dependencies to the implementation configuration
    // optional
    classpath("org.example:dependencyATransitive:xyz") // adds a dependency to the remap classpath
    // classpath ag.libraries("1.16.5") // minecraft's dependencies for the given version (though this is usually uneeded)
    removeRemapper(0) // removes tiny remapper
    tinyRemapperWithUnpick() // tiny remapper but with the in-house unpick extension (doesn't work perfectly though)
    
    // mandatory
    mappings("org.example:intermediary:xyz", "official", "named") // can be called multiple times for layered mappings
    // mappings(ag.intermediary("1.16.5")) // fabricmc intermediary
    // caches the remapped dependency in the root project's build folder
    remappedDependencyA = inputLocal("org.example:dependencyA:xyz")
    // caches the remapped dependency in gradle's cache folder
    inputGlobal("org.example:dependencyB:xyz")
}
shade remappedDependencyA // shade just the remapped version of dependencyA
```

### Remapping Output Artifacts
```groovy
task productionJar(type: RemapJar) { // remaps classes
    // regular Jar task configuration
    with jar
    classifier = 'fabric'
    // custom
    classpath = configurations.compileClasspath // sets the classpath required for remapping
    remapAw() // remaps access wideners
    useExperimentalMixinRemapper() // remaps mixins without needing the annotation processor
    mappings(mergedMappings, 'named', 'intermediary') // adds the mappings to be used (can be called multiple times, useful when using annotation processor)
}

task sourcesJar(type: RemapSourcesJar, dependsOn: classes) { // remaps sources
    classifier = 'sources'
    from sourceSets.main.allSource
    classpath = configurations.compileClasspath
    mappings(mergedMappings, 'named', 'intermediary')
    
    // can't remap access wideners and mixins in sources because it's really not important
}

// RemapAllJars is useful when multiple RemapJar tasks share similar classpaths
// instead of having to read the entire classpath each time, it only has to do it once
// when any of the remapJar tasks are executed, it will do nothing and finalize with the RemapAllJars task
// which when run will actually do the remapping
// you can also call the RemapAllJars task directly
task remapAll(type: RemapAllJars) {
    addTask(productionJar)
    addTask(subproject.tasks.productionJar)
}

// same deal for sources
task remapAllSources(type: RemapAllSourcesJars) {
    addTask(sourcesJar)
}
```

## Publishing
Amalgamation mangles the name and version of artifacts, so when publishing the pom.xml will contain invalid names
this can be rectified as follows:
```groovy
publishing {
    publications {
        myPublication(MavenPublication) {
            from components.java
            // optional (order is important)

            // removes any dependencies in the configuration from the pom, this is useful when publishing artifacts compiled against minecraft, 
            // since those dependencies are automatically always included, you should add minecraft's dependencies and minecraft itself into it's own configuration
            // and exclude it
            ag.excludeConfiguration(it, configurations.commonDependencies)

            setArtifacts([]) // removes the default jar artifacts and whatnot from the publication
            
            // mandatory
            
            ag.fixPom(it) // demangles artifact names
            
            artifact(remapJar) {
                builtBy remapJar // even when using remapAllJars it should work
            }
        }
    }
}
```

## Decompilation
Amalgamation's decompilation is very configurable.
It uses a decompiler (eg. fernflower) to decompile a jar's classes into source files for IDE indexing.
If the decompiler used is fabric fernflower or a fork, then you can optionally include mappings,
which will allow the decompiler to add javadocs into the generated source code.
```groovy
def decompiledDependencyA
def decompiledDependencyB

ag.decompile {
    // optional
    optionalTask("decompileDependencies") // does not decompile until this task is run (it creates one with that name) 
    decompilerClasspath("org.example:additionalDependency:xyz") // adds a dependency to the decompiler's classpath
    classpath("org.example:dependencyATransitive:xyz") // adds a dependency to the decompilation environment classpath
    
    // mandatory
    // here, you can chose the decompiler, in this example we are using fabric-flower, to showcase the javadoc insertion ability
    fernflower("net.fabricmc:fabric-fernflower:1.4.1")
    
    // caches the decompiled dependency in the root project's build folder
    decompiledDependencyA = inputLocal("org.example:dependencyA:xyz")
    // caches the decompiled dependency in the gradle cache folder
    decompiledDependencyB = inputGlobal("org.example:dependencyB:xyz")
    
    classpath libs
    javadocs('net.fabricmc:yarn:1.17.1+build.61:v2', 'named')
}

// I seperated out each decompile dependency to showcase that this is possible, u may want to seperate
// them out in the event one dependency belongs to a different configuration, however if 
// u don't need this, then you can still do "implementation ag.decompile {...}"
implementation decompiledDependencyA
compileOnly decompiledDependencyB
```

## MojMerger
"MojMerger" is short for "Mojang Mappings Merger", and it's functionality is similar to that of the CAS Merger.
However, instead of requiring the server jar, it instead downloads the official deobfuscation mappings for the server
provided by mojang, and uses that to determine what members are present on the server.
It then uses a clientside mapping to determine what members are present on the client, this is to
avoid needing to read the entire jar first, since mappings have to be parsed later on anyways (remapping)
This is much faster than CASMerging, and should be used when preferable. MojMerge provided jars are also
automatically "split" in order to improve remapping performance.

```groovy
import io.github.astrarre.amalgamation.gradle.dependencies.cas_merger.SideAnnotationHandler
implementation ag.mojmerged("1.17.1") // uses fabricmc annotations and fabricmc intermediary mappings
implementation ag.mojmerged(
        "1.7.10",
        new SideAnnotationHandler() {/*...*/},
        mappings("org.example:customIntermediary:xyz", "notch", "intermediate")
)
```

## IDE configuration
intellij's idea-ext plugin is excruciatingly slow and doesn't seem to work, so amalgamation provides
run configurations generation. At the moment only intellij run configurations are implemented, since
that's what I use, and am too lazy to test outside my ide of comfort.
```groovy
plugins {
    // ...
    id 'idea' // the intellij extension will only be applied if the idea plugin is applied
}

task runTask(type: JavaExec) {
    // ...
    // mandatory
    main = "..."
}

ag.idea().java(aaaaa) {
    // recommended
    overrideClasspath(project, sourceSets.main) // if you set your javaexec classpath to `sourceSets.XXX.runtimeClasspath` you can use this instead, intellij will cry about "module not specified" otherwise (though the task will still work)
    excludeDependency(tasks.classes) // if the javaexec task depends on another, u can exclude the other task from being included in the run configuration, for example, in this case we want to build with intellij, which will run the classes task (or use it's own compiler) on it's own, and therefor we don't need to run it prior the task ourselves
    // optional
    setJvmVersion("8") // sets the jvm version, has to be something intellij will recognize though
    setShorten("MANIFEST_JAR") // when the classpath of a task is very large, you may run into the command size limit, this will make amalgamation generate a jar file with a manifest that points to all the jars on the classpath
    setCustomName("runTaskIntellij") // set the run configuration's name
    setCustomPath("runtask") // change the name of the generated run configuration file
}
```

## AccessWidener
This allows u to increase the visibility of members and classes in a dependency.
For example, if a method you want to access is private, you can use an [access-widener](https://github.com/FabricMC/access-widener) to make it public
and then shade the widened version of the dependency, or if you're using fabric-loader,
then u can just add it to your  fabric.mod.json and fabric-loader will apply the change at runtime.
```groovy
implementation(shade(ag.accessWidener("org.example:myDependency:xyz") {
    accessWidener("myAccessWidener.aw")
}))
```

## URL
for some reason gradle doesn't allow you to have a dependency on a url, but amalgamation does
```groovy
implementation ag.url("https://myjar.com/jar_jar_binks.jar")
```

## Other Minecraft Misc
```groovy
// gets all of minecraft's dependencies for that version
implementation ag.libraries("1.17.1")

implementation(ag.gradleFriendlyLibraries("1.17.1")) { // useful for dependency replacement, though sources may not attach
    exclude group: "com.mojang"
}

task runClient(type: JavaExec) {
    classpath sourceSets.main.runtimeClasspath
    main = "net.fabricmc.loader.launch.knot.KnotClient"
    def natives = ag.natives("1.17.1") // creates and returns the path to the natives directory (idk it's this weird thing don't ask)
    systemProperty("fabric.development", true)
    systemProperty("fabric.gameVersion", mc_vers)
    systemProperty("java.library.globalCache", natives)
    systemProperty("org.lwjgl.librarypath", natives)
    def assets = ag.assets("1.16.5") // creates and returns the path to the assets directory
    args("--assetIndex", assets.getAssetIndex(), "--assetsDir", assets.getAssetsDir())
    workingDir("$rootDir/run")
    dependsOn tasks.classes
}
```

## CAS Merger
This stands for **C.lient A.nd S.erver** merger, and combines 2 dependencies into one,
if a class or  method or field is only present in one of the two dependencies, it is annotated accordingly.
The exact annotation can be configured by setting the SideAnnotationHandler
```groovy
import io.github.astrarre.amalgamation.gradle.dependencies.cas_merger.SideAnnotationHandler
implementation ag.merged("1.17.1") { // the version becomes the maven artifact's version
    // optional
    global = false // cache locally
    checkForServerOnly = true // by default the CAS merger does not check if there are server-specific members (since it's not needed for minecraft at the time of this writing)
    handler = new SideAnnotationHandler() {/*...*/}
    // mandatory
    client = "org.example:util-client:xyz"
    server = "org.example:util:xyz"
}
```
It should be noted that this will also merge any dependencies into the same jar,
so if u wish to only merge the direct dependencies, you should disable transitiveness

