/*
 * Amalgamation
 * Copyright (C) 2021 Astrarre
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.f2bb.amalgamation.gradle.tasks;

import io.github.astrarre.api.PlatformId;
import io.github.astrarre.splitter.Splitter;
import io.github.astrarre.splitter.impl.AccessSplitter;
import io.github.astrarre.splitter.impl.ClassSplitter;
import io.github.astrarre.splitter.impl.HeaderSplitter;
import io.github.astrarre.splitter.impl.InnerClassAttributeSplitter;
import io.github.astrarre.splitter.impl.InterfaceSplitter;
import io.github.astrarre.splitter.impl.SignatureSplitter;
import io.github.astrarre.splitter.impl.SuperclassSplitter;
import io.github.f2bb.amalgamation.gradle.util.DelegatedFilterReader;
import org.gradle.api.tasks.Input;
import org.gradle.jvm.tasks.Jar;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class StripJar extends Jar {
    private final List<String> platforms = new ArrayList<>();
    public StripJar() {
        getMainSpec().appendCachingSafeCopyAction(fileCopyDetails -> {
            if (!fileCopyDetails.getName().endsWith(".class")) {
                return;
            }

            ClassNode node = new ClassNode();

            try {
                new ClassReader(fileCopyDetails.open()).accept(node, 0);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

            List<Splitter> splitters = new ArrayList<>();
            splitters.add(new AccessSplitter(null));
            splitters.add(new ClassSplitter(null));
            splitters.add(new HeaderSplitter(null));
            splitters.add(new InnerClassAttributeSplitter(null));
            splitters.add(new InterfaceSplitter(null));
            splitters.add(new SignatureSplitter(null));
            splitters.add(new SuperclassSplitter(null));
            ClassNode split = new ClassNode();
            for (Splitter splitter : splitters) {
                if(splitter.split(node, new PlatformId(this.platforms), split)) {
                    fileCopyDetails.exclude();
                    return;
                }
            }
            ClassWriter writer = new ClassWriter(0);
            split.accept(writer);

            Map<String, Object> properties = new HashMap<>();
            properties.put("data", new InputStreamReader(new ByteArrayInputStream(writer.toByteArray())));
            fileCopyDetails.filter(properties, DelegatedFilterReader.class);
        });
    }

    @Input
    public List<String> getPlatforms() {
        return platforms;
    }

    public void platform(Object platform) {
        if (platform instanceof Collection) {
            for (Object o : ((Collection<?>) platform)) {
                platform(o);
            }
        } else {
            platforms.add(String.valueOf(platform));
        }
    }
}
