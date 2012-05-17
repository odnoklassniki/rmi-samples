package one.rmi.serial;

import one.rmi.io.DigestStream;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public abstract class Serializer<T> implements SelfSerializable {
    private static long nextSimpleUid = -10L;

    Class cls;
    long uid;

    Serializer(Class cls, boolean useSimpleUid) {
        this.cls = cls;
        this.uid = useSimpleUid ? nextSimpleUid-- : generateLongUid();
    }

    @Override
    public void writeObject(ObjectOutput out) throws IOException {
        out.writeUTF(cls.getName());
        out.writeLong(uid);
    }

    @Override
    public void readObject(ObjectInput in) throws IOException, ClassNotFoundException {
        this.cls = Class.forName(in.readUTF());
        this.uid = in.readLong();
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(100);
        builder.append("---\n");
        builder.append("Class: ").append(cls.getName()).append('\n');
        builder.append("UID: ").append(Long.toHexString(uid)).append('\n');
        return builder.toString();
    }

    private long generateLongUid() {
        DigestStream ds = new DigestStream("MD5");
        try {
            ds.writeUTF(getClass().getName());
            writeObject(ds);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        byte[] tmp = ds.digest();
        return (tmp[0] & 0x7fL) << 56 |
               (tmp[1] & 0xffL) << 48 |
               (tmp[2] & 0xffL) << 40 |
               (tmp[3] & 0xffL) << 32 |
               (tmp[4] & 0xffL) << 24 |
               (tmp[5] & 0xffL) << 16 |
               (tmp[6] & 0xffL) <<  8 |
               (tmp[7] & 0xffL);
    }

    public abstract void write(T obj, ObjectOutput out) throws IOException;
    public abstract Object read(ObjectInput in) throws IOException, ClassNotFoundException;
    public abstract void skip(ObjectInput in) throws IOException;
    
    public void fill(T obj, ObjectInput in) throws IOException, ClassNotFoundException {
        // Nothing to do here if read() completes creation of an object
    }
}
