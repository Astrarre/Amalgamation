import io.github.astrarre.amalgamation.api.Displace;
import io.github.astrarre.amalgamation.api.Platform;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;

public class Concern implements Runnable {
    public static void main(String[] args) {
        System.out.println("aaaaaaggggadwadawdafffffnnnna");
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

    @Override
    public void run() {
        CompoundTag compound = new CompoundTag();
        compound.put("urmom", new ByteArrayTag(new byte[0]));
    }
}
