package net.devtech.stacc.mixin;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * fixes ItemStack to serialize count as int instead of byte
 */
@Mixin (ItemStack.class)
public abstract class SerializationFixin {
	@Environment (EnvType.CLIENT) private static final NumberFormat FORMAT = NumberFormat.getNumberInstance(Locale.US);
	@Shadow private int count;

	@Inject (at = @At ("TAIL"), method = "<init>(Lnet/minecraft/nbt/NbtCompound;)V")
	void onDeserialization(NbtCompound tag, CallbackInfo callbackInformation) {
		if (tag.contains("countInteger")) {
			this.count = tag.getInt("countInteger");
		}
	}

	@Inject (at = @At ("TAIL"), method = "writeNbt")
	void onSerialization(NbtCompound tag, CallbackInfoReturnable<NbtCompound> callbackInformationReturnable) {
		if (this.count > Byte.MAX_VALUE) {
			tag.putInt("countInteger", this.count);
			// make downgrading less painful
			tag.putByte("Count", Byte.MAX_VALUE);
		}
	}

	@Environment (EnvType.CLIENT)
	@Inject (method = "getTooltip", at = @At ("RETURN"), cancellable = true)
	private void addOverflowTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
		if (this.getCount() > 1000) {
			List<Text> texts = cir.getReturnValue();
			texts.add(1, new LiteralText(FORMAT.format(this.getCount())).formatted(Formatting.GRAY));
		}
	}

	@Shadow
	public abstract int getCount();
}