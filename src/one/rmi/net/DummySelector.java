package one.rmi.net;

import java.util.Iterator;

/*
 * This is a trivial implementation of Selector that does not use OS-specific features.
 * It simply returns all registred sockets as if all of them are ready for reading.
 * Caution: this implementation is NOT suitable for use in production!
 */
class DummySelector implements Selector {
    LinkedSocketHandle head;
    int size;
    boolean closed;

    @Override
    public SocketHandle register(Socket socket) {
        return new LinkedSocketHandle(socket);
    }

    @Override
    public synchronized Iterator<SocketHandle> iterator() {
        return new Iterator<SocketHandle>() {
            private LinkedSocketHandle current = null;
            private LinkedSocketHandle next = head;

            @Override
            public final boolean hasNext() {
                return next != null;
            }

            @Override
            public final SocketHandle next() {
                current = next;
                next = current.next;
                return current;
            }

            @Override
            public final void remove() {
                current.cancel();
            }
        };
    }

    @Override
    public synchronized Iterator<SocketHandle> select() {
        while (!closed && head == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Iterator<SocketHandle> result = iterator();
        head = null;
        return result;
    }

    @Override
    public synchronized int size() {
        return size;
    }

    @Override
    public synchronized void close() {
        head = null;
        size = 0;
        closed = true;
        notifyAll();
    }

    synchronized void push(LinkedSocketHandle handle) {
        if (head == null) {
            notify();
        }
        handle.next = head;
        head = handle;
    }

    private class LinkedSocketHandle extends SocketHandle {
        LinkedSocketHandle next;

        LinkedSocketHandle(Socket socket) {
            super(socket);
        }

        @Override
        public void activate() {
            synchronized (DummySelector.this) {
                size++;
                push(this);
            }
        }

        @Override
        public void renew() {
            push(this);
        }

        @Override
        public void cancel() {
            synchronized (DummySelector.this) {
                size--;
            }
        }

        @Override
        public boolean isReadable() {
            return true;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }
}
