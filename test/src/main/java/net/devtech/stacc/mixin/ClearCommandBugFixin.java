package net.devtech.stacc.mixin;

import java.util.Collection;
import java.util.function.Predicate;

import net.devtech.stacc.StaccGlobals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ClearCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin (ClearCommand.class)
public class ClearCommandBugFixin {
	@Inject (method = "execute", at = @At ("HEAD"))
	private static void clearCommand(ServerCommandSource source, Collection<ServerPlayerEntity> targets, Predicate<ItemStack> item, int maxCount,
	                                 CallbackInfoReturnable<Integer> cir) {
		StaccGlobals.COUNT.set(0L);
	}

	@ModifyArg (method = "execute",
			at = @At (value = "INVOKE", target = "Lnet/minecraft/text/TranslatableText;<init>(Ljava/lang/String;[Ljava/lang/Object;)V"),
			index = 1)
	private static Object[] exec(Object[] arr) {
		arr[0] = StaccGlobals.COUNT.get();
		return arr;
	}

	@Mixin (Inventories.class)
	private static class InventoriesFixin {
		@Inject (method = "remove(Lnet/minecraft/item/ItemStack;Ljava/util/function/Predicate;IZ)I",
				at = @At (value = "RETURN"),
				cancellable = true)
		private static void total(ItemStack itemStack,
				Predicate<ItemStack> predicate,
				int i,
				boolean bl,
				CallbackInfoReturnable<Integer> cir) {
			StaccGlobals.COUNT.set(StaccGlobals.COUNT.get() + cir.getReturnValue());
		}
	}
}
