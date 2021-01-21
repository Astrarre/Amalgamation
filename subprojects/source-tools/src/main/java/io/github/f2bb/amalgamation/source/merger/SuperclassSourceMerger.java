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

package io.github.f2bb.amalgamation.source.merger;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.github.f2bb.amalgamation.Parent;

import java.util.*;

public enum SuperclassSourceMerger implements SourceMerger<ClassOrInterfaceDeclaration> {

    INSTANCE;

    @Override
    public boolean isApplicable(TypeDeclaration<?> typeDeclaration) {
        return typeDeclaration instanceof ClassOrInterfaceDeclaration && !((ClassOrInterfaceDeclaration) typeDeclaration).isInterface();
    }

    @Override
    public void merge(SourceHelper helper, ClassOrInterfaceDeclaration node, Set<SourceInfo<ClassOrInterfaceDeclaration>> infos) {
        Map<ClassOrInterfaceType, List<SourceInfo<ClassOrInterfaceDeclaration>>> supers = new HashMap<>();

        for (SourceInfo<ClassOrInterfaceDeclaration> info : infos) {
            supers.computeIfAbsent(info.getTypeDeclaration().getExtendedTypes(0), s -> new ArrayList<>()).add(info);
        }

        // most common super class, this gets priority and is what is shown in the source
        ClassOrInterfaceType mostCommon = null;
        int count = 0;

        for (ClassOrInterfaceType s : supers.keySet()) {
            int size = supers.get(s).size();

            if (size > count) {
                mostCommon = s;
                count = size;
            }
        }

        if (mostCommon == null && count == 0) {
            throw new IllegalStateException("no classes! " + supers);
        }

        node.addExtendedType(mostCommon);
        supers.remove(mostCommon);

        if (!supers.isEmpty()) {
            supers.forEach((type, platforms) -> {
                ArrayInitializerExpr array = new ArrayInitializerExpr();

                for (SourceInfo<ClassOrInterfaceDeclaration> info : platforms) {
                    array.getValues().add(info.createPlatformAnnotation(helper.bind(node)));
                }

                NormalAnnotationExpr annotation = new NormalAnnotationExpr(helper.addImport(node, Parent.class), new NodeList<>());
                annotation.addPair("parent", new ClassExpr(type));
                annotation.addPair("platform", array);
                node.getAnnotations().add(annotation);
            });
        }
    }
}
