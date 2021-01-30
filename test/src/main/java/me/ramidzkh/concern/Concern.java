package me.ramidzkh.concern;

import io.github.f2bb.amalgamation.Displace;
import io.github.f2bb.amalgamation.Platform;
import net.fabricmc.api.ClientModInitializer;

public class Concern implements @Platform("client") ClientModInitializer {

    @Override
    @Platform("client")
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(Concern::doStuff);
    }

    @Platform({"fabric", "client", "1.16.5"})
    private static void doStuff(MinecraftClient client) {
        System.out.println("1.16.5!");
    }

    @Displace(value = "doStuff")
    @Platform({"fabric", "client", "1.16"})
    private static void doStuff_1_16(MinecraftClient client) {
        System.out.println("1.16!");
    }
}
