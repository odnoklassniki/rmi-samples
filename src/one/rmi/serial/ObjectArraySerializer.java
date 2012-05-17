package one.rmi.serial;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;
import java.lang.reflect.Array;

class ObjectArraySerializer extends Serializer<Object[]> {
    private Class componentType;

    ObjectArraySerializer(Class cls, boolean useSimpleUid) {
        super(cls, useSimpleUid);
        this.componentType = cls.getComponentType();
    }

    @Override
    public void readObject(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readObject(in);
        this.componentType = cls.getComponentType();
    }

    @Override
    public void write(Object[] obj, ObjectOutput out) throws IOException {
        out.writeInt(obj.length);
        for (Object v : obj) {
            out.writeObject(v);
        }
    }

    @Override
    public Object read(ObjectInput in) throws IOException, ClassNotFoundException {
        return Array.newInstance(componentType, in.readInt());
    }

    @Override
    public void fill(Object[] obj, ObjectInput in) throws IOException, ClassNotFoundException {
        for (int i = 0; i < obj.length; i++) {
            obj[i] = in.readObject();
        }
    }

    @Override
    public void skip(ObjectInput in) throws IOException {
        int length = in.readInt();
        for (int i = 0; i < length; i++) {
            in.skip(0);
        }
    }
}
