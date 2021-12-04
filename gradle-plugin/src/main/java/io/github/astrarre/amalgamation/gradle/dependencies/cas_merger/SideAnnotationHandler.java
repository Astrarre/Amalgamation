package io.github.astrarre.amalgamation.gradle.dependencies.cas_merger;

import java.nio.charset.StandardCharsets;

import com.google.common.hash.Hasher;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

public interface SideAnnotationHandler {
	StandardHandler FABRIC = new StandardHandler("Lnet/fabricmc/api/Environment;", "Lnet/fabricmc/api/EnvironmentInterface;", "Lnet/fabricmc/api/EnvType;");

	String normalDesc();

	String ifaceDesc();

	void accept(AnnotationVisitor visitor, boolean isClient);

	void accept(AnnotationVisitor visitor, String iface, boolean isClient);

	void hashInputs(Hasher hasher);

	class StandardHandler implements SideAnnotationHandler {
		final String normalDesc, ifaceDesc, enumDesc, client, server;

		public StandardHandler(String normalDesc, String ifaceDesc, String enumDesc) {
			this(normalDesc, ifaceDesc, enumDesc, "CLIENT", "SERVER");
		}

		public StandardHandler(String normalDesc, String ifaceDesc, String enumDesc, String client, String server) {
			this.normalDesc = normalDesc;
			this.ifaceDesc = ifaceDesc;
			this.enumDesc = enumDesc;
			this.client = client;
			this.server = server;
		}

		@Override
		public String normalDesc() {
			return this.normalDesc;
		}

		@Override
		public String ifaceDesc() {
			return this.ifaceDesc;
		}

		@Override
		public void accept(AnnotationVisitor visitor, boolean isClient) {
			visitor.visitEnum("value", this.enumDesc, this.forSide(isClient));
		}

		@Override
		public void accept(AnnotationVisitor visitor, String iface, boolean isClient) {
			visitor.visit("itf", Type.getObjectType(iface));
			visitor.visitEnum("value", this.enumDesc, this.forSide(isClient));
		}

		@Override
		public void hashInputs(Hasher hasher) {
			hasher.putString(this.getClass().toString(), StandardCharsets.UTF_8);
			hasher.putString(this.normalDesc, StandardCharsets.UTF_8);
			hasher.putString(this.ifaceDesc, StandardCharsets.UTF_8);
			hasher.putString(this.client, StandardCharsets.UTF_8);
			hasher.putString(this.server, StandardCharsets.UTF_8);
			hasher.putString(this.enumDesc, StandardCharsets.UTF_8);
		}

		public String forSide(boolean side) {
			return side ? this.client : this.server;
		}
	}
}
