package Common;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Connection implements AutoCloseable {

    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Lock rl = new ReentrantLock();
    private final Lock wl = new ReentrantLock();

    public Connection(Socket socket) throws IOException {
        this.dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public void send(int tag, String stringInput, byte[] data) throws IOException {
        wl.lock();
        try {
            new Frame(tag, stringInput, data).serialize(dos);
            dos.flush();
        } finally {
            wl.unlock();
        }
    }

    public void send(Frame frame) throws IOException {
        wl.lock();
        try {
            frame.serialize(dos);
            dos.flush();
        } finally {
            wl.unlock();
        }
    }

    public void send(FrameList frameList) throws IOException {
        wl.lock();
        try {
            frameList.serialize(dos);
            dos.flush();
        } finally {
            wl.unlock();
        }
    }

    public Frame receive() throws IOException {
        rl.lock();
        try {
            return Frame.deserialize(dis);
        } finally {
            rl.unlock();
        }
    }

    public FrameList receiveFrameList() throws IOException {
        rl.lock();
        try {
            return FrameList.deserialize(dis);
        } finally {
            rl.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        dis.close();
        dos.close();
    }
}