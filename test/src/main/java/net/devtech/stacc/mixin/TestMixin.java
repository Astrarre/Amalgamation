package net.devtech.stacc.mixin;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.inventory.Inventory;
import net.minecraft.server.command.ClearCommand;

@Implements({@Interface(iface = Inventory.class, prefix = "bruh$")})
@Mixin(ClearCommand.class)
public class TestMixin {
	boolean bruh$isEmpty() {
		return true;
	}

	void bruh2$run() {

	}
}
