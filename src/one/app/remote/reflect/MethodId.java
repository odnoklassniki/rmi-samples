package one.app.remote.reflect;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class MethodId implements Serializable {
    private static final long serialVersionUID = 1L;

    private String methodName;

    private String genericParameterTypes;

    public MethodId(Method method) {
        this(method.getName(), method.getGenericParameterTypes());
    }

    public MethodId(String methodName, Type[] genericParameterTypes) {
        this.methodName = methodName;
        this.genericParameterTypes = toString(genericParameterTypes);
    }

    public String getMethodName() {
        return methodName;
    }

    public String getGenericParameterTypes() {
        return genericParameterTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MethodId methodId = (MethodId) o;

        return genericParameterTypes.equals(methodId.genericParameterTypes) && methodName.equals(methodId.methodName);
    }

    @Override
    public int hashCode() {
        int result = methodName.hashCode();
        result = 31 * result + genericParameterTypes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MethodId{" +
                "methodName='" + methodName + '\'' +
                ", genericParameterTypes='" + genericParameterTypes + '\'' +
                '}';
    }

    private String toString(Type[] typeparms) {
        StringBuilder sb = new StringBuilder();
        if (typeparms.length > 0) {
            boolean first = true;
            sb.append("<");
            for (Type typeparm : typeparms) {
                if (!first) {
                    sb.append(",");
                }
                if (typeparm instanceof Class) {
                    sb.append(((Class) typeparm).getName());
                } else {
                    sb.append(typeparm.toString());
                }
                first = false;
            }
            sb.append("> ");
        }
        return sb.toString();
    }
}
