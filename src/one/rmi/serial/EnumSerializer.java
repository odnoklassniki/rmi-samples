package one.rmi.serial;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;

class EnumSerializer extends Serializer<Enum> {
    private Enum[] values;

    EnumSerializer(Class cls, boolean useSimpleUid) {
        super(cls, useSimpleUid);
        this.values = (Enum[]) cls.getEnumConstants();
    }

    @Override
    public void readObject(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readObject(in);
        this.values = (Enum[]) cls.getEnumConstants();
    }

    @Override
    public void write(Enum obj, ObjectOutput out) throws IOException {
        out.writeShort(obj.ordinal());
    }

    @Override
    public Enum read(ObjectInput in) throws IOException {
        return values[in.readUnsignedShort()];
    }

    @Override
    public void skip(ObjectInput in) throws IOException {
        in.skipBytes(2);
    }
}
