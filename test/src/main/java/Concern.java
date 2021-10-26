import com.mojang.serialization.Codec;

public class Concern implements Runnable {
    public static void main(String[] args) {
        System.out.println("aaaaaaggggadwadawdafffffnnnna");
        hello();
        System.out.println(Boolean.getBoolean("fabric.development"));
    }

    private static void hello() {
        System.out.println("hello from server!");
    }

    private static void hello_client() {
        System.out.println("hello from client!");
    }

    @Override
    public void run() {
    }
}
