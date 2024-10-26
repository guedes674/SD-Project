package Client;

import java.util.*;

public class CLI {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Enter server host:");
        String host = scanner.nextLine();

        System.out.println("Enter server port:");
        int port = Integer.parseInt(scanner.nextLine());

        try (Client client = new Client(host, port)) {
            while (true) {
                System.out.println("\n1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");

                int choice = Integer.parseInt(scanner.nextLine());

                if (choice == 1) {
                    handleRegister(client);
                } else if (choice == 2) {
                    if (handleLogin(client)) {
                        handleOperations(client);
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleRegister(Client client) throws Exception {
        System.out.println("Enter username:");
        String username = scanner.nextLine();
        System.out.println("Enter password:");
        String password = scanner.nextLine();

        boolean success = client.register(username, password);
        System.out.println(success ? "Registration successful" : "Registration failed");
    }

    private static boolean handleLogin(Client client) throws Exception {
        System.out.println("Enter username:");
        String username = scanner.nextLine();
        System.out.println("Enter password:");
        String password = scanner.nextLine();

        boolean success = client.authenticate(username, password);
        System.out.println(success ? "Login successful" : "Login failed");
        return success;
    }

    private static void handleOperations(Client client) throws Exception {
        while (true) {
            System.out.println("\n1. Put");
            System.out.println("2. Get");
            System.out.println("3. MultiPut");
            System.out.println("4. MultiGet");
            System.out.println("5. Logout");

            int choice = Integer.parseInt(scanner.nextLine());

            if (choice == 1) {
                System.out.println("Enter key:");
                String key = scanner.nextLine();
                System.out.println("Enter value:");
                String value = scanner.nextLine();
                client.put(key, value.getBytes());
                System.out.println("Put successful");
            } else if (choice == 2) {
                System.out.println("Enter key:");
                String key = scanner.nextLine();
                byte[] value = client.get(key);
                System.out.println("Value: " + (value != null ? new String(value) : "null"));
            } else if (choice == 3) {
                Map<String, byte[]> pairs = new HashMap<>();
                System.out.println("Enter number of pairs:");
                int n = Integer.parseInt(scanner.nextLine());
                for (int i = 0; i < n; i++) {
                    System.out.println("Enter key " + (i + 1) + ":");
                    String key = scanner.nextLine();
                    System.out.println("Enter value " + (i + 1) + ":");
                    String value = scanner.nextLine();
                    pairs.put(key, value.getBytes());
                }
                client.multiPut(pairs);
                System.out.println("MultiPut successful");
            } else if (choice == 4) {
                Set<String> keys = new HashSet<>();
                System.out.println("Enter number of keys:");
                int n = Integer.parseInt(scanner.nextLine());
                for (int i = 0; i < n; i++) {
                    System.out.println("Enter key " + (i + 1) + ":");
                    keys.add(scanner.nextLine());
                }
                Map<String, byte[]> values = client.multiGet(keys);
                System.out.println("Values:");
                for (Map.Entry<String, byte[]> entry : values.entrySet()) {
                    System.out.println(entry.getKey() + ": " + new String(entry.getValue()));
                }
            } else if (choice == 5) {
                break;
            }
        }
    }
}
