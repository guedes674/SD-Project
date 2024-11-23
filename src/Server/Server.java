package Server;

import Common.Connection;
import Common.Request;
import Common.Frame;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Server {

    private static final Map<String, String> credentialsMap = new HashMap<>();
    private static final ReentrantLock liuLock = new ReentrantLock();
    private static final ReentrantLock storeLock = new ReentrantLock();
    private static final Condition storeCondition = storeLock.newCondition();
    private static final Condition loginCondition = liuLock.newCondition();
    private static final Map<String, byte[]> store = new HashMap<>();
    private static final Set<String> loggedInUsers = new HashSet<>();
    private static final Queue<Connection> waitingQueue = new LinkedList<>();
    private static final int MAX_SESSIONS = 1;
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
                        Frame frame = c.receive();
                        switch (frame.tag) {
                            case Request.AUTH:
                                handleAuth(frame, c);
                                break;
                            case Request.REGISTER:
                                handleRegister(frame, c);
                                break;
                            case Request.PUT:
                                handleMultiPut(frame, c);
                                break;
                            case Request.GET:
                                handleMultiGet(frame, c);
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            new Thread(worker).start();
        }
    }

    private static void handleAuth(Frame frame, Connection c) throws IOException {
        System.out.println("Server: User authentication attempt.");
        String username = frame.keyValuePairs.keySet().iterator().next();
        String password = new String(frame.keyValuePairs.get(username));

        liuLock.lock();
        try {
            System.out.println("Server: Authenticating user " + username);
            System.out.println("Logging in users: " + loggedInUsers);
            if (credentialsMap.containsKey(username)) {
                String storedPassword = credentialsMap.get(username);
                if (loggedInUsers.contains(username)) {
                    System.out.println("Server: User already logged in");
                    c.send(new Frame(Request.AUTH,
                            Collections.singletonMap("ERROR", "Erro - sessão já iniciada.".getBytes())));
                } else if (storedPassword.equals(password)) {
                    while (currentSessions >= MAX_SESSIONS) {
                        System.out.println("Server: Maximum sessions reached. Adding to waiting queue.");
                        waitingQueue.add(c);
                        c.send(new Frame(Request.AUTH,
                                Collections.singletonMap("WAIT",
                                        "Aguarde, por favor. Sessões máximas atingidas.".getBytes())));
                        loginCondition.await();
                    }
                    System.out.println("Server: Authentication successful");
                    c.send(new Frame(Request.AUTH,
                            Collections.singletonMap(username, "Sessão iniciada com sucesso!".getBytes())));
                    loggedInUsers.add(username);
                    currentSessions++;
                    System.out.println("Current sessions: " + currentSessions);
                } else {
                    c.send(new Frame(Request.AUTH,
                            Collections.singletonMap("ERRO", "Erro - palavra-passe errada.".getBytes())));
                }
            } else {
                c.send(new Frame(Request.AUTH,
                        Collections.singletonMap("ERRO", "Erro - conta não existe.".getBytes())));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            liuLock.unlock();
        }
    }

    private static void handleRegister(Frame frame, Connection c) throws IOException {
        System.out.println("Server: User registration attempt.");
        String username = frame.keyValuePairs.keySet().iterator().next();
        String password = new String(frame.keyValuePairs.get(username));

        liuLock.lock();
        try {
            if (credentialsMap.containsKey(username)) {
                System.out.println("Server: Account already exists");
                c.send(new Frame(Request.REGISTER, Collections.singletonMap(username,
                        "Erro - endereço de email já pertence a uma conta.".getBytes())));
            } else {
                System.out.println("Server: Creating new account");
                credentialsMap.put(username, password);
                c.send(new Frame(Request.REGISTER,
                        Collections.singletonMap(username, "Registo efetuado com sucesso!".getBytes())));
                System.out.println("Current sessions: " + currentSessions);
            }
        } finally {
            liuLock.unlock();
        }
    }

    private static void handleMultiPut(Frame frame, Connection c) throws IOException {
        System.out.println("MultiPut request.");

        storeLock.lock();
        try {
            for (Map.Entry<String, byte[]> entry : frame.keyValuePairs.entrySet()) {
                store.put(entry.getKey(), entry.getValue());
            }
            storeCondition.signalAll();
        } finally {
            storeLock.unlock();
        }
        c.send(new Frame(Request.PUT, Collections.singletonMap("", new byte[] { 1 })));
    }

    private static void handleMultiGet(Frame frame, Connection c) throws IOException {
        System.out.println("MultiGet request.");
        Map<String, byte[]> results = new HashMap<>();

        storeLock.lock();
        try {
            for (String key : frame.keyValuePairs.keySet()) {
                byte[] value = store.get(key);
                results.put(key, value != null ? value : new byte[0]);
            }
        } finally {
            storeLock.unlock();
        }
        c.send(new Frame(Request.GET, results));
    }

    private static void handleGetWhen(Frame frame, Connection c) throws IOException {
        System.out.println("GetWhen request.");
        String keyCond = frame.keyValuePairs.keySet().iterator().next();
        byte[] valueCond = frame.keyValuePairs.get(keyCond);

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
        c.send(new Frame(Request.GET_WHEN, Collections.singletonMap(keyCond, value != null ? value : new byte[0])));
    }

    private static void handleLogout(Frame frame, Connection c) throws IOException {
        System.out.println("Server: User logout attempt.");
        String username = frame.keyValuePairs.keySet().iterator().next();

        liuLock.lock();
        try {
            loggedInUsers.remove(username);
            currentSessions--;
            System.out.println("Current sessions: " + currentSessions);
            c.send(new Frame(Request.LOGOUT, Collections.singletonMap(username, new byte[] { 1 })));
            if (!waitingQueue.isEmpty()) {
                Connection nextClient = waitingQueue.poll();
                loginCondition.signalAll();
            }
        } finally {
            liuLock.unlock();
        }
    }
}