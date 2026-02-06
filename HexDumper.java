import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HexDumper {
    public static void main(String[] args) throws IOException {
        String path = args[0];
        byte[] data = Files.readAllBytes(Path.of(path));
        System.out.println("File: " + path + " | Total bytes: " + data.length);
        for (int i = 0; i < Math.min(data.length, 512); i++) {
            if (i % 16 == 0)
                System.out.printf("%04X: ", i);
            System.out.printf("%02X ", data[i]);
            if ((i + 1) % 16 == 0)
                System.out.println();
        }
        System.out.println();
    }
}
