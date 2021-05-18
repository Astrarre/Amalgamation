package me.ramidzkh.concern;

import java.util.Arrays;

import io.github.f2bb.amalgamation.Displace;
import io.github.f2bb.amalgamation.Platform;

public class Concern {
    public static void main(String[] args) {
        hello();
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
