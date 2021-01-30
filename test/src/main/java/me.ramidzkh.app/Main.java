package me.ramidzkh.app;

import io.github.f2bb.amalgamation.Platform;
import net.fabricmc.fabric.mixin.networking.accessor.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;

public class Main implements @Platform("1.16") MinecraftClientAccessor {

    public static void main(String[] args) {
        MinecraftClient c = null;
    }

    @Override
    @Platform("1.16")
    public ClientConnection getConnection() {
        return null;
    }
}
