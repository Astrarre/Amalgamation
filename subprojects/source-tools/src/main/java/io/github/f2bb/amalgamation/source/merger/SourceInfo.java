/*
 * Amalgamation
 * Copyright (C) 2020 Astrarre
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

import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.github.f2bb.amalgamation.Platform;

import java.util.Objects;
import java.util.Set;

public class SourceInfo<T extends TypeDeclaration<T>> {

    private final Set<String> name;
    private final T typeDeclaration;

    public SourceInfo(Set<String> name, T typeDeclaration) {
        this.name = name;
        this.typeDeclaration = typeDeclaration;
    }

    public Set<String> getName() {
        return name;
    }

    public T getTypeDeclaration() {
        return typeDeclaration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceInfo<?> that = (SourceInfo<?>) o;
        return Objects.equals(name, that.name) && Objects.equals(typeDeclaration, that.typeDeclaration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, typeDeclaration);
    }

    @Override
    public String toString() {
        return "SourceInfo{" +
                "name=" + name +
                ", typeDeclaration=" + typeDeclaration +
                '}';
    }

    public AnnotationExpr createPlatformAnnotation(SourceHelper.Bound helper) {
        ArrayInitializerExpr array = new ArrayInitializerExpr();

        for (String n : name) {
            array.getValues().add(new StringLiteralExpr(n));
        }

        return new SingleMemberAnnotationExpr(helper.addImport(Platform.class), array);
    }
}
