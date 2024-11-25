package Common;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demultiplexer class that handles receiving frames and dispatching them to the
 * appropriate handlers.
 */
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

    /**
     * Constructs a new Demultiplexer.
     *
     * @param conn The connection to use for communication
     */
    public Demultiplexer(Connection conn) {
        this.c = conn;
    }

    /**
     * Starts the demultiplexer to receive frames.
     */
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
                        fv.queue.add(frame);
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

    /**
     * Sends a frame over the connection.
     *
     * @param frame The frame to send
     * @throws IOException If an I/O error occurs
     */
    public void send(Frame frame) throws IOException {
        c.send(frame);
    }

    /**
     * Receives a frame with the specified tag.
     *
     * @param tag The tag of the frame to receive
     * @return The received frame
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If the thread is interrupted
     */
    public Frame receive(int tag) throws IOException, InterruptedException {
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
                    Frame reply = fv.queue.poll();
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

    /**
     * Closes the demultiplexer and the underlying connection.
     *
     * @throws IOException If an I/O error occurs
     */
    public void close() throws IOException {
        c.close();
    }

    /**
     * Returns the underlying connection.
     *
     * @return The connection
     */
    public Connection getConnection() {
        return c;
    }
}