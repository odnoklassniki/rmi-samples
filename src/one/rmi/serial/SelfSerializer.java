package one.rmi.serial;

import one.rmi.unsafe.JavaInternals;

import sun.misc.Unsafe;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

class SelfSerializer extends Serializer<SelfSerializable> {
    private static final Unsafe unsafe = JavaInternals.getUnsafe();

    SelfSerializer(Class cls, boolean useSimpleUid) {
        super(cls, useSimpleUid);
    }

    @Override
    public void write(SelfSerializable obj, ObjectOutput out) throws IOException {
        obj.writeObject(out);
    }

    @Override
    public Object read(ObjectInput in) throws IOException, ClassNotFoundException {
        try {
            return unsafe.allocateInstance(cls);
        } catch (InstantiationException e) {
            throw new IOException(e);
        }
    }
    
    @Override
    public void fill(SelfSerializable obj, ObjectInput in) throws IOException, ClassNotFoundException {
        obj.readObject(in);
    }

    @Override
    public void skip(ObjectInput in) throws IOException {
        try {
            ((SelfSerializer) read(in)).readObject(in);
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
