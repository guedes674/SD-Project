package Client;

import Common.*;

import java.net.*;
import java.io.*;
import java.util.*;

public class Client implements AutoCloseable {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public Client(String host, int port) throws IOException {
        System.out.println(host);
        socket = new Socket(host, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    public boolean authenticate(String username, String password) throws IOException {
        System.out.println("Authenticating");
        out.writeInt(Request.AUTH);
        out.writeUTF(username);
        out.writeUTF(password);
        out.flush();

        boolean success = in.readBoolean();
        return success;
    }

    public boolean register(String username, String password) throws IOException {
        System.out.println("Registering");
        System.out.println(username);
        System.out.println(password);
        System.out.println(Request.REGISTER);
        out.writeInt(Request.REGISTER);
        out.writeUTF(username);
        out.writeUTF(password);
        out.flush();

        boolean success = in.readBoolean();
        return success;
    }

    public void put(String key, byte[] value) throws IOException {
        out.writeInt(Request.PUT);
        out.writeUTF(key);
        out.writeInt(value.length);
        out.write(value);
        out.flush();

        in.readBoolean(); // Read the response
    }

    public byte[] get(String key) throws IOException {
        out.writeInt(Request.GET);
        out.writeUTF(key);
        out.flush();

        boolean success = in.readBoolean();
        if (success) {
            int length = in.readInt();
            byte[] value = new byte[length];
            in.readFully(value);
            return value;
        } else {
            return null;
        }
    }

    public void multiPut(Map<String, byte[]> pairs) throws IOException {
        out.writeInt(Request.MULTI_PUT);
        out.writeInt(pairs.size());
        for (Map.Entry<String, byte[]> entry : pairs.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue().length);
            out.write(entry.getValue());
        }
        out.flush();

        in.readBoolean(); // Read the response
    }

    public Map<String, byte[]> multiGet(Set<String> keys) throws IOException {
        out.writeInt(Request.MULTI_GET);
        out.writeInt(keys.size());
        for (String key : keys) {
            out.writeUTF(key);
        }
        out.flush();

        boolean success = in.readBoolean();
        if (success) {
            int size = in.readInt();
            Map<String, byte[]> values = new HashMap<>();
            for (int i = 0; i < size; i++) {
                String key = in.readUTF();
                int length = in.readInt();
                byte[] value = new byte[length];
                in.readFully(value);
                values.put(key, value);
            }
            return values;
        } else {
            return null;
        }
    }

    public byte[] getWhen(String key, String keyCond, byte[] valueCond) throws IOException {
        out.writeInt(Request.GET_WHEN);
        out.writeUTF(key);
        out.writeUTF(keyCond);
        out.writeInt(valueCond.length);
        out.write(valueCond);
        out.flush();
        boolean success = in.readBoolean();
        if (success) {
            int length = in.readInt();
            byte[] value = new byte[length];
            in.readFully(value);
            return value;
        } else {
            return null;
        }
    }

    public void logout() throws IOException {
        out.writeInt(Request.LOGOUT);
        out.flush();
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