package one.rmi.serial;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

class MapSerializer extends Serializer<Map> {
    private Constructor constructor;

    MapSerializer(Class cls, boolean useSimpleUid) {
        super(cls, useSimpleUid);
        this.constructor = findConstructor();
        this.constructor.setAccessible(true);
    }

    @Override
    public void readObject(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readObject(in);
        this.constructor = findConstructor();
        this.constructor.setAccessible(true);
    }

    @Override
    public void write(Map obj, ObjectOutput out) throws IOException {
        out.writeInt(obj.size());
        for (Map.Entry e : ((Map<?, ?>) obj).entrySet()) {
            out.writeObject(e.getKey());
            out.writeObject(e.getValue());
        }
    }

    @Override
    public Object read(ObjectInput in) throws IOException, ClassNotFoundException {
        try {
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void fill(Map obj, ObjectInput in) throws IOException, ClassNotFoundException {
        int length = in.readInt();
        for (int i = 0; i < length; i++) {
            obj.put(in.readObject(), in.readObject());
        }
    }

    @Override
    public void skip(ObjectInput in) throws IOException {
        int length = in.readInt();
        for (int i = 0; i < length; i++) {
            in.skip(0);
            in.skip(0);
        }
    }

    private Constructor findConstructor() {
        try {
            return cls.getConstructor();
        } catch (NoSuchMethodException e) {
            Class implementation = SortedMap.class.isAssignableFrom(cls) ? TreeMap.class : HashMap.class;
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e1) {
                return null;
            }
        }
    }
}
