package one.rmi.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class NativeLibrary {
    private static boolean isLoaded;

    static synchronized void load() {
        if (isLoaded) {
            return;
        }

        try {
            InputStream in = NativeLibrary.class.getResourceAsStream("/libnativeio.so");
            File dll = File.createTempFile("libnativeio-", ".so");
            dll.deleteOnExit();
            OutputStream out = new FileOutputStream(dll);
            copyStreams(in, out);
            in.close();
            out.close();
            System.load(dll.getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load native IO library", e);
        }

        isLoaded = true;
    }

    private static void copyStreams(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[64000];
        for (int bytes; (bytes = in.read(buffer)) > 0; ) {
            out.write(buffer, 0, bytes);
        }
    }
}
