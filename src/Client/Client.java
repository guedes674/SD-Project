package Client;

import Common.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Client implements AutoCloseable {
    private final Socket socket;
    private final Connection connection;
    private final Demultiplexer demultiplexer;
    private final ReentrantLock lock = new ReentrantLock();

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        connection = new Connection(socket);
        demultiplexer = new Demultiplexer(connection);
        demultiplexer.start();
    }

    public boolean authenticate(String username, String password) throws IOException, InterruptedException {
        lock.lock();
        try {
            demultiplexer.send(new Frame(Request.AUTH, username, password.getBytes()));
            byte[] response = demultiplexer.receive(Request.AUTH);
            return new String(response).equals("Autenticado com sucesso!");
        } finally {
            lock.unlock();
        }
    }

    public boolean register(String username, String password) throws IOException, InterruptedException {
        lock.lock();
        try {
            demultiplexer.send(new Frame(Request.REGISTER, username, password.getBytes()));
            byte[] response = demultiplexer.receive(Request.REGISTER);
            return new String(response).equals("Registrado com sucesso!");
        } finally {
            lock.unlock();
        }
    }

    public void put(String key, byte[] value) throws IOException, InterruptedException {
        lock.lock();
        try {
            demultiplexer.send(new Frame(Request.PUT, key, value));
            demultiplexer.receive(Request.PUT); // Read response
        } finally {
            lock.unlock();
        }
    }

    public byte[] get(String key) throws IOException, InterruptedException {
        lock.lock();
        try {
            demultiplexer.send(new Frame(Request.GET, key, new byte[0]));
            return demultiplexer.receive(Request.GET);
        } finally {
            lock.unlock();
        }
    }

    public void multiPut(Map<String, byte[]> pairs) throws IOException, InterruptedException {
        lock.lock();
        try {
            // contruir a lista de frames
            FrameList frameList = new FrameList();
            for (Map.Entry<String, byte[]> entry : pairs.entrySet()) {
                frameList.add(new Frame(Request.PUT, entry.getKey(), entry.getValue()));
            }
            demultiplexer.send(frameList);
        } finally {
            lock.unlock();
        }
    }

    public Map<String, byte[]> multiGet(Set<String> keys) throws IOException, InterruptedException {
        lock.lock();
        try {
            // Create request framelist
            FrameList requestFrames = new FrameList();
            for (String key : keys) {
                requestFrames.add(new Frame(Request.GET, key, new byte[0]));
            }

            // Send the request framelist
            demultiplexer.send(requestFrames);

            // Receive response framelist
            FrameList responseFrames = demultiplexer.receiveFrameList();

            // Convert response to map
            Map<String, byte[]> results = new HashMap<>();
            for (Frame frame : responseFrames) {
                results.put(frame.stringInput, frame.data);
            }

            return results;
        } finally {
            lock.unlock();
        }
    }

    public void getWhen(String key, String keyCond, byte[] valueCond, AsyncCallback callback) {
        System.out.println("Client: starting getWhen operation");
        new Thread(() -> {
            try {
                lock.lock();
                try {
                    demultiplexer.send(new Frame(Request.GET_WHEN, key, keyCond.getBytes()));
                    byte[] response = demultiplexer.receive(Request.GET_WHEN);
                    callback.onSuccess(response);
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void logout() throws IOException, InterruptedException {
        lock.lock();
        try {
            FrameList frameList = new FrameList();
            frameList.add(new Frame(Request.LOGOUT, "", new byte[0]));
            demultiplexer.send(frameList);
            demultiplexer.receive(Request.LOGOUT); // Read response
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            demultiplexer.close();
        } finally {
            socket.close();
        }
    }

    public interface AsyncCallback {
        void onSuccess(byte[] result);

        void onFailure();

        void onError(Exception e);
    }
}