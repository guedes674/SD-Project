package Client;

import Common.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Client implements AutoCloseable {
    private final Demultiplexer demultiplexer;
    private final ReentrantLock lock = new ReentrantLock();
    public String username;

    public Client(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        Connection connection = new Connection(socket);
        this.demultiplexer = new Demultiplexer(connection);
        this.demultiplexer.start();
        this.username = null;
    }

    public void put(String key, byte[] value) throws IOException, InterruptedException {
        lock.lock();
        try {
            Map<String, byte[]> pair = new HashMap<>();
            pair.put(key, value);
            Frame frame = new Frame(Request.PUT, pair);
            demultiplexer.send(frame);
            demultiplexer.receive(Request.PUT); // Read response for the entire operation
        } finally {
            lock.unlock();
        }
    }

    public byte[] get(String key) throws IOException, InterruptedException {
        lock.lock();
        try {
            Map<String, byte[]> request = new HashMap<>();
            request.put(key, new byte[0]);
            demultiplexer.send(new Frame(Request.GET, request));
            Frame responseFrame = demultiplexer.receive(Request.GET);
            return responseFrame.keyValuePairs.get(key);
        } finally {
            lock.unlock();
        }
    }

    public void multiPut(Map<String, byte[]> pairs) throws IOException, InterruptedException {
        lock.lock();
        try {
            Frame frame = new Frame(Request.PUT, pairs);
            demultiplexer.send(frame);
            demultiplexer.receive(Request.PUT); // Read response for the entire operation
        } finally {
            lock.unlock();
        }
    }

    public Map<String, byte[]> multiGet(Set<String> keys) throws IOException, InterruptedException {
        lock.lock();
        try {
            Map<String, byte[]> request = new HashMap<>();
            for (String key : keys) {
                request.put(key, new byte[0]);
            }
            demultiplexer.send(new Frame(Request.GET, request));
            Frame responseFrame = demultiplexer.receive(Request.GET);
            return responseFrame.keyValuePairs;
        } finally {
            lock.unlock();
        }
    }

    public void getWhen(String key, String keyCond, byte[] valueCond, AsyncCallback callback) {
        System.out.println("Client: starting getWhen operation");
        new Thread(() -> {
            try {
                Map<String, byte[]> request = new HashMap<>();
                request.put(key, new byte[0]);
                request.put(keyCond, valueCond);

                demultiplexer.send(new Frame(Request.GET_WHEN, request));

                Frame responseFrame = demultiplexer.receive(Request.GET_WHEN);

                callback.onSuccess(responseFrame.keyValuePairs.get(key));
            } catch (IOException | InterruptedException e) {
                callback.onError(e);
            }
        }).start();
    }

    public boolean register(String username, String password) throws IOException, InterruptedException {
        Map<String, byte[]> credentials = new HashMap<>();
        credentials.put(username, password.getBytes());
        demultiplexer.send(new Frame(Request.REGISTER, credentials));
        Frame responseFrame = demultiplexer.receive(Request.REGISTER);

        if (responseFrame.keyValuePairs.containsKey("ERROR")) {
            throw new IOException(new String(responseFrame.keyValuePairs.get("ERROR")));
        }

        return responseFrame.keyValuePairs.containsKey("Registo efetuado com sucesso!");
    }

    public boolean authenticate(String username, String password) throws IOException, InterruptedException {
        Map<String, byte[]> credentials = new HashMap<>();
        credentials.put(username, password.getBytes());
        demultiplexer.send(new Frame(Request.AUTH, credentials));
        Frame responseFrame = demultiplexer.receive(Request.AUTH);

        while (responseFrame.keyValuePairs.containsKey("WAIT")) {
            System.out.println(new String(responseFrame.keyValuePairs.get("WAIT")));
            responseFrame = demultiplexer.receive(Request.AUTH);
        }

        if (responseFrame.keyValuePairs.containsKey("ERROR")) {
            throw new IOException(new String(responseFrame.keyValuePairs.get("ERROR")));
        }

        this.username = username;
        return true;
    }

    public void logout() throws IOException, InterruptedException {
        if (username != null) {
            Map<String, byte[]> request = new HashMap<>();
            request.put(username, new byte[0]);
            demultiplexer.send(new Frame(Request.LOGOUT, request));
            demultiplexer.receive(Request.LOGOUT);
            this.username = null;
        }
    }

    public interface AsyncCallback {
        void onSuccess(byte[] result);

        void onFailure();

        void onError(Exception e);
    }

    @Override
    public void close() throws IOException, InterruptedException {
        logout();
        demultiplexer.close();

    }
}