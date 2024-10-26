package Server;

// Server/ClientHandler.java

import Common.*;

import java.net.*;
import java.io.*;
import java.util.Map;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final Server server;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.clientSocket = socket;
        this.server = server;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            while (true) {
                Request request = (Request) in.readObject();

                if (request instanceof AuthRequest) {
                    handleAuth((AuthRequest) request);
                } else if (request instanceof RegisterRequest) {
                    handleRegister((RegisterRequest) request);
                } else if (username == null) {
                    out.writeObject(new Response(false, "Not authenticated"));
                    continue;
                }

                if (request instanceof PutRequest) {
                    handlePut((PutRequest) request);
                } else if (request instanceof GetRequest) {
                    handleGet((GetRequest) request);
                } else if (request instanceof MultiPutRequest) {
                    handleMultiPut((MultiPutRequest) request);
                } else if (request instanceof MultiGetRequest) {
                    handleMultiGet((MultiGetRequest) request);
                } else if (request instanceof LogoutRequest) {
                    handleLogout();
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            handleLogout();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleAuth(AuthRequest request) throws IOException {
        boolean success = server.authenticate(request.username, request.password);
        if (success) {
            username = request.username;
        }
        out.writeObject(new Response(success, success ? "Authentication successful" : "Authentication failed"));
    }

    private void handleRegister(RegisterRequest request) throws IOException {
        boolean success = server.register(request.username, request.password);
        out.writeObject(new Response(success, success ? "Registration successful" : "Username already exists"));
    }

    private void handlePut(PutRequest request) throws IOException {
        server.put(request.key, request.value);
        out.writeObject(new Response(true, "Put successful"));
    }

    private void handleGet(GetRequest request) throws IOException {
        byte[] value = server.get(request.key);
        out.writeObject(new GetResponse(value));
    }

    private void handleMultiPut(MultiPutRequest request) throws IOException {
        server.multiPut(request.pairs);
        out.writeObject(new Response(true, "MultiPut successful"));
    }

    private void handleMultiGet(MultiGetRequest request) throws IOException {
        Map<String, byte[]> values = server.multiGet(request.keys);
        out.writeObject(new MultiGetResponse(values));
    }

    private void handleLogout() {
        if (username != null) {
            server.logout(username);
        }
    }
}