package Client;

import Common.*;

import java.net.*;
import java.io.*;
import java.util.*;

public class Client implements AutoCloseable {
    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;

    public Client(String host, int port) throws IOException {
        System.out.println(host);
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public boolean authenticate(String username, String password) throws IOException, ClassNotFoundException {
        out.writeObject(new AuthRequest(username, password));
        Response response = (Response) in.readObject();
        return response.success;
    }

    public boolean register(String username, String password) throws IOException, ClassNotFoundException {
        out.writeObject(new RegisterRequest(username, password));
        Response response = (Response) in.readObject();
        return response.success;
    }

    public void put(String key, byte[] value) throws IOException, ClassNotFoundException {
        out.writeObject(new PutRequest(key, value));
        in.readObject();
    }

    public byte[] get(String key) throws IOException, ClassNotFoundException {
        out.writeObject(new GetRequest(key));
        GetResponse response = (GetResponse) in.readObject();
        return response.value;
    }

    public void multiPut(Map<String, byte[]> pairs) throws IOException, ClassNotFoundException {
        out.writeObject(new MultiPutRequest(pairs));
        in.readObject();
    }

    public Map<String, byte[]> multiGet(Set<String> keys) throws IOException, ClassNotFoundException {
        out.writeObject(new MultiGetRequest(keys));
        MultiGetResponse response = (MultiGetResponse) in.readObject();
        return response.values;
    }

    public void logout() throws IOException {
        out.writeObject(new LogoutRequest());
    }

    @Override
    public void close() throws IOException {
        try {
            logout();
        } finally {
            socket.close();
        }
    }
}