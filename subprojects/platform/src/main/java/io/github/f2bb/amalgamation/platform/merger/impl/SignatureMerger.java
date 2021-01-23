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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.f2bb.amalgamation.platform.util.ClassInfo;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.ClassNode;

public class SignatureMerger extends SignatureWriter implements Merger {
	private ClassNode root;

	private String superClassSign;
	private Set<String> interfaceSign = new HashSet<>();
	private String interfaceStack;

	public SignatureMerger() {
		this(null);
	}

	public SignatureMerger(ClassNode root) {
		this.root = root;
	}

	@Override
	public void merge(ClassNode node, List<ClassInfo> infos) {
		SignatureMerger writer = new SignatureMerger(node);
		for (ClassInfo info : infos) {
			String sign = info.node.signature;
			if (sign != null) {
				// todo implement formal type parameters
				SignatureReader reader = new SignatureReader(sign);
				reader.accept(writer);
				break;
			}
		}


		// basically you map from identifier::signature -> list<classinfo>
		// and then take the 'gcd' of the bounds
		// if a type parameter is added, include it, we need to find out how to strip them though...
		boolean special = false;
		StringBuilder sign = new StringBuilder(writer.toString());

		if (sign.length() != 0) {
			special = true;
			sign.append('>');
		}

		if (writer.superClassSign != null) {
			if (writer.superClassSign.contains("<")) {
				special = true;
			}
			sign.append(writer.superClassSign);
		} else {
			sign.append("Ljava/lang/Object;");
		}


		for (String s : writer.interfaceSign) {
			if (s.contains("<")) {
				special = true;
			}
			sign.append(s);
		}

		if (special) {
			node.signature = sign.toString();
		}
	}

	@Override
	public SignatureVisitor visitSuperclass() {
		return new SignatureWriter() {
			@Override
			public void visitEnd() {
				super.visitEnd();
				String str = this.toString();
				if (str.startsWith("L" + SignatureMerger.this.root.superName)) {
					// this'll get overriden multiple times if there are type parameters, that's ok
					SignatureMerger.this.superClassSign = str;
				}
			}
		};
	}

	@Override
	public SignatureVisitor visitInterface() {
		if (this.interfaceStack != null) {
			this.interfaceSign.add(this.interfaceStack);
			this.interfaceStack = null;
		}

		// pain I fucking hate asm signature api, I'll have to write my own parser later
		return new SignatureWriter() {
			@Override
			public void visitEnd() {
				super.visitEnd();
				SignatureMerger.this.interfaceStack = this.toString();
			}
		};
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		if (this.interfaceStack != null) {
			this.interfaceSign.add(this.interfaceStack);
			this.interfaceStack = null;
		}
	}
}
