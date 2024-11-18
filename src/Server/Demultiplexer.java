package Server;

import Common.Request;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class Demultiplexer {
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Server server;
    private final ReentrantLock outLock = new ReentrantLock();
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
                if (username == null) {
                    sendNotAuthenticatedResponse();
                } else {
                    handlePut();
                }
                break;
            case Request.GET:
                if (username == null) {
                    sendNotAuthenticatedResponse();
                } else {
                    handleGet();
                }
                break;
            case Request.MULTI_PUT:
                if (username == null) {
                    sendNotAuthenticatedResponse();
                } else {
                    handleMultiPut();
                }
                break;
            case Request.MULTI_GET:
                if (username == null) {
                    sendNotAuthenticatedResponse();
                } else {
                    handleMultiGet();
                }
                break;
            case Request.GET_WHEN:
                if (username == null) {
                    sendNotAuthenticatedResponse();
                } else {
                    handleGetWhen();
                }
                break;
            case Request.LOGOUT:
                if (username == null) {
                    sendNotAuthenticatedResponse();
                } else {
                    handleLogout();
                }
                break;
            default:
                System.out.println("Unknown request type: " + requestType);
        }
    }

    private void handleAuth() throws IOException, InterruptedException {
        String username = in.readUTF();
        String password = in.readUTF();
        boolean success = server.authenticate(username, password);
        if (success) {
            this.username = username;
        }
        out.writeBoolean(success);
        out.flush();
    }

    private void handleRegister() throws IOException {
        String username = in.readUTF();
        String password = in.readUTF();
        boolean success = server.register(username, password);
        out.writeBoolean(success);
        out.flush();
    }

    private void handlePut() throws IOException {
        String key = in.readUTF();
        int length = in.readInt();
        byte[] value = new byte[length];
        in.readFully(value);
        server.put(key, value);
        out.writeBoolean(true);
        out.flush();
    }

    private void handleGet() throws IOException {
        String key = in.readUTF();
        byte[] value = server.get(key);
        if (value != null) {
            out.writeBoolean(true);
            out.writeInt(value.length);
            out.write(value);
        } else {
            out.writeBoolean(false);
        }
        out.flush();
    }

    private void handleMultiPut() throws IOException {
        int size = in.readInt();
        Map<String, byte[]> pairs = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = in.readUTF();
            int length = in.readInt();
            byte[] value = new byte[length];
            in.readFully(value);
            pairs.put(key, value);
        }
        server.multiPut(pairs);
        out.writeBoolean(true);
        out.flush();
    }

    private void handleMultiGet() throws IOException {
        int size = in.readInt();
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < size; i++) {
            keys.add(in.readUTF());
        }
        Map<String, byte[]> result = server.multiGet(keys);
        out.writeBoolean(true);
        out.writeInt(result.size());
        for (Map.Entry<String, byte[]> entry : result.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue().length);
            out.write(entry.getValue());
        }
        out.flush();
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
                outLock.lock();
                try {
                    System.out.println("Demultiplexer: sending response, result is " + (new String(result)));
                    out.writeBoolean(true); // Sending success
                    if (result != null) {
                        out.writeInt(result.length);
                        out.write(result);
                    } else {
                        out.writeInt(0);
                    }
                    out.flush();
                    System.out.println("Demultiplexer: response sent successfully");
                } finally {
                    outLock.unlock();
                }
            } catch (Exception e) {
                System.out.println("Demultiplexer: getWhen failed with error: " + e.getMessage());
                outLock.lock();
                try {
                    out.writeBoolean(false);
                    out.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    outLock.unlock();
                }
            }
        }).start();
    }

    private void handleLogout() throws IOException {
        server.logout(username);
        username = null;
        out.writeBoolean(true);
        out.flush();
    }

    private void sendNotAuthenticatedResponse() throws IOException {
        out.writeBoolean(false);
        out.flush();
    }
}