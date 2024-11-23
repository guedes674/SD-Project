package Common;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Demultiplexer {

    private final Connection c;
    private final ReentrantLock l = new ReentrantLock();
    private final Map<Integer, FrameValue> map = new HashMap<>();
    private IOException exception = null;

    private class FrameValue {
        int waiters = 0;
        Queue<Frame> queue = new ArrayDeque<>();
        Condition c = l.newCondition();

        public FrameValue() {
        }
    }

    public Demultiplexer(Connection conn) {
        this.c = conn;
    }

    public void start() {
        new Thread(() -> {
            try {
                while (true) {
                    Frame frame = c.receive();
                    l.lock();
                    try {
                        FrameValue fv = map.get(frame.tag);
                        if (fv != null) {
                            fv.queue.add(frame);
                            fv.c.signal();
                        }
                    } finally {
                        l.unlock();
                    }
                }
            } catch (IOException e) {
                l.lock();
                try {
                    exception = e;
                    for (FrameValue fv : map.values()) {
                        fv.c.signalAll();
                    }
                } finally {
                    l.unlock();
                }
            }
        }).start();
    }

    public void send(Frame frame) throws IOException {
        System.out.println("Demultiplexer: Sending frame with tag " + frame.tag);
        c.send(frame);
    }

    public Frame receive(int tag) throws IOException, InterruptedException {
        l.lock();
        try {
            FrameValue fv = map.computeIfAbsent(tag, k -> new FrameValue());
            fv.waiters++;
            while (fv.queue.isEmpty() && exception == null) {
                fv.c.await();
            }
            fv.waiters--;
            if (exception != null) {
                throw exception;
            }
            return fv.queue.poll();
        } finally {
            l.unlock();
        }
    }

    public void close() throws IOException {
        c.close();
    }
}