package me.ramidzkh.concern;


import io.github.astrarre.amalgamation.api.Displace;
import io.github.astrarre.amalgamation.api.Platform;

public class Concern {
    public static void main(String[] args) {
        System.out.println("");
        hello();
        System.out.println(Boolean.getBoolean("fabric.development"));
    }

    @Platform("server")
    public static void hello() {
        System.out.println("hello from server!");
    }

    @Platform("client")
    @Displace("hello")
    public static void hello_client() {
        System.out.println("hello from client!");
    }
}
