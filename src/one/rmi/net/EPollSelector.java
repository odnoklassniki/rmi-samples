package one.rmi.net;

import one.rmi.unsafe.JavaInternals;

import sun.misc.Unsafe;

import java.util.Arrays;
import java.util.Iterator;

class EPollSelector implements Selector {
    static final Unsafe unsafe = JavaInternals.getUnsafe();

    static final int EPOLL_CTL_ADD = 1;
    static final int EPOLL_CTL_DEL = 2;
    static final int EPOLL_CTL_MOD = 3;
    static final int EPOLLIN       = 1;
    static final int EPOLLOUT      = 4;
    static final int EPOLLERR      = 8;
    static final int EPOLLHUP      = 16;
    static final int EPOLLONESHOT  = 1 << 30;
    static final int EPOLLET       = 1 << 31;

    static final int EPOLL_MAX_EVENTS  = 1024;
    static final int EPOLL_STRUCT_SIZE = 12;

    static native int epollCreate();
    static native void epollClose(int epollFD);
    static native int epollWait(int epollFD, long epollStruct, int epollElements);
    static native void epollCtl(int epollFD, int op, int fd, int data, int events);

    final int epollFD;
    final long epollStruct;

    EPollSocketHandle[] handles;
    int size;
    int closeFlag;

    public EPollSelector() {
        this.epollFD = epollCreate();
        this.epollStruct = JavaInternals.allocateMemory(EPOLL_MAX_EVENTS * EPOLL_STRUCT_SIZE, this);
        this.handles = new EPollSocketHandle[EPOLL_MAX_EVENTS];
    }

    @Override
    public synchronized SocketHandle register(Socket socket) {
        if (++size > handles.length) {
            handles = Arrays.copyOf(handles, handles.length * 2);
        }
        final int mask = handles.length - 1;
        for (int slot = socket.fd & mask; ; slot = (slot + 1) & mask) {
            if (handles[slot] == null) {
                return handles[slot] = new EPollSocketHandle(socket, slot);
            }
        }
    }

    @Override
    public Iterator<SocketHandle> iterator() {
        return new Iterator<SocketHandle>() {
            private EPollSocketHandle next = findNext(0);
            
            private EPollSocketHandle findNext(int slot) {
                for (EPollSocketHandle[] handles = EPollSelector.this.handles; slot < handles.length; slot++) {
                    EPollSocketHandle handle = handles[slot];
                    if (handle != null) {
                        return handle;
                    }
                }
                return null;
            }
            
            @Override
            public final boolean hasNext() {
                return next != null;
            }

            @Override
            public final SocketHandle next() {
                EPollSocketHandle handle = next;
                next = findNext(handle.slot + 1);
                return handle;
            }

            @Override
            public final void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Iterator<SocketHandle> select() {
        final int count = epollWait(epollFD, epollStruct, EPOLL_MAX_EVENTS) | closeFlag;

        return new Iterator<SocketHandle>() {
            private long nextAddr = epollStruct;
            private long lastAddr = nextAddr + count * EPOLL_STRUCT_SIZE;
            private EPollSocketHandle next = findNext();

            private EPollSocketHandle findNext() {
                for (long currentAddr = nextAddr; currentAddr < lastAddr; currentAddr = nextAddr) {
                    EPollSocketHandle handle = handles[unsafe.getInt(currentAddr + 4)];
                    nextAddr = currentAddr + EPOLL_STRUCT_SIZE;
                    if (handle != null) {
                        handle.flags = unsafe.getInt(currentAddr);
                        return handle;
                    }
                }
                return null;
            }

            @Override
            public final boolean hasNext() {
                return next != null;
            }

            @Override
            public final SocketHandle next() {
                EPollSocketHandle handle = next;
                next = findNext();
                return handle;
            }

            @Override
            public final void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public synchronized int size() {
        return size;
    }

    @Override
    public synchronized void close() {
        if (closeFlag == 0) {
            closeFlag = -1;
            epollClose(epollFD);
        }
    }

    synchronized void remove(EPollSocketHandle handle) {
        if (handles[handle.slot] == handle) {
            handles[handle.slot] = null;
            size--;
        }
    }

    private class EPollSocketHandle extends SocketHandle {
        int slot;
        int flags;

        EPollSocketHandle(Socket socket, int slot) {
            super(socket);
            this.slot = slot;
        }

        @Override
        public void activate() {
            epollCtl(epollFD, EPOLL_CTL_ADD, socket.fd, slot, EPOLLIN | EPOLLONESHOT);
        }

        @Override
        public void renew() {
            epollCtl(epollFD, EPOLL_CTL_MOD, socket.fd, slot, EPOLLIN | EPOLLONESHOT);
        }

        @Override
        public void cancel() {
            epollCtl(epollFD, EPOLL_CTL_DEL, socket.fd, slot, 0);
            remove(this);
        }

        @Override
        public boolean isReadable() {
            return (flags & EPOLLIN) != 0;
        }

        @Override
        public boolean isWritable() {
            return (flags & EPOLLOUT) != 0;
        }

        @Override
        public String toString() {
            return "EPollSocketHandle[socket=" + socket.fd + ",attachment=" + attachment + ",flags=" + flags + "]";
        }
    }
    
    static {
        NativeLibrary.load();
    }
}
