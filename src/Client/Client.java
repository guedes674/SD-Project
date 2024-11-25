package Client;

import Common.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Client class that handles communication with the server.
 */
public class Client implements AutoCloseable {
    private final Demultiplexer demultiplexer;
    private final ReentrantLock lock = new ReentrantLock();
    public String username;

    /**
     * Constructs a new Client and connects to the server.
     *
     * @param host The server host
     * @param port The server port
     * @throws IOException If an I/O error occurs
     */
    public Client(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        Connection connection = new Connection(socket);
        this.demultiplexer = new Demultiplexer(connection);
        this.demultiplexer.start();
        this.username = null;
    }

    /**
     * Sends a PUT request to the server.
     *
     * @param key   The key to store
     * @param value The value to store
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If the thread is interrupted
     */
    public void put(String key, byte[] value) throws IOException, InterruptedException {
        lock.lock();
        try {
            Map<String, byte[]> pair = new HashMap<>();
            pair.put(key, value);
            Frame frame = new Frame(Request.PUT, pair);
            demultiplexer.send(frame);
            demultiplexer.receive(Request.PUT);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sends a GET request to the server.
     *
     * @param key The key to retrieve
     * @return The value associated with the key
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If the thread is interrupted
     */
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

    /**
     * Sends a multi-put request to the server.
     *
     * @param pairs The key-value pairs to store
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If the thread is interrupted
     */
    public void multiPut(Map<String, byte[]> pairs) throws IOException, InterruptedException {
        lock.lock();
        try {
            Frame frame = new Frame(Request.PUT, pairs);
            demultiplexer.send(frame);
            demultiplexer.receive(Request.PUT);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sends a multi-get request to the server.
     *
     * @param keys The keys to retrieve
     * @return The key-value pairs retrieved from the server
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If the thread is interrupted
     */
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

    /**
     * Sends a get-when request to the server.
     *
     * @param key       The key to retrieve
     * @param keyCond   The condition key
     * @param valueCond The condition value
     * @param callback  The callback to handle the response
     */
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

    /**
     * Registers a new user with the server.
     *
     * @param username The username
     * @param password The password
     * @return True if registration is successful, false otherwise
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If the thread is interrupted
     */
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

    /**
     * Authenticates a user with the server.
     *
     * @param username The username
     * @param password The password
     * @return True if authentication is successful, false otherwise
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If the thread is interrupted
     */
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

    /**
     * Logs out the current user.
     *
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If the thread is interrupted
     */
    public void logout() throws IOException, InterruptedException {
        if (username != null) {
            Map<String, byte[]> request = new HashMap<>();
            request.put(username, new byte[0]);
            demultiplexer.send(new Frame(Request.LOGOUT, request));
            demultiplexer.receive(Request.LOGOUT);
            this.username = null;
        }
    }

    /**
     * Interface for asynchronous callbacks.
     */
    public interface AsyncCallback {
        void onSuccess(byte[] result);

        void onFailure();

        void onError(Exception e);
    }

    /**
     * Closes the client, logging out and closing the demultiplexer.
     *
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If the thread is interrupted
     */
    @Override
    public void close() throws IOException, InterruptedException {
        logout();
        demultiplexer.close();
    }
}