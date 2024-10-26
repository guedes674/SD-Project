package Server;

// Server/Server.java
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class Server {
    private final Map<String, byte[]> store = new HashMap<>();
    private final Map<String, String> users = new HashMap<>(); // username -> password
    private final Set<String> activeSessions = new HashSet<>();
    private final int maxSessions;

    private final ReentrantLock storeLock = new ReentrantLock();
    private final ReentrantLock sessionLock = new ReentrantLock();
    private final Condition sessionAvailable = sessionLock.newCondition();

    public Server(int maxSessions) {
        this.maxSessions = maxSessions;
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket, this).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean authenticate(String username, String password) {
        sessionLock.lock();
        try {
            if (!users.containsKey(username) || !users.get(username).equals(password)) {
                return false;
            }

            while (activeSessions.size() >= maxSessions) {
                sessionAvailable.await();
            }

            activeSessions.add(username);
            return true;
        } catch (InterruptedException e) {
            return false;
        } finally {
            sessionLock.unlock();
        }
    }

    public void logout(String username) {
        sessionLock.lock();
        try {
            activeSessions.remove(username);
            sessionAvailable.signalAll();
        } finally {
            sessionLock.unlock();
        }
    }

    public boolean register(String username, String password) {
        sessionLock.lock();
        try {
            if (users.containsKey(username)) {
                return false;
            }
            users.put(username, password);
            return true;
        } finally {
            sessionLock.unlock();
        }
    }

    public void put(String key, byte[] value) {
        storeLock.lock();
        try {
            store.put(key, value);
        } finally {
            storeLock.unlock();
        }
    }

    public byte[] get(String key) {
        storeLock.lock();
        try {
            return store.get(key);
        } finally {
            storeLock.unlock();
        }
    }

    public void multiPut(Map<String, byte[]> pairs) {
        storeLock.lock();
        try {
            store.putAll(pairs);
        } finally {
            storeLock.unlock();
        }
    }

    public Map<String, byte[]> multiGet(Set<String> keys) {
        storeLock.lock();
        try {
            Map<String, byte[]> result = new HashMap<>();
            for (String key : keys) {
                byte[] value = store.get(key);
                if (value != null) {
                    result.put(key, value);
                }
            }
            return result;
        } finally {
            storeLock.unlock();
        }
    }
}