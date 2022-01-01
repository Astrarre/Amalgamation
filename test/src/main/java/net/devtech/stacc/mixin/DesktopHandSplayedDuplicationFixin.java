package net.devtech.stacc.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;

@Mixin (ScreenHandler.class)
public class DesktopHandSplayedDuplicationFixin {
	@Redirect (method = "calculateStackSize", at = @At (value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;floor(F)I"))
	private static int floor(float input, Set<Slot> slots, int mode, ItemStack stack, int stackSize) {
		return MathHelper.floor((double) stack.getCount() / (double) slots.size());
	}
}
