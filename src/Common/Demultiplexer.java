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
        Queue<byte[]> queue = new ArrayDeque<>();
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
                        if (fv == null) {
                            fv = new FrameValue();
                            map.put(frame.tag, fv);
                        }
                        fv.queue.add(frame.data);
                        fv.c.signal();
                    } finally {
                        l.unlock();
                    }
                }
            } catch (IOException e) {
                exception = e;
            }
        }).start();
    }

    public void send(Frame frame) throws IOException {
        System.out.println("Demultiplexer: Sending frame with tag " + frame.tag);
        c.send(frame);
    }

    public void send(FrameList frameList) throws IOException {
        c.send(frameList);
    }

    // public void send(int tag, String username, byte[] data) throws IOException {
    // c.send(tag, username, data);
    // }

    public byte[] receive(int tag) throws IOException, InterruptedException {
        l.lock();
        FrameValue fv;
        try {
            fv = map.get(tag);
            if (fv == null) {
                fv = new FrameValue();
                map.put(tag, fv);
            }
            fv.waiters++;
            while (true) {
                if (!fv.queue.isEmpty()) {
                    fv.waiters--;
                    byte[] reply = fv.queue.poll();
                    if (fv.waiters == 0 && fv.queue.isEmpty())
                        map.remove(tag);
                    return reply;
                }
                if (exception != null) {
                    throw exception;
                }
                fv.c.await();
            }
        } finally {
            l.unlock();
        }
    }

    public FrameList receiveFrameList() throws IOException, InterruptedException {
        l.lock();
        try {
            FrameList frameList = c.receiveFrameList();
            FrameList responseList = new FrameList();

            // For each frame, wait for its response
            for (Frame frame : frameList) {
                FrameValue fv = map.get(frame.tag);
                if (fv == null) {
                    fv = new FrameValue();
                    map.put(frame.tag, fv);
                }
                fv.waiters++;

                while (true) {
                    if (!fv.queue.isEmpty()) {
                        fv.waiters--;
                        byte[] reply = fv.queue.poll();
                        if (fv.waiters == 0 && fv.queue.isEmpty())
                            map.remove(frame.tag);
                        responseList.add(new Frame(frame.tag, frame.stringInput, reply));
                        break;
                    }
                    if (exception != null) {
                        throw exception;
                    }
                    fv.c.await();
                }
            }

            return responseList;
        } finally {
            l.unlock();
        }
    }

    public void close() throws IOException {
        c.close();
    }
}