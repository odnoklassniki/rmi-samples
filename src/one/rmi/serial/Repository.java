package one.rmi.serial;

import one.app.remote.comp.RemoteMethodCallRequest;
import one.app.remote.reflect.MethodId;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

public class Repository {
    private static final IdentityHashMap<Class, Serializer> classMap = new IdentityHashMap<Class, Serializer>(128);
    private static final HashMap<Long, Serializer> uidMap = new HashMap<Long, Serializer>(128);
    private static final Serializer[] bootstrapSerializers = new Serializer[128];
    private static final int ENUM = 0x4000;

    static {
        add(new IntegerSerializer());
        add(new LongSerializer());
        add(new BooleanSerializer());
        add(new ByteSerializer());
        add(new ShortSerializer());
        add(new CharacterSerializer());
        add(new FloatSerializer());
        add(new DoubleSerializer());
        add(new StringSerializer());
        add(new DateSerializer());
        add(new ClassSerializer());
        add(new BitSetSerializer());

        add(new BooleanArraySerializer());
        add(new ByteArraySerializer());
        add(new ShortArraySerializer());
        add(new CharacterArraySerializer());
        add(new IntegerArraySerializer());
        add(new LongArraySerializer());
        add(new FloatArraySerializer());
        add(new DoubleArraySerializer());

        add(new ObjectArraySerializer(Object[].class, true));
        add(new ObjectArraySerializer(String[].class, true));
        add(new ObjectArraySerializer(Class[].class, true));
        add(new ObjectArraySerializer(Long[].class, true));

        add(new CollectionSerializer(ArrayList.class, true));
        add(new CollectionSerializer(LinkedList.class, true));
        add(new CollectionSerializer(Vector.class, true));
        add(new CollectionSerializer(HashSet.class, true));
        add(new CollectionSerializer(TreeSet.class, true));
        add(new CollectionSerializer(LinkedHashSet.class, true));

        add(new MapSerializer(HashMap.class, true));
        add(new MapSerializer(TreeMap.class, true));
        add(new MapSerializer(LinkedHashMap.class, true));
        add(new MapSerializer(Hashtable.class, true));
        add(new MapSerializer(IdentityHashMap.class, true));
        add(new MapSerializer(ConcurrentHashMap.class, true));
        
        add(new SelfSerializer(ObjectArraySerializer.class, true));
        add(new SelfSerializer(EnumSerializer.class, true));
        add(new SelfSerializer(CollectionSerializer.class, true));
        add(new SelfSerializer(MapSerializer.class, true));
        add(new SelfSerializer(SelfSerializer.class, true));
        add(new SelfSerializer(UnsafeFieldSerializer.class, true));
        add(new SelfSerializer(SerializerNotFoundException.class, true));

        add(new UnsafeFieldSerializer(MethodId.class, true));
        add(new UnsafeFieldSerializer(RemoteMethodCallRequest.class, true));

        add(new TimestampSerializer());
    }
    
    private static void add(Serializer serializer) {
        classMap.put(serializer.cls, serializer);
        uidMap.put(serializer.uid, serializer);
        if (serializer.uid < 0) {
            bootstrapSerializers[128 + (int) serializer.uid] = serializer;
        }
    }

    public static Serializer get(Class cls) {
        Serializer result = classMap.get(cls);
        return result != null ? result : generateFor(cls);
    }

    public static Serializer requestSerializer(long uid) throws SerializerNotFoundException {
        Serializer result = uidMap.get(uid);
        if (result != null) {
            return result;
        }
        throw new SerializerNotFoundException(uid);
    }

    public static Serializer requestBootstrapSerializer(byte uid) {
        return bootstrapSerializers[128 + uid];
    }

    public static synchronized void provideSerializer(Serializer serializer) {
        uidMap.put(serializer.uid, serializer);
    }

    public static synchronized void dump() {
        for (Serializer s : uidMap.values()) {
            System.out.print(s);
        }
    }

    private static synchronized Serializer generateFor(Class cls) {
        Serializer serializer = classMap.get(cls);
        if (serializer == null) {
            if (cls.isArray()) {
                get(cls.getComponentType());
                serializer = new ObjectArraySerializer(cls, false);
            } else if ((cls.getModifiers() & ENUM) != 0) {
                if (cls.getSuperclass() != Enum.class) {
                    serializer = get(cls.getSuperclass());
                    classMap.put(cls, serializer);
                    return serializer;
                }
                serializer = new EnumSerializer(cls, false);
            } else if (SelfSerializable.class.isAssignableFrom(cls)) {
                serializer = new SelfSerializer(cls, false);
            } else if (Collection.class.isAssignableFrom(cls)) {
                serializer = new CollectionSerializer(cls, false);
            } else if (Map.class.isAssignableFrom(cls)) {
                serializer = new MapSerializer(cls, false);
            } else if (Serializable.class.isAssignableFrom(cls)) {
                serializer = new UnsafeFieldSerializer(cls, false);
            } else {
                serializer = new InvalidSerializer(cls);
            }
            add(serializer);
        }
        return serializer;
    }
 }
