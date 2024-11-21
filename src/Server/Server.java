package Server;

import Common.Connection;
import Common.Request;
import Common.Accounts;
import Common.Frame;
import Common.FrameList;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Server {

    private static final Accounts accounts = new Accounts();
    private static final ReentrantLock liuLock = new ReentrantLock();
    private static final ReentrantLock storeLock = new ReentrantLock();
    private static final Condition storeCondition = storeLock.newCondition();
    private static final Map<String, byte[]> store = new HashMap<>();
    private static final Set<String> loggedInUsers = new HashSet<>();
    private static final int MAX_SESSIONS = 10;
    private static int currentSessions = 0;

    public static void main(String[] args) throws IOException {
        int port = 8080;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server: Listening on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected from " + clientSocket.getInetAddress().getHostAddress());
            Connection c = new Connection(clientSocket);

            Runnable worker = () -> {
                try {
                    while (true) {
                        FrameList frameList = c.receiveFrameList();
                        System.out.println("Server: Received frame list with size: " + frameList.size());

                        for (Frame frame : frameList) {
                            switch (frame.tag) {
                                case Request.AUTH:
                                    handleAuth(frame, c);
                                    break;
                                case Request.REGISTER:
                                    handleRegister(frame, c);
                                    break;
                                case Request.PUT:
                                    handlePut(frame, c);
                                    break;
                                case Request.GET:
                                    handleGet(frame, c);
                                    break;
                                case Request.MULTI_PUT:
                                    handleMultiPut(frameList, c);
                                    break;
                                case Request.MULTI_GET:
                                    handleMultiGet(frameList, c);
                                    break;
                                case Request.GET_WHEN:
                                    handleGetWhen(frame, c);
                                    break;
                                case Request.LOGOUT:
                                    handleLogout(frame, c);
                                    break;
                                default:
                                    System.out.println("Unknown request type: " + frame.tag);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            new Thread(worker).start();
        }
    }

    private static void handleAuth(Frame frame, Connection c) throws IOException {
        System.out.println("Server: User authentication attempt.");
        String username = frame.stringInput;
        String password = new String(frame.data);

        accounts.l.readLock().lock();
        try {
            if (accounts.accountExists(username)) {
                String storedPassword = accounts.getPassword(username);
                if (storedPassword.equals(password)) {
                    System.out.println("Server: Authentication successful");
                    c.send(new Frame(Request.AUTH, username, "Sessão iniciada com sucesso!".getBytes()));
                    liuLock.lock();
                    try {
                        loggedInUsers.add(username);
                        currentSessions++;
                    } finally {
                        liuLock.unlock();
                    }
                } else {
                    c.send(new Frame(Request.AUTH, username, "Erro - palavra-passe errada.".getBytes()));
                }
            } else {
                c.send(new Frame(Request.AUTH, username, "Erro - conta não existe.".getBytes()));
            }
        } finally {
            accounts.l.readLock().unlock();
        }
    }

    private static void handleRegister(Frame frame, Connection c) throws IOException {
        System.out.println("Server: User registration attempt.");
        String username = frame.stringInput;
        String password = new String(frame.data);

        accounts.l.writeLock().lock();
        try {
            if (accounts.accountExists(username)) {
                System.out.println("Server: Account already exists");
                c.send(new Frame(Request.REGISTER, username,
                        "Erro - endereço de email já pertence a uma conta.".getBytes()));
            } else {
                System.out.println("Server: Creating new account");
                accounts.addAccount(username, password);
                c.send(new Frame(Request.REGISTER, username, "Registo efetuado com sucesso!".getBytes()));
                liuLock.lock();
                try {
                    loggedInUsers.add(username);
                    currentSessions++;
                } finally {
                    liuLock.unlock();
                }
            }
        } finally {
            accounts.l.writeLock().unlock();
        }
    }

    private static void handlePut(Frame frame, Connection c) throws IOException {
        System.out.println("Put request.");
        String key = frame.stringInput;
        byte[] value = frame.data;

        storeLock.lock();
        try {
            store.put(key, value);
            storeCondition.signalAll();
        } finally {
            storeLock.unlock();
        }
        c.send(new Frame(Request.PUT, key, new byte[] { 1 }));
    }

    private static void handleGet(Frame frame, Connection c) throws IOException {
        System.out.println("Get request.");
        String key = frame.stringInput;

        byte[] value;
        storeLock.lock();
        try {
            value = store.get(key);
        } finally {
            storeLock.unlock();
        }
        c.send(new Frame(Request.GET, key, value != null ? value : new byte[0]));
    }

    private static void handleMultiPut(FrameList frameList, Connection c) throws IOException {
        System.out.println("MultiPut request.");
        Map<String, byte[]> pairs = new HashMap<>();
        for (Frame frame : frameList) {
            pairs.put(frame.stringInput, frame.data);
        }

        storeLock.lock();
        try {
            store.putAll(pairs);
            storeCondition.signalAll();
        } finally {
            storeLock.unlock();
        }
        c.send(new Frame(Request.MULTI_PUT, "", new byte[] { 1 }));
    }

    private static void handleMultiGet(FrameList frameList, Connection c) throws IOException {
        System.out.println("MultiGet request.");
        Set<String> keys = new HashSet<>();
        for (Frame frame : frameList) {
            keys.add(frame.stringInput);
        }

        Map<String, byte[]> result = new HashMap<>();
        storeLock.lock();
        try {
            for (String key : keys) {
                result.put(key, store.get(key));
            }
        } finally {
            storeLock.unlock();
        }
        FrameList responseList = new FrameList();
        for (Map.Entry<String, byte[]> entry : result.entrySet()) {
            responseList.add(new Frame(Request.MULTI_GET, entry.getKey(), entry.getValue()));
        }
        c.send(responseList);
    }

    private static void handleGetWhen(Frame frame, Connection c) throws IOException {
        System.out.println("GetWhen request.");
        String keyCond = frame.stringInput;
        byte[] valueCond = frame.data;

        byte[] value;
        storeLock.lock();
        try {
            while (!Arrays.equals(store.get(keyCond), valueCond)) {
                storeCondition.await();
            }
            value = store.get(keyCond);
        } catch (InterruptedException e) {
            throw new IOException(e);
        } finally {
            storeLock.unlock();
        }
        c.send(new Frame(Request.GET_WHEN, keyCond, value != null ? value : new byte[0]));
    }

    private static void handleLogout(Frame frame, Connection c) throws IOException {
        System.out.println("Server: User logout attempt.");
        String username = frame.stringInput;

        liuLock.lock();
        try {
            loggedInUsers.remove(username);
            currentSessions--;
            c.send(new Frame(Request.LOGOUT, username, new byte[] { 1 }));
        } finally {
            liuLock.unlock();
        }
    }
}