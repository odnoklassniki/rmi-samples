package one.rmi.serial;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

public interface SelfSerializable extends Serializable {
    void writeObject(ObjectOutput out) throws IOException;
    void readObject(ObjectInput in) throws IOException, ClassNotFoundException;
}
