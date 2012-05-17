package one.rmi.io;

import java.io.ObjectOutput;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestStream implements ObjectOutput {
    protected MessageDigest md;
    protected byte[] buf;

    public DigestStream(String algorithm) {
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        buf = new byte[8];
    }

    public byte[] digest() {
        return md.digest();
    }

    public void write(int b) {
        md.update((byte) b);
    }

    public void write(byte[] b) {
        md.update(b);
    }

    public void write(byte[] b, int off, int len) {
        md.update(b, off, len);
    }

    public void writeBoolean(boolean v) {
        md.update(v ? (byte) 1 : (byte) 0);
    }

    public void writeByte(int v) {
        md.update((byte) v);
    }

    public void writeShort(int v) {
        buf[0] = (byte) (v >>> 8);
        buf[1] = (byte) v;
        md.update(buf, 0, 2);
    }

    public void writeChar(int v) {
        buf[0] = (byte) (v >>> 8);
        buf[1] = (byte) v;
        md.update(buf, 0, 2);
    }

    public void writeInt(int v) {
        buf[0] = (byte) (v >>> 24);
        buf[1] = (byte) (v >>> 16);
        buf[2] = (byte) (v >>> 8);
        buf[3] = (byte) v;
        md.update(buf, 0, 4);
    }

    public void writeLong(long v) {
        buf[0] = (byte) (v >>> 56);
        buf[1] = (byte) (v >>> 48);
        buf[2] = (byte) (v >>> 40);
        buf[3] = (byte) (v >>> 32);
        buf[4] = (byte) (v >>> 24);
        buf[5] = (byte) (v >>> 16);
        buf[6] = (byte) (v >>> 8);
        buf[7] = (byte) v;
        md.update(buf, 0, 8);
    }

    public void writeFloat(float v) {
        writeInt(Float.floatToRawIntBits(v));
    }

    public void writeDouble(double v) {
        writeLong(Double.doubleToRawLongBits(v));
    }

    @SuppressWarnings("deprecation")
    public void writeBytes(String s) {
        int length = s.length();
        byte[] tmp = new byte[length];
        s.getBytes(0, length, tmp, length);
        md.update(tmp);
    }

    public void writeChars(String s) {
        int length = s.length();
        byte[] tmp = new byte[length << 1];
        int pos = 0;
        for (int i = 0; i < length; i++) {
            int v = s.charAt(i);
            tmp[pos++] = (byte) (v >>> 8);
            tmp[pos++] = (byte) v;
        }
        md.update(tmp);
    }

    public void writeUTF(String s) {
        int utfLength = UTFUtils.length(s);
        byte[] tmp = new byte[utfLength + 2];
        tmp[0] = (byte) (utfLength >>> 8);
        tmp[1] = (byte) utfLength;
        UTFUtils.write(s, tmp, 2);
        md.update(tmp);
    }

    public void writeObject(Object obj) {
        writeInt(obj.hashCode());
    }

    public void flush() {
        // Nothing to do
    }

    public void close() {
        // Nothing to do
    }
}
