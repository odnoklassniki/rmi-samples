package one.rmi.serial;

import one.rmi.unsafe.JavaInternals;

import sun.misc.Unsafe;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

class UnsafeFieldSerializer extends Serializer {
    static final Unsafe unsafe = JavaInternals.getUnsafe();
    static final ClassSerializer classSerializer = (ClassSerializer) Repository.get(Class.class);

    private FieldAccessor[] fieldAccessors;

    UnsafeFieldSerializer(Class cls, boolean useSimpleUid) {
        super(cls, useSimpleUid);
        List<Field> ownFields = getSerializableFields();
        this.fieldAccessors = new FieldAccessor[ownFields.size()];
        for (int i = 0; i < fieldAccessors.length; i++) {
            Field f = ownFields.get(i);
            fieldAccessors[i] = getFieldAccessor(f.getType(), unsafe.objectFieldOffset(f));
        }
    }

    @Override
    public void writeObject(ObjectOutput out) throws IOException {
        super.writeObject(out);

        List<Field> ownFields = getSerializableFields();
        out.writeShort(ownFields.size());
        
        for (Field f : ownFields) {
            out.writeUTF(f.getName());
            classSerializer.write(f.getType(), out);
        }
    }

    @Override
    public void readObject(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readObject(in);

        List<Field> ownFields = getSerializableFields();
        int incomingFields = in.readUnsignedShort();
        this.fieldAccessors = new FieldAccessor[incomingFields];
        
        for (int i = 0; i < incomingFields; i++) {
            String name = in.readUTF();
            Class type = classSerializer.read(in);

            for (Field f : ownFields) {
                if (f.getName().equals(name) && f.getType() == type) {
                    fieldAccessors[i] = getFieldAccessor(type, unsafe.objectFieldOffset(f));
                    break;
                }
            }

            if (fieldAccessors[i] == null) {
                fieldAccessors[i] = new SkipFieldAccessor(getFieldAccessor(type, 0L));
            }
        }
    }

    @Override
    public void write(Object obj, ObjectOutput out) throws IOException {
        for (FieldAccessor f : fieldAccessors) {
            f.write(obj, out);
        }
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
    public void fill(Object obj, ObjectInput in) throws IOException, ClassNotFoundException {
        for (FieldAccessor f : fieldAccessors) {
            f.read(obj, in);
        }
    }

    @Override
    public void skip(ObjectInput in) throws IOException {
        for (FieldAccessor f : fieldAccessors) {
            f.skip(in);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append("Fields:\n");
        for (Field f : getSerializableFields()) {
            builder.append(" - Name: ").append(f.getName()).append('\n');
            builder.append("   Type: ").append(f.getType().getName()).append('\n');
        }
        builder.append("Accessors:\n");
        for (FieldAccessor f : fieldAccessors) {
            builder.append(" - ").append(f).append('\n');
        }
        return builder.toString();
    }

    private static FieldAccessor getFieldAccessor(final Class type, final long offset) {
        if (type == int.class) {
            return new FieldAccessor() {
                void write(Object obj, ObjectOutput out) throws IOException {
                    out.writeInt(unsafe.getInt(obj, offset));
                }
                void read(Object obj, ObjectInput in) throws IOException {
                    unsafe.putInt(obj, offset, in.readInt());
                }
                void skip(ObjectInput in) throws IOException {
                    in.skipBytes(4);
                }
                public String toString() {
                    return "int@" + offset;
                }
            };
        } else if (type == long.class) {
            return new FieldAccessor() {
                void write(Object obj, ObjectOutput out) throws IOException {
                    out.writeLong(unsafe.getLong(obj, offset));
                }
                void read(Object obj, ObjectInput in) throws IOException {
                    unsafe.putLong(obj, offset, in.readLong());
                }
                void skip(ObjectInput in) throws IOException {
                    in.skipBytes(8);
                }
                public String toString() {
                    return "long@" + offset;
                }
            };
        } else if (type == boolean.class) {
            return new FieldAccessor() {
                void write(Object obj, ObjectOutput out) throws IOException {
                    out.writeBoolean(unsafe.getBoolean(obj, offset));
                }
                void read(Object obj, ObjectInput in) throws IOException {
                    unsafe.putBoolean(obj, offset, in.readBoolean());
                }
                void skip(ObjectInput in) throws IOException {
                    in.skipBytes(1);
                }
                public String toString() {
                    return "boolean@" + offset;
                }
            };
        } else if (type == byte.class) {
            return new FieldAccessor() {
                void write(Object obj, ObjectOutput out) throws IOException {
                    out.writeByte(unsafe.getByte(obj, offset));
                }
                void read(Object obj, ObjectInput in) throws IOException {
                    unsafe.putByte(obj, offset, in.readByte());
                }
                void skip(ObjectInput in) throws IOException {
                    in.skipBytes(1);
                }
                public String toString() {
                    return "byte@" + offset;
                }
            };
        } else if (type == short.class) {
            return new FieldAccessor() {
                void write(Object obj, ObjectOutput out) throws IOException {
                    out.writeShort(unsafe.getShort(obj, offset));
                }
                void read(Object obj, ObjectInput in) throws IOException {
                    unsafe.putShort(obj, offset, in.readShort());
                }
                void skip(ObjectInput in) throws IOException {
                    in.skipBytes(2);
                }
                public String toString() {
                    return "short@" + offset;
                }
            };
        } else if (type == char.class) {
            return new FieldAccessor() {
                void write(Object obj, ObjectOutput out) throws IOException {
                    out.writeChar(unsafe.getChar(obj, offset));
                }
                void read(Object obj, ObjectInput in) throws IOException {
                    unsafe.putChar(obj, offset, in.readChar());
                }
                void skip(ObjectInput in) throws IOException {
                    in.skipBytes(2);
                }
                public String toString() {
                    return "char@" + offset;
                }
            };
        } else if (type == float.class) {
            return new FieldAccessor() {
                void write(Object obj, ObjectOutput out) throws IOException {
                    out.writeFloat(unsafe.getFloat(obj, offset));
                }
                void read(Object obj, ObjectInput in) throws IOException {
                    unsafe.putFloat(obj, offset, in.readFloat());
                }
                void skip(ObjectInput in) throws IOException {
                    in.skipBytes(4);
                }
                public String toString() {
                    return "float@" + offset;
                }
            };
        } else if (type == double.class) {
            return new FieldAccessor() {
                void write(Object obj, ObjectOutput out) throws IOException {
                    out.writeDouble(unsafe.getDouble(obj, offset));
                }
                void read(Object obj, ObjectInput in) throws IOException {
                    unsafe.putDouble(obj, offset, in.readDouble());
                }
                void skip(ObjectInput in) throws IOException {
                    in.skipBytes(8);
                }
                public String toString() {
                    return "double@" + offset;
                }
            };
        } else {
            return new FieldAccessor() {
                void write(Object obj, ObjectOutput out) throws IOException {
                    out.writeObject(unsafe.getObject(obj, offset));
                }
                void read(Object obj, ObjectInput in) throws IOException, ClassNotFoundException {
                    unsafe.putObject(obj, offset, in.readObject());
                }
                void skip(ObjectInput in) throws IOException {
                    in.skip(0);
                }
                public String toString() {
                    return type.getName() + '@' + offset;
                }
            };
        }
    }

    private List<Field> getSerializableFields() {
        ArrayList<Field> list = new ArrayList<Field>();
        getSerializableFields(cls, list);
        return list;
    }

    private static void getSerializableFields(Class cls, List<Field> list) {
        if (cls != null) {
            getSerializableFields(cls.getSuperclass(), list);
            for (Field f : cls.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers())) {
                    list.add(f);
                }
            }
        }
    }

    private static abstract class FieldAccessor {
        abstract void write(Object obj, ObjectOutput out) throws IOException;
        abstract void read(Object obj, ObjectInput in) throws IOException, ClassNotFoundException;
        abstract void skip(ObjectInput in) throws IOException;
    }

    private static class SkipFieldAccessor extends FieldAccessor {
        private FieldAccessor parent;

        SkipFieldAccessor(FieldAccessor parent) {
            this.parent = parent;
        }

        void write(Object obj, ObjectOutput out) throws IOException {
            parent.write(obj, out);
        }
        void read(Object obj, ObjectInput in) throws IOException, ClassNotFoundException {
            parent.skip(in);
        }
        void skip(ObjectInput in) throws IOException {
            parent.skip(in);
        }
        public String toString() {
            return "skip " + parent;
        }
    }
}
