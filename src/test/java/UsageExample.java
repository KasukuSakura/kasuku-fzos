import io.github.karlatemp.kasukufzos.image.reader.KFzClassLoader;
import io.github.karlatemp.kasukufzos.image.reader.KFzReader;
import io.github.karlatemp.kasukufzos.image.writer.KFzWriter;
import io.github.karlatemp.kasukufzos.utils.TransferKit;

import java.io.File;
import java.util.zip.ZipFile;

public class UsageExample {
    public static void main(String[] args) throws Exception {
        File image = new File("misc/img.application.bin");
        if (!image.isFile()) {
            System.out.println("Writing application image....");
            KFzWriter writer = KFzWriter.of(image);
            //noinspection ConstantConditions
            for (File lib : new File("application").listFiles()) {
                try (ZipFile zip = new ZipFile(lib)) {
                    TransferKit.transfer(zip, lib.getName(), writer);
                }
            }
            writer.close();
            System.out.println("Completed");
        }
        var reader = KFzReader.from(image);
        KFzClassLoader.Options options = new KFzClassLoader.Options();
        options.signAction = KFzClassLoader.Options.SignAction.THROW_ON_FAILURE;
        KFzClassLoader loader = new KFzClassLoader(null, reader, options);
        loader.loadClass("org.example.launcher.Launcher")
                .getMethod("main", String[].class)
                .invoke(null, (Object) new String[0]);
    }
}
