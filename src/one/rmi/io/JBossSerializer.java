package one.rmi.io;

import org.jboss.remoting.serialization.SerializationManager;
import org.jboss.remoting.serialization.SerializationStreamFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class JBossSerializer {
    private static final SerializationManager MANAGER = getSerializationManager();

    private static SerializationManager getSerializationManager() {
        try {
            return SerializationStreamFactory.getManagerInstance(SerializationStreamFactory.JBOSS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static ObjectOutputStream createBufferedOutput(OutputStream out) throws IOException {
        return MANAGER.createOutput(new BufferedOutputStream(out)); 
    }

    public static ObjectInputStream createBufferedInput(InputStream in) throws IOException {
        return MANAGER.createRegularInput(new BufferedInputStream(in));
    }
}
