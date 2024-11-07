package Server;

import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.*;

public class Server {
    private final int maxSessions;
    private final Map<String, String> users = new HashMap<>();
    private final Map<String, byte[]> activeSessions = new HashMap<>();
    private final Map<String, byte[]> store = new HashMap<>();
    private final Lock sessionLock = new ReentrantLock();
    private final Condition sessionAvailable = sessionLock.newCondition();
    private final Lock storeLock = new ReentrantLock();

    public Server(int maxSessions) {
        this.maxSessions = maxSessions;
    }

    public void start(int port) throws InterruptedException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket, this).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean authenticate(String username, String password) throws InterruptedException {
        sessionLock.lock();
        try {
            if (!users.containsKey(username) || !users.get(username).equals(password)) {
                return false;
            }
            System.out.println("User " + username + " logged in" + " (" + activeSessions + " active sessions)");
            while (activeSessions.size() >= maxSessions) {
                sessionAvailable.await();
            }
            activeSessions.put(username, new byte[0]);
            return true;
        } finally {
            sessionLock.unlock();
        }
    }

    public void logout(String username) {
        sessionLock.lock();
        try {
            System.out.println("User " + username + " logging out");
            activeSessions.remove(username);
            sessionAvailable.signalAll();
            System.out.println("User " + username + " logged out" + " (" + activeSessions + " active sessions)");
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
                if (store.containsKey(key)) {
                    result.put(key, store.get(key));
                }
            }
            return result;
        } finally {
            storeLock.unlock();
        }
    }
}