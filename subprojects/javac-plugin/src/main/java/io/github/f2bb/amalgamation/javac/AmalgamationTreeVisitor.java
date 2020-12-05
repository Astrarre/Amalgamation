/*
 * Amalgamation
 * Copyright (C) 2020 IridisMC
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

package io.github.f2bb.amalgamation.javac;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import io.github.f2bb.amalgamation.javac.reflect.JavacList;
import io.github.f2bb.amalgamation.javac.reflect.LiveFieldReference;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class AmalgamationTreeVisitor extends TreeScanner<Object, Object> {

    private final AmalgamationPlatformChecker checker;

    public AmalgamationTreeVisitor(AmalgamationPlatformChecker checker) {
        this.checker = checker;
    }

    @Override
    public Object scan(Tree var1, Object o) {
        if (var1 != null) {
            for (Field field : var1.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType() != List.class) {
                    continue;
                }

                try {
                    JavacList.removeIf(new LiveFieldReference<>(var1, field), this::isInvalid);
                } catch (Exception ignored) {
                }
            }
        }

        return super.scan(var1, o);
    }

    @Override
    public Object visitClass(ClassTree classTree, Object o) {
        // TODO: Superclass
        return super.visitClass(classTree, o);
    }

    private boolean isInvalid(JCTree tree) {
        if (tree instanceof JCTree.JCClassDecl) {
            return checker.isInvalid(((JCTree.JCClassDecl) tree).mods.getAnnotations());
        } else if (tree instanceof JCTree.JCMethodDecl) {
            return checker.isInvalid(((JCTree.JCMethodDecl) tree).mods.getAnnotations());
        } else if (tree instanceof JCTree.JCVariableDecl) {
            return checker.isInvalid(((JCTree.JCVariableDecl) tree).mods.getAnnotations());
        } else if (tree instanceof JCTree.JCAnnotatedType) {
            return checker.isInvalid(((JCTree.JCAnnotatedType) tree).getAnnotations());
        } else {
            return false;
        }
    }
}
