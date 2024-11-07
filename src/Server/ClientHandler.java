package Server;

import Common.*;

import java.net.*;
import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final Server server;
    private final DataInputStream in;
    private final DataOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.clientSocket = socket;
        this.server = server;
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            while (true) {
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
                    case Request.LOGOUT:
                        System.out.println("Logging out");
                        handleLogout();
                        return;
                    default:
                        throw new IllegalArgumentException("Unknown request type: " + requestType);
                }
            }
        } catch (IOException e) {
            handleLogout();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        System.out.println("Registering");

        String username = in.readUTF();
        String password = in.readUTF();

        System.out.println(username);
        boolean success = server.register(username, password);

        System.out.println(success ? "Registration successful" : "Registration failed");
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
        Map<String, byte[]> values = server.multiGet(keys);
        if (values != null) {
            out.writeBoolean(true);
            out.writeInt(values.size());
            for (Map.Entry<String, byte[]> entry : values.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeInt(entry.getValue().length);
                out.write(entry.getValue());
            }
        } else {
            out.writeBoolean(false);
        }
        out.flush();
    }

    private void handleLogout() {
        if (username != null) {
            server.logout(username);
        }
    }

    private void sendNotAuthenticatedResponse() throws IOException {
        out.writeBoolean(false);
        out.flush();
    }
}