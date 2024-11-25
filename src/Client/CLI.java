package Client;

import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class CLI {
    private static final Scanner scanner = new Scanner(System.in);
    private static Client client;

    // ANSI escape codes for colors
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";

    public static void main(String[] args) {
        try {
            String host = "localhost";
            int port = 8080;

            client = new Client(host, port);

            // Add shutdown hook to handle logout on Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (client != null) {
                        client.close();
                        System.out.println(GREEN + "Logged out successfully" + RESET);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

            while (true) {
                System.out.println(BLUE + "\n--- Main Menu ---" + RESET);
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Select an option: ");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        handleRegister(client);
                        break;
                    case "2":
                        if (handleLogin(client)) {
                            handleOperations(client);
                        } else {
                            System.out.println(RED + "Login failed" + RESET);
                        }
                        break;
                    case "3":
                        System.out.println("Exiting...");
                        client.close();
                        return;
                    default:
                        System.out.println(RED + "Invalid option. Please try again." + RESET);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleRegister(Client client) {
        System.out.println(BLUE + "\n--- Register ---" + RESET);
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        try {
            client.register(username, password);
            System.out.println(GREEN + "Registration successful" + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Registration failed - " + e.getMessage() + RESET);
        }
    }

    private static boolean handleLogin(Client client) throws Exception {
        System.out.println(BLUE + "\n--- Login ---" + RESET);
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        boolean success = client.authenticate(username, password);
        if (success) {
            System.out.println(GREEN + "Login successful" + RESET);
        }
        return success;
    }

    private static void handleOperations(Client client) throws Exception {
        while (true) {
            System.out.println(BLUE + "\n--- Operations Menu ---" + RESET);
            System.out.println("1. Put");
            System.out.println("2. Get");
            System.out.println("3. MultiPut");
            System.out.println("4. MultiGet");
            System.out.println("5. GetWhen");
            System.out.println("6. Logout");
            System.out.print("Select an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    handlePut(client);
                    break;
                case "2":
                    handleGet(client);
                    break;
                case "3":
                    handleMultiPut(client);
                    break;
                case "4":
                    handleMultiGet(client);
                    break;
                case "5":
                    handleGetWhen(client);
                    break;
                case "6":
                    client.logout();
                    System.out.println(GREEN + "Logged out successfully" + RESET);
                    return;
                default:
                    System.out.println(RED + "Invalid option. Please try again." + RESET);
            }
        }
    }

    private static void handlePut(Client client) throws Exception {
        System.out.println(BLUE + "\n--- Put ---" + RESET);
        System.out.print("Enter key: ");
        String key = scanner.nextLine();
        System.out.print("Enter value: ");
        String value = scanner.nextLine();

        client.put(key, value.getBytes());
        System.out.println(GREEN + "Put operation successful" + RESET);
    }

    private static void handleGet(Client client) throws Exception {
        System.out.println(BLUE + "\n--- Get ---" + RESET);
        System.out.print("Enter key: ");
        String key = scanner.nextLine();

        byte[] value = client.get(key);
        System.out.println(GREEN + "Get operation successful. Value: " + new String(value) + RESET);
    }

    private static void handleMultiPut(Client client) throws Exception {
        System.out.println(BLUE + "\n--- MultiPut ---" + RESET);
        Map<String, byte[]> pairs = new HashMap<>();
        while (true) {
            System.out.print("Enter key (or 'done' to finish): ");
            String key = scanner.nextLine();
            if (key.equalsIgnoreCase("done")) {
                break;
            }
            System.out.print("Enter value: ");
            String value = scanner.nextLine();
            pairs.put(key, value.getBytes());
        }

        client.multiPut(pairs);
        System.out.println(GREEN + "MultiPut operation successful" + RESET);
    }

    private static void handleMultiGet(Client client) throws Exception {
        System.out.println(BLUE + "\n--- MultiGet ---" + RESET);
        Set<String> keys = new HashSet<>();
        while (true) {
            System.out.print("Enter key (or 'done' to finish): ");
            String key = scanner.nextLine();
            if (key.equalsIgnoreCase("done")) {
                break;
            }
            keys.add(key);
        }

        Map<String, byte[]> results = client.multiGet(keys);
        System.out.println(GREEN + "MultiGet operation successful. Results:" + RESET);
        for (Map.Entry<String, byte[]> entry : results.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + new String(entry.getValue()));
        }
    }

    private static void handleGetWhen(Client client) throws Exception {
        System.out.println(BLUE + "\n--- GetWhen ---" + RESET);
        System.out.print("Enter key: ");
        String key = scanner.nextLine();
        System.out.print("Enter condition key: ");
        String keyCond = scanner.nextLine();
        System.out.print("Enter condition value: ");
        String valueCond = scanner.nextLine();

        client.getWhen(key, keyCond, valueCond.getBytes(), new Client.AsyncCallback() {
            @Override
            public void onSuccess(byte[] result) {
                System.out.println(GREEN + "GetWhen operation successful. Value: " + new String(result) + RESET);
            }

            @Override
            public void onFailure() {
                System.out.println(RED + "GetWhen operation failed." + RESET);
            }

            @Override
            public void onError(Exception e) {
                System.out.println(RED + "GetWhen operation error: " + e.getMessage() + RESET);
            }
        });
    }
}