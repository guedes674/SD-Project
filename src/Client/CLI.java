package Client;

import java.util.*;

public class CLI {
    private static final Scanner scanner = new Scanner(System.in);
    private static final String host = "localhost";
    private static final int port = 8080;

    public static void main(String[] args) {
        while (true) {
            try (Client client = new Client(host, port)) {
                while (true) {
                    if (client.username == null) {
                        System.out.println("\n1. Register");
                        System.out.println("2. Login");
                        System.out.println("3. Exit");

                        int choice = Integer.parseInt(scanner.nextLine());

                        if (choice == 1) {
                            handleRegister(client);
                        } else if (choice == 2) {
                            if (handleLogin(client)) {
                                System.out.println("Login successful");
                            } else {
                                System.out.println("Login failed");
                            }
                        } else if (choice == 3) {
                            System.out.println("Exiting...");
                            return;
                        } else {
                            System.out.println("Invalid choice. Please try again.");
                        }
                    } else {
                        handleOperations(client);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleRegister(Client client) throws Exception {
        System.out.println("Enter username:");
        String username = scanner.nextLine();
        System.out.println("Enter password:");
        String password = scanner.nextLine();

        try {
            client.register(username, password);
            System.out.println("Registration successful");
        } catch (Exception e) {
            System.out.println("Registration failed - " + e.getMessage());
        }
    }

    private static boolean handleLogin(Client client) throws Exception {
        System.out.println("Enter username:");
        String username = scanner.nextLine();
        System.out.println("Enter password:");
        String password = scanner.nextLine();

        boolean success = client.authenticate(username, password);
        return success;
    }

    private static void handleOperations(Client client) throws Exception {
        while (true) {
            System.out.println("\n1. Put");
            System.out.println("2. Get");
            System.out.println("3. MultiPut");
            System.out.println("4. MultiGet");
            System.out.println("5. GetWhen");
            System.out.println("6. Logout");

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
                if (value != null) {
                    System.out.println("Value: " + new String(value));
                } else {
                    System.out.println("Value: null");
                }
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
                    if (entry.getValue() != null) {
                        System.out.println(entry.getKey() + ": " + new String(entry.getValue()));
                    } else {
                        System.out.println(entry.getKey() + ": null");
                    }
                }
            } else if (choice == 5) {
                System.out.println("Enter key:");
                String key = scanner.nextLine();
                System.out.println("Enter condition key:");
                String keyCond = scanner.nextLine();
                System.out.println("Enter condition value:");
                String valueCond = scanner.nextLine();
                client.getWhen(key, keyCond, valueCond.getBytes(), new Client.AsyncCallback() {
                    @Override
                    public void onSuccess(byte[] result) {
                        if (result != null) {
                            System.out.println("GetWhen successful. Value: " + new String(result));
                        } else {
                            System.out.println("GetWhen successful. Value: null");
                        }
                    }

                    @Override
                    public void onFailure() {
                        System.out.println("GetWhen failed.");
                    }

                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                    }
                });
                System.out.println("GetWhen operation started in background.");
            } else if (choice == 6) {
                client.logout();
                System.out.println("Logout successful");
                break;
            } else {
                System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}