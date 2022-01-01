package net.devtech.stacc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ItemScatterer;

@Mixin (ItemScatterer.class)
public class OOMFixin {
	@Redirect (method = "spawn(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V",
			at = @At (value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;split(I)Lnet/minecraft/item/ItemStack;"))
	private static ItemStack noSplit(ItemStack stack, int amount) {
		ItemStack copy = stack.copy();
		stack.setCount(0);
		return copy;
	}
}
