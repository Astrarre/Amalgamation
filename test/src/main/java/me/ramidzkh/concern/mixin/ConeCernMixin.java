package me.ramidzkh.concern.mixin;

import java.util.function.BooleanSupplier;

//@Mixin(value = MinecraftServer.class)
public abstract class ConeCernMixin {
	//@Shadow protected abstract void tick(BooleanSupplier supplier);

	//@Shadow private DataCommandStorage dataCommandStorage;

	//@Shadow public abstract String getVersion();

	//@Inject(method = "tick", at = @At("HEAD"))
	/*public void tickAtHead(BooleanSupplier supplier, CallbackInfo ci) {
		System.out.println("aaaaa");
		this.getVersion();
		this.dataCommandStorage = null;
	}*/
}
