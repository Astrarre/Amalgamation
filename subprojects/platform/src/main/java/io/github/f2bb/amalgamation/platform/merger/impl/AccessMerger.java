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

package io.github.f2bb.amalgamation.platform.merger.impl;

import io.github.f2bb.amalgamation.Access;
import io.github.f2bb.amalgamation.Platform;
import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Modifier;
import java.util.*;

public class AccessMerger implements @Platform({"fabric"}) Merger {

    public static final String ACCESSES = Type.getDescriptor(Access.Accesses.class);
    public static final String ACCESS = Type.getDescriptor(Access.class);

    @Override
    public void merge(ClassNode node, List<ClassInfo> infos) {
        Map<Integer, List<ClassInfo>> accessFlags = new HashMap<>();
        for (ClassInfo info : infos) {
            accessFlags.computeIfAbsent(info.node.access, s -> new ArrayList<>()).add(info);
        }

        int widest = getWidest(accessFlags);
        accessFlags.remove(widest);
        node.access = widest;

        if (!accessFlags.isEmpty()) {
            if (node.visibleAnnotations == null) node.visibleAnnotations = new ArrayList<>();
            node.visibleAnnotations.addAll(visit(accessFlags));
        }
    }

    @Override
    public boolean strip(ClassNode in, Set<String> available) {
        return false;
    }

    public static List<AnnotationNode> visit(Map<Integer, List<ClassInfo>> accessFlags) {
        List<AnnotationNode> node = new ArrayList<>();
        accessFlags.forEach((access, info) -> {
            AnnotationNode accessAnnotation = new AnnotationNode(ACCESS);
            AnnotationVisitor platforms = accessAnnotation.visitArray("platforms");
            for (ClassInfo classInfo : info) {
                platforms.visit("platforms", classInfo.createPlatformAnnotation());
            }
            platforms.visitEnd();

            AnnotationVisitor flags = accessAnnotation.visitArray("flags");
            if (Modifier.isPublic(access)) {
                flags.visit("flags", "public");
            } else if (Modifier.isProtected(access)) {
                flags.visit("flags", "protected");
            } else if (Modifier.isPrivate(access)) {
                flags.visit("flags", "private");
            } else {
                flags.visit("flags", "package-private");
            }

            if (Modifier.isInterface(access)) {
                flags.visit("flags", "interface");
            } else if (Modifier.isAbstract(access)) {
                flags.visit("flags", "abstract class");
            } else if ((ACC_ENUM & access) != 0) {
                flags.visit("flags", "enum");
            } else if ((ACC_ANNOTATION & access) != 0) {
                flags.visit("flags", "@interface");
            } else {
                flags.visit("flags", "class");
            }

            if (Modifier.isFinal(access)) {
                flags.visit("flags", "final");
            }

            if (Modifier.isStatic(access)) {
                flags.visit("flags", "static");
            }

            if (Modifier.isSynchronized(access)) {
                flags.visit("flags", "synchronized");
            }

            if (Modifier.isVolatile(access)) {
                flags.visit("flags", "volatile");
            }

            if (Modifier.isTransient(access)) {
                flags.visit("flags", "transient");
            }

            if (Modifier.isNative(access)) {
                flags.visit("flags", "native");
            }

            flags.visitEnd();
            accessAnnotation.visitEnd();
            node.add(accessAnnotation);
        });
        return node;
    }

    public static int getWidest(Map<Integer, List<ClassInfo>> accessFlags) {
        int widest = 0;
        for (int access : accessFlags.keySet()) {
            if (Modifier.isPublic(access)) {
                widest = access;
            } else if (Modifier.isProtected(access) && !Modifier.isPublic(widest)) {
                widest = access;
            } else if (widest == 0) {
                widest = access;
            }
        }
        return widest;
    }
}
