package io.github.astrarre.api.classpath;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.github.astrarre.merger.Mergers;
import org.objectweb.asm.tree.ClassNode;

public interface Context {
	ClassNode getClass(String internalName);

	default TypeDeclaration<?> getJava(String internalName) {
		return this.getJava(new ClassSourceInfo(internalName, null, 0, null, null, null));
	}

	/**
	 * can use additional context to find
	 */
	TypeDeclaration<?> getJava(ClassSourceInfo node);

	InputStream getResource(String fileName);

	default byte[] getResourceAsByteArray(String fileName) throws IOException {
		InputStream stream = this.getResource(fileName);
		if(stream == null) return null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Mergers.copy(stream, baos);
		return baos.toByteArray();
	}

	default ClassSourceInfo get(ClassNode node) {
		String outer = node.outerMethodDesc;
		if (outer != null) {
			String name = node.name;
			int lastName = name.lastIndexOf('$');
			String subStr = name.substring(lastName + 1);
			try {
				return new ClassSourceInfo(name, node.sourceFile, Integer.parseInt(subStr), node.outerMethod, node.outerMethodDesc, null);
			} catch (NumberFormatException e) {
				// detected local class or some cursed shit
				int i;
				for (i = 0; i < subStr.length(); i++) {
					if (!Character.isDigit(subStr.charAt(i))) {
						break;
					}
				}

				try {
					return new ClassSourceInfo(name, node.sourceFile, Integer.parseInt(subStr.substring(0, i+1)), node.outerMethod, node.outerMethodDesc, subStr.substring(i+1));
				} catch (NumberFormatException e1) {
					System.err.println("Detected non-java local class " + node.name);
					return null;
				}
			}
		} else {
			return new ClassSourceInfo(node.name, node.sourceFile, 0, null, null, null);
		}
	}

	class ClassSourceInfo {
		public final String internalName;
		public final String sourceFile;
		public final int approximateMethodIndex;
		public final String methodName;
		public final String methodDesc;
		public final String localClassName;

		public ClassSourceInfo(String internalName, String sourceFile, int index, String methodName, String methodDesc, String localClassName) {
			this.internalName = internalName;
			this.sourceFile = sourceFile;
			this.approximateMethodIndex = index;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
			this.localClassName = localClassName;
		}
	}
}
