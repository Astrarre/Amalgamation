package net.devtech.stacc.mixin;

import net.devtech.stacc.ItemCountRenderHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment (EnvType.CLIENT)
@Mixin (ItemRenderer.class)
public class RenderItemCountFixin {

	@Redirect (method = "renderGuiItemOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
			at = @At (value = "INVOKE", target = "Ljava/lang/String;valueOf(I)Ljava/lang/String;"))
	private String render(int i) {
		return ItemCountRenderHandler.getInstance().toConsiseString(i);
	}

	@Redirect (method = "renderGuiItemOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
			at = @At (value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Ljava/lang/String;)I"))
	private int width(TextRenderer renderer, String text) {
		return (int) (renderer.getWidth(text) * ItemCountRenderHandler.getInstance().scale(text));
	}

	@Inject (method = "renderGuiItemOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
			at = @At (value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V", shift = At.Shift.AFTER),
			locals = LocalCapture.CAPTURE_FAILHARD)
	private void rescaleText(TextRenderer fontRenderer, ItemStack stack, int x, int y, String amountText, CallbackInfo ci, MatrixStack matrixStack,
	                         String string) {
		float f = ItemCountRenderHandler.getInstance().scale(string);
		if (f != 1f) {
			matrixStack.translate(x * (1 - f), y * (1 - f) + (1 - f) * 16, 0);
			matrixStack.scale(f, f, f);
		}
	}
}