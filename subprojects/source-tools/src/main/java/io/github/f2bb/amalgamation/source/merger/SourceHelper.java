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

import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;

public interface SourceHelper {

    Name addImport(TypeDeclaration<?> typeDeclaration, Class<?> type);

    default Bound bind(TypeDeclaration<?> typeDeclaration) {
        return type -> SourceHelper.this.addImport(typeDeclaration, type);
    }

    interface Bound {
        Name addImport(Class<?> type);
    }
}
