/*
 * Artifactural
 * Copyright (c) 2018-2021.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.artifactural.base.cache;

import net.minecraftforge.artifactural.api.artifact.Artifact;
import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier;
import net.minecraftforge.artifactural.base.util.PatternReplace;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

// Why? I don't know
public class LocatedArtifactCache extends ArtifactCacheBase {
    private static final String PATTERN = "[group]/[name](/[meta_hash])/[version]/[name]-[version](-[classifier])(-[specifier]).[extension]";
    private final File path;

    public LocatedArtifactCache(File path) {
        this.path = path;
    }

    @Override
    public Artifact.Cached store(Artifact artifact) {
        return doStore(getPath(artifact), artifact);
    }

    public File getPath(Artifact artifact) {
        ArtifactIdentifier identifier = artifact.getIdentifier();
        Map<String, String> names = new HashMap<>();

        names.put("group", identifier.getGroup());
        names.put("name", identifier.getName());
        names.put("version", identifier.getVersion());
        names.put("classifier", identifier.getClassifier());
        names.put("extension", identifier.getExtension());
        names.put("meta_hash", artifact.getMetadata().getHash());

        return new File(path, PatternReplace.replace(PATTERN, names));
    }

    @Override
    public String toString() {
        return "LocatedArtifactCache(" + path + ")";
    }
}
