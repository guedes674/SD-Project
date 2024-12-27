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

/**
 * Server class that handles client connections and processes requests.
 */
public class Server {

    private static final Map<String, String> credentialsMap = new HashMap<>();
    private static final ReentrantLock liuLock = new ReentrantLock();
    private static final ReentrantLock storeLock = new ReentrantLock();
    private static final Condition storeCondition = storeLock.newCondition();
    private static final Condition loginCondition = liuLock.newCondition();
    private static final Map<String, byte[]> store = new HashMap<>();
    private static final Set<String> loggedInUsers = new HashSet<>();
    private static final Queue<Connection> waitingQueue = new LinkedList<>();
    private static final int MAX_SESSIONS = 3000;
    private static int currentSessions = 0;

    /**
     * Main method to start the server.
     *
     * @param args Command line arguments
     * @throws IOException If an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        int port = 8080;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server: Listening on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            // System.out.println("Client connected from " +
            // clientSocket.getInetAddress().getHostAddress());
            Connection c = new Connection(clientSocket);
            new Thread(() -> handleClient(c)).start();
        }
    }

    /**
     * Handles client connections and processes incoming frames.
     *
     * @param c The client connection
     */
    private static void handleClient(Connection c) {
        try {
            while (true) {
                Frame frame = c.receive();
                new Thread(() -> handleRequest(frame, c)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles incoming requests based on the frame tag.
     *
     * @param frame The received frame
     * @param c     The client connection
     */
    private static void handleRequest(Frame frame, Connection c) {
        try {
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

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles user authentication.
     *
     * @param frame The received frame
     * @param c     The client connection
     * @throws IOException If an I/O error occurs
     */
    private static void handleAuth(Frame frame, Connection c) throws IOException {
        // System.out.println("Server: User authentication attempt.");
        String username = frame.keyValuePairs.keySet().iterator().next();
        String password = new String(frame.keyValuePairs.get(username));

        // Lock the credentials map to prevent concurrent access
        liuLock.lock();
        try {
            // Check if the user exists and if is already logged in
            if (credentialsMap.containsKey(username)) {
                String storedPassword = credentialsMap.get(username);
                if (loggedInUsers.contains(username)) {
                    // System.out.println("Server: User already logged in");
                    c.send(new Frame(Request.AUTH,
                            Collections.singletonMap("ERROR", "Error - user already logged in.".getBytes())));
                }
                // Check if the password is correct
                else if (storedPassword.equals(password)) {

                    // Check if the maximum number of sessions has been reached
                    while (currentSessions >= MAX_SESSIONS) {
                        // System.out.println("Server: Maximum sessions reached. Adding to waiting
                        // queue.");
                        // Add the client to the waiting queue and wait for a signal
                        waitingQueue.add(c);
                        c.send(new Frame(Request.AUTH,
                                Collections.singletonMap("WAIT",
                                        "Waiting for a session to become available...".getBytes())));
                        loginCondition.await();
                    }

                    // System.out.println("Server: Authentication successful");
                    c.send(new Frame(Request.AUTH,
                            Collections.singletonMap(username, "Login made successfully.".getBytes())));
                    loggedInUsers.add(username);
                    currentSessions++;
                    // System.out.println("Current sessions: " + currentSessions);
                } else {
                    c.send(new Frame(Request.AUTH,
                            Collections.singletonMap("ERROR", "Error - Wrong password.".getBytes())));
                }
            } else {
                c.send(new Frame(Request.AUTH,
                        Collections.singletonMap("ERROR", "Error - User not found.".getBytes())));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // System.out.println("Users logged in: " + loggedInUsers);
            liuLock.unlock();
        }
    }

    /**
     * Handles user registration.
     *
     * @param frame The received frame
     * @param c     The client connection
     * @throws IOException If an I/O error occurs
     */
    private static void handleRegister(Frame frame, Connection c) throws IOException {
        // System.out.println("Server: User registration attempt.");
        String username = frame.keyValuePairs.keySet().iterator().next();
        String password = new String(frame.keyValuePairs.get(username));

        liuLock.lock();
        try {
            // Check if the account already exists
            if (credentialsMap.containsKey(username)) {
                // System.out.println("Server: Account already exists");
                c.send(new Frame(Request.REGISTER, Collections.singletonMap("ERROR",
                        "Error - Account already exists.".getBytes())));

            }
            // Create a new account
            else {
                // System.out.println("Server: Creating new account");
                credentialsMap.put(username, password);
                c.send(new Frame(Request.REGISTER,
                        Collections.singletonMap(username, "Successful registration!".getBytes())));
            }
        } finally {
            liuLock.unlock();
        }
    }

    /**
     * Handles multi-put requests.
     *
     * @param frame The received frame
     * @param c     The client connection
     * @throws IOException If an I/O error occurs
     */
    private static void handleMultiPut(Frame frame, Connection c) throws IOException {

        storeLock.lock();
        try {
            // Store each key-value pair in the store
            for (Map.Entry<String, byte[]> entry : frame.keyValuePairs.entrySet()) {
                store.put(entry.getKey(), entry.getValue());
            }
            // Signal all waiting threads that the store has been updated
            storeCondition.signalAll();
        } finally {
            storeLock.unlock();
        }
        // Send a response back to the client indicating success
        c.send(new Frame(Request.PUT, Collections.singletonMap("", new byte[] { 1 })));
    }

    /**
     * Handles multi-get requests.
     *
     * @param frame The received frame
     * @param c     The client connection
     * @throws IOException If an I/O error occurs
     */
    private static void handleMultiGet(Frame frame, Connection c) throws IOException {
        Map<String, byte[]> results = new HashMap<>();

        storeLock.lock();
        try {
            // Retrieve each requested key from the store
            for (String key : frame.keyValuePairs.keySet()) {
                byte[] value = store.get(key);
                results.put(key, value != null ? value : "null".getBytes());
            }
        } finally {
            storeLock.unlock();
        }
        // Send the retrieved key-value pairs back to the client
        c.send(new Frame(Request.GET, results));
    }

    /**
     * Handles get-when requests.
     *
     * @param frame The received frame
     * @param c     The client connection
     * @throws IOException If an I/O error occurs
     */
    private static void handleGetWhen(Frame frame, Connection c) throws IOException {
        Map<String, byte[]> request = frame.keyValuePairs;
        Iterator<String> keysIterator = request.keySet().iterator();

        String key = keysIterator.next();
        String keyCond = keysIterator.next();
        byte[] valueCond = request.get(keyCond);

        byte[] value;
        storeLock.lock();
        try {
            // Wait until the condition key has the specified value
            while (!Arrays.equals(store.get(keyCond), valueCond)) {
                storeCondition.await();
            }
            // Retrieve the value for the requested key
            value = store.get(key);
        } catch (InterruptedException e) {
            value = null;
        } finally {
            storeLock.unlock();
        }
        try {
            // Send the retrieved value back to the client
            c.send(new Frame(Request.GET_WHEN,
                    Collections.singletonMap(key, value != null ? value : "null".getBytes())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles user logout.
     *
     * @param frame The received frame
     * @param c     The client connection
     * @throws IOException If an I/O error occurs
     */
    private static void handleLogout(Frame frame, Connection c) throws IOException {
        // System.out.println("Server: User logout attempt.");
        String username = frame.keyValuePairs.keySet().iterator().next();

        liuLock.lock();
        try {
            // Remove the user from the logged-in users set
            loggedInUsers.remove(username);
            // Decrement the current session count
            currentSessions--;

            // Send a response back to the client indicating success
            c.send(new Frame(Request.LOGOUT, Collections.singletonMap(username, new byte[] { 1 })));

            // If there are clients waiting, signal the next one
            if (!waitingQueue.isEmpty()) {
                waitingQueue.poll();
                loginCondition.signalAll();
            }
        } finally {
            // System.out.println("Current sessions: " + currentSessions);
            liuLock.unlock();
        }
    }
}