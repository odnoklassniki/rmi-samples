package one.rmi.serial;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class SerializerNotFoundException extends IOException implements SelfSerializable {
    private long uid;

    public SerializerNotFoundException(long uid) {
        this.uid = uid;
    }

    public long getUid() {
        return uid;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + Long.toHexString(uid);
    }

    @Override
    public void writeObject(ObjectOutput out) throws IOException {
        out.writeLong(uid);
    }

    @Override
    public void readObject(ObjectInput in) throws IOException {
        this.uid = in.readLong();
    }
}
