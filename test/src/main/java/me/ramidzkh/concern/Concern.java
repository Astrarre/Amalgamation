package me.ramidzkh.concern;

import io.github.f2bb.amalgamation.Platform;


import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;

import net.fabricmc.api.ClientModInitializer;

public class Concern implements @Platform("client") ClientModInitializer {
    @Override
    public void onInitializeClient() {
    }
}
