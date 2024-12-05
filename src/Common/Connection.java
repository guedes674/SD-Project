package Common;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Connection class that handles sending and receiving frames over a socket.
 */
public class Connection implements AutoCloseable {

    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Lock rl = new ReentrantLock();
    private final Lock wl = new ReentrantLock();

    /**
     * Constructs a new Connection.
     *
     * @param socket The socket to use for communication
     * @throws IOException If an I/O error occurs
     */
    public Connection(Socket socket) throws IOException {
        this.dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    /**
     * Sends a frame over the connection.
     *
     * @param frame The frame to send
     * @throws IOException If an I/O error occurs
     */
    public void send(Frame frame) throws IOException {
        wl.lock();
        try {
            frame.serialize(dos);
            dos.flush();
        } finally {
            wl.unlock();
        }
    }

    /**
     * Receives a frame from the connection.
     *
     * @return The received frame
     * @throws IOException If an I/O error occurs
     */
    public Frame receive() throws IOException {
        rl.lock();
        try {
            return Frame.deserialize(dis);
        } finally {
            rl.unlock();
        }
    }

    /**
     * Closes the connection.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        dis.close();
        dos.close();
    }
}