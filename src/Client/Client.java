package Client;

import Common.*;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Client implements AutoCloseable {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final ReentrantLock lock = new ReentrantLock();
    private final ReentrantLock readLock = new ReentrantLock();
    private final ReentrantLock outLock = new ReentrantLock();
    private final ReentrantLock inLock = new ReentrantLock();

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    public boolean authenticate(String username, String password) throws IOException {
        lock.lock();
        try {
            out.writeInt(Request.AUTH);
            out.writeUTF(username);
            out.writeUTF(password);
            out.flush();

            readLock.lock();
            try {
                return in.readBoolean();
            } finally {
                readLock.unlock();
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean register(String username, String password) throws IOException {
        lock.lock();
        try {
            out.writeInt(Request.REGISTER);
            out.writeUTF(username);
            out.writeUTF(password);
            out.flush();

            readLock.lock();
            try {
                return in.readBoolean();
            } finally {
                readLock.unlock();
            }
        } finally {
            lock.unlock();
        }
    }

    public void put(String key, byte[] value) throws IOException {
        lock.lock();
        try {
            out.writeInt(Request.PUT);
            out.writeUTF(key);
            out.writeInt(value.length);
            out.write(value);
            out.flush();

            readLock.lock();
            try {
                in.readBoolean(); // Read the response
            } finally {
                readLock.unlock();
            }
        } finally {
            lock.unlock();
        }
    }

    public byte[] get(String key) throws IOException {
        lock.lock();
        try {
            out.writeInt(Request.GET);
            out.writeUTF(key);
            out.flush();

            readLock.lock();
            try {
                boolean success = in.readBoolean();
                if (success) {
                    int length = in.readInt();
                    if (length > 0) {
                        byte[] value = new byte[length];
                        in.readFully(value);
                        return value;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } finally {
                readLock.unlock();
            }
        } finally {
            lock.unlock();
        }
    }

    public void multiPut(Map<String, byte[]> pairs) throws IOException {
        lock.lock();
        try {
            out.writeInt(Request.MULTI_PUT);
            out.writeInt(pairs.size());
            for (Map.Entry<String, byte[]> entry : pairs.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeInt(entry.getValue().length);
                out.write(entry.getValue());
            }
            out.flush();

            readLock.lock();
            try {
                in.readBoolean(); // Read the response
            } finally {
                readLock.unlock();
            }
        } finally {
            lock.unlock();
        }
    }

    public Map<String, byte[]> multiGet(Set<String> keys) throws IOException {
        lock.lock();
        try {
            out.writeInt(Request.MULTI_GET);
            out.writeInt(keys.size());
            for (String key : keys) {
                out.writeUTF(key);
            }
            out.flush();

            readLock.lock();
            try {
                boolean success = in.readBoolean();
                if (success) {
                    int size = in.readInt();
                    Map<String, byte[]> values = new HashMap<>();
                    for (int i = 0; i < size; i++) {
                        String key = in.readUTF();
                        int length = in.readInt();
                        if (length > 0) {
                            byte[] value = new byte[length];
                            in.readFully(value);
                            values.put(key, value);
                        } else {
                            values.put(key, null);
                        }
                    }
                    return values;
                } else {
                    return null;
                }
            } finally {
                readLock.unlock();
            }
        } finally {
            lock.unlock();
        }
    }

    public void getWhen(String key, String keyCond, byte[] valueCond, AsyncCallback callback) {
        System.out.println("Client: starting getWhen operation");
        new Thread(() -> {
            outLock.lock();
            try {
                System.out.println("Client: sending request");
                out.writeInt(Request.GET_WHEN);
                out.writeUTF(key);
                out.writeUTF(keyCond);
                out.writeInt(valueCond.length);
                out.write(valueCond);
                out.flush();
            } catch (IOException e) {
                System.out.println("Client: error: " + e.getMessage());
            } finally {
                outLock.unlock();
            }

            try {
                inLock.lock();
                try {
                    System.out.println("Client: reading response");
                    boolean success = in.readBoolean();
                    System.out.println("Client: success=" + success);

                    if (success) {
                        int length = in.readInt();
                        System.out.println("Client: length=" + length);
                        if (length > 0) {
                            byte[] value = new byte[length];
                            in.readFully(value);
                            System.out.println("Client: received value: " + new String(value));
                            callback.onSuccess(value);
                        } else {
                            callback.onSuccess(null);
                        }
                    } else {
                        callback.onFailure();
                    }
                } finally {
                    inLock.unlock();
                }
            } catch (IOException e) {
                System.out.println("Client: error: " + e.getMessage());
                e.printStackTrace();
                callback.onError(e);
            }
        }).start();
    }

    public void logout() throws IOException {
        lock.lock();
        try {
            out.writeInt(Request.LOGOUT);
            out.flush();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            logout();
        } finally {
            socket.close();
        }
    }

    public interface AsyncCallback {
        void onSuccess(byte[] result);

        void onFailure();

        void onError(Exception e);
    }
}