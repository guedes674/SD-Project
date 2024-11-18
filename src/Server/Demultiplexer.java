package Server;

import Common.Request;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Demultiplexer {
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Server server;
    private final ReentrantLock lock = new ReentrantLock();
    private String username;

    public Demultiplexer(DataInputStream in, DataOutputStream out, Server server) {
        this.in = in;
        this.out = out;
        this.server = server;
    }

    public void handleRequest() throws IOException, InterruptedException {
        int requestType = in.readInt();

        switch (requestType) {
            case Request.AUTH:
                handleAuth();
                break;
            case Request.REGISTER:
                handleRegister();
                break;
            case Request.PUT:
                handlePut();
                break;
            case Request.GET:
                handleGet();
                break;
            case Request.MULTI_PUT:
                handleMultiPut();
                break;
            case Request.MULTI_GET:
                handleMultiGet();
                break;
            case Request.GET_WHEN:
                handleGetWhen();
                break;
            case Request.LOGOUT:
                handleLogout();
                break;
            default:
                throw new IOException("Unknown request type");
        }
    }

    private void handleGetWhen() throws IOException {
        String key = in.readUTF();
        String keyCond = in.readUTF();
        int length = in.readInt();
        byte[] valueCond = new byte[length];
        in.readFully(valueCond);

        System.out.println("Demultiplexer: handling getWhen for key " + key);

        new Thread(() -> {
            try {
                byte[] result = server.getWhen(key, keyCond, valueCond);
                lock.lock();
                try {
                    System.out.println("Demultiplexer: sending response, result="
                            + (result != null ? new String(result) : "null"));
                    out.writeBoolean(true);
                    if (result != null) {
                        out.writeInt(result.length);
                        out.write(result);
                    } else {
                        out.writeInt(0);
                    }
                    out.flush();
                    System.out.println("Demultiplexer: response sent successfully");
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                lock.lock();
                try {
                    System.out.println("Demultiplexer: sending error response: " + e.getMessage());
                    out.writeBoolean(false);
                    out.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }).start();
    }

    private void handleAuth() throws IOException, InterruptedException {
        String username = in.readUTF();
        String password = in.readUTF();

        lock.lock();
        try {
            boolean success = server.authenticate(username, password);
            if (success) {
                this.username = username;
            }
            out.writeBoolean(success);
            out.flush();
        } finally {
            lock.unlock();
        }
    }

    private void handleRegister() throws IOException {
        String username = in.readUTF();
        String password = in.readUTF();

        lock.lock();
        try {
            boolean success = server.register(username, password);
            out.writeBoolean(success);
            out.flush();
        } finally {
            lock.unlock();
        }
    }

    private void handlePut() throws IOException {
        if (username == null) {
            sendNotAuthenticatedResponse();
            return;
        }

        String key = in.readUTF();
        int length = in.readInt();
        byte[] value = new byte[length];
        in.readFully(value);

        lock.lock();
        try {
            server.put(key, value);
            out.writeBoolean(true);
            out.flush();
        } finally {
            lock.unlock();
        }
    }

    private void handleGet() throws IOException {
        if (username == null) {
            sendNotAuthenticatedResponse();
            return;
        }

        String key = in.readUTF();

        lock.lock();
        try {
            byte[] value = server.get(key);
            out.writeBoolean(true);
            if (value != null) {
                out.writeInt(value.length);
                out.write(value);
            } else {
                out.writeInt(0);
            }
            out.flush();
        } finally {
            lock.unlock();
        }
    }

    private void handleMultiPut() throws IOException {
        if (username == null) {
            sendNotAuthenticatedResponse();
            return;
        }

        int numPairs = in.readInt();
        Map<String, byte[]> pairs = new HashMap<>();
        for (int i = 0; i < numPairs; i++) {
            String key = in.readUTF();
            int length = in.readInt();
            byte[] value = new byte[length];
            in.readFully(value);
            pairs.put(key, value);
        }

        lock.lock();
        try {
            server.multiPut(pairs);
            out.writeBoolean(true);
            out.flush();
        } finally {
            lock.unlock();
        }
    }

    private void handleMultiGet() throws IOException {
        if (username == null) {
            sendNotAuthenticatedResponse();
            return;
        }

        int numKeys = in.readInt();
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < numKeys; i++) {
            keys.add(in.readUTF());
        }

        lock.lock();
        try {
            Map<String, byte[]> result = server.multiGet(keys);
            out.writeBoolean(true);
            out.writeInt(result.size());
            for (Map.Entry<String, byte[]> entry : result.entrySet()) {
                out.writeUTF(entry.getKey());
                if (entry.getValue() != null) {
                    out.writeInt(entry.getValue().length);
                    out.write(entry.getValue());
                } else {
                    out.writeInt(0);
                }
            }
            out.flush();
        } finally {
            lock.unlock();
        }
    }

    private void handleLogout() throws IOException {
        if (username != null) {
            lock.lock();
            try {
                server.logout(username);
                username = null;
            } finally {
                lock.unlock();
            }
        }
    }

    private void sendNotAuthenticatedResponse() throws IOException {
        lock.lock();
        try {
            out.writeBoolean(false);
            out.flush();
        } finally {
            lock.unlock();
        }
    }
}