package one.app.remote.comp;

import java.lang.reflect.Type;
import java.util.Arrays;

import one.app.remote.reflect.MethodId;

public class RemoteMethodCallRequest  {
    private static final long serialVersionUID = 1L;

    private MethodId methodId;

    private Object[] args;

    public RemoteMethodCallRequest(MethodId methodId, Object[] args) {
        this.methodId = methodId;
        this.args = args;
    }

    public MethodId getMethodId() {
        return methodId;
    }

    public String getMethodName() {
        return methodId.getMethodName();
    }

    public Object[] getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return "RemoteMethodCallRequest{methodId=" + methodId + ", args=" + Arrays.toString(args) + '}';
    }
}
