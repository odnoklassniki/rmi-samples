package one.rmi.net;

public class SelectorFactory {
    private static final boolean isLinux64 =
            System.getProperty("os.name").toLowerCase().contains("linux") &&
            System.getProperty("os.arch").contains("64");

    public static Selector create() {
        return isLinux64 ? new EPollSelector() : new DummySelector();
    }
}
