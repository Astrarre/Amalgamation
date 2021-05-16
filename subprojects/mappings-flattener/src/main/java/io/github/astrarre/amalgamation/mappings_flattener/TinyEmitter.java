package io.github.astrarre.amalgamation.mappings_flattener;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import net.fabricmc.mapping.reader.v2.MappingGetter;
import net.fabricmc.mapping.reader.v2.TinyMetadata;
import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.FieldDef;
import net.fabricmc.mapping.tree.LocalVariableDef;
import net.fabricmc.mapping.tree.MethodDef;
import net.fabricmc.mapping.tree.ParameterDef;

public class TinyEmitter implements AutoCloseable {
	private static final char[] SPECIAL = "\\\0\r\n\t".toCharArray();
	private static final String[] REPLACE = {"\\", "\\0", "\\r", "\\n", "\\t"};
	private static final String[] TABS = {"", "\t", "\t\t", "\t\t\t", "\t\t\t\t"};
	public BufferedWriter writer;

	protected List<String> namespaces;
	protected String rootNamespace;
	protected int commentTabLevel = 0;
	protected boolean escape;
	protected boolean newln = false;

	public TinyEmitter(BufferedWriter writer) {
		this.writer = writer;
	}

	public TinyEmitter() {
	}

	public void writeLn() throws IOException {
		if(this.newln) {
			this.writer.write('\n');
		}
	}

	public void start(TinyMetadata metadata) {
		try {
			this.escape = metadata.getProperties().containsKey("escaped-names");
			this.writer.write("tiny\t");
			this.writer.write(String.valueOf(metadata.getMajorVersion()));
			this.writer.write('\t');
			this.writer.write(String.valueOf(metadata.getMinorVersion()));
			this.namespaces = metadata.getNamespaces();
			for (String namespace : metadata.getNamespaces()) {
				this.writer.write('\t');
				this.writer.write(namespace);
			}
			this.rootNamespace = this.namespaces.get(0);
			this.newln = true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void pushClass(ClassDef name) {
		try {
			this.commentTabLevel = 1;
			this.writeLn();
			this.writer.write('c');
			for (String namespace : this.namespaces) {
				this.writer.write('\t');
				this.writer.write(name.getRawName(namespace));
			}
			this.newln = true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void pushField(FieldDef name) {
		try {
			this.commentTabLevel = 2;
			this.writeLn();
			this.writer.write("\tf\t");
			this.writer.write(name.getDescriptor(this.rootNamespace));
			for (String namespace : this.namespaces) {
				this.writer.write('\t');
				this.writer.write(name.getRawName(namespace));
			}
			this.newln = true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void pushMethod(MethodDef name) {
		try {
			this.commentTabLevel = 2;
			this.writeLn();
			this.writer.write("\tm\t");
			this.writer.write(name.getDescriptor(this.rootNamespace));
			for (String namespace : this.namespaces) {
				this.writer.write('\t');
				this.writer.write(name.getRawName(namespace));
			}
			this.newln = true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void pushParameter(ParameterDef def) {
		try {
			this.commentTabLevel = 3;
			this.writeLn();
			this.writer.write("\t\tp\t");
			this.writer.write(String.valueOf(def.getLocalVariableIndex()));
			for (String namespace : this.namespaces) {
				this.writer.write('\t');
				this.writer.write(def.getRawName(namespace));
			}
			this.newln = true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void pushLocalVariable(LocalVariableDef def) {
		try {
			this.commentTabLevel = 3;
			this.writeLn();
			this.writer.write("\t\tv\t");
			this.writer.write(String.valueOf(def.getLocalVariableIndex()));
			this.writer.write('\t');
			this.writer.write(String.valueOf(def.getLocalVariableStartOffset()));
			this.writer.write('\t');
			this.writer.write(String.valueOf(def.getLocalVariableTableIndex()));
			for (String namespace : this.namespaces) {
				this.writer.write('\t');
				this.writer.write(def.getRawName(namespace));
			}
			this.newln = true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void pushComment(String comment) {
		if(comment == null) return;
		try {
			this.writeLn();
			this.writer.write(TABS[this.commentTabLevel]);
			this.writer.write("c\t");
			if(!escape) {
				for (int i = 0, length = SPECIAL.length; i < length; i++) {
					char c = SPECIAL[i];
					String toRep = REPLACE[i];
					comment = comment.replace(c + "", toRep);
				}
			}
			this.writer.write(comment);
			this.newln = true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void pop(int count) {

	}

	@Override
	public void close() throws Exception {
		this.writer.close();
	}
}
