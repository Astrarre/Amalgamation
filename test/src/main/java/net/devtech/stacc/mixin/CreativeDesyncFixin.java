package net.devtech.stacc.mixin;

import net.devtech.stacc.StaccGlobals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.server.network.ServerPlayNetworkHandler;

/**
 * fixes server-client desync when cheating in items in creative mode
 */
@Mixin (ServerPlayNetworkHandler.class)
public class CreativeDesyncFixin {
	@ModifyConstant (method = "onCreativeInventoryAction", constant = @Constant (intValue = 64))
	private int max(int old) {
		return StaccGlobals.getMax();
	}
}