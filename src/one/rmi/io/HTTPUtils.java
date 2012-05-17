package one.rmi.io;

public class HTTPUtils {

    public static int findHeaderEnd(byte[] buf, int pos, int length) {
        while (pos < length) {
            if (buf[pos++] == 10 && pos >= 4 && buf[pos - 4] == 13 && buf[pos - 3] == 10 && buf[pos - 2] == 13) {
                return pos;
            }
        }
        return -1;
    }
}
