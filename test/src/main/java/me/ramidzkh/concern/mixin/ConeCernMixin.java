package me.ramidzkh.concern.mixin;

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.command.DataCommandStorage;
import net.minecraft.server.MinecraftServer;

@Mixin(value = MinecraftServer.class)
public abstract class ConeCernMixin {
	@Shadow protected abstract void tick(BooleanSupplier supplier);

	@Shadow private DataCommandStorage dataCommandStorage;

	@Shadow public abstract String getVersion();

	@Inject(method = "tick", at = @At("HEAD"))
	public void tickAtHead(BooleanSupplier supplier, CallbackInfo ci) {
		System.out.println("aaaaa");
		this.getVersion();
		this.dataCommandStorage = null;
	}
}
