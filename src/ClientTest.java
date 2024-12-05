import Client.Client;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ClientTest {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;
        int numOperations = 10000; // Number of operations to test with
        int hotKeysPercentage = 10; // Percentage of hot keys
        int valueSize = 1000; // Fixed size of the value byte array

        try {
            Client client = new Client(host, port);

            // Register and authenticate the user
            String username = "testUser";
            String password = "testPassword";
            client.register(username, password);
            client.authenticate(username, password);

            // Generate keys with fixed length
            Set<String> allKeys = new HashSet<>();
            for (int i = 0; i < numOperations; i++) {
                allKeys.add(String.format("%07d", i));
            }

            // Determine hot keys
            int numHotKeys = numOperations * hotKeysPercentage / 100;
            Set<String> hotKeys = new HashSet<>();
            Random random = new Random();
            for (int i = 0; i < numHotKeys; i++) {
                hotKeys.add(String.format("%07d", random.nextInt(numOperations)));
            }

            // Measure the time taken for the entire test
            long startTime = System.currentTimeMillis();

            // Perform put operations with random byte array values
            for (String key : allKeys) {
                byte[] value = new byte[valueSize];
                random.nextBytes(value);
                client.put(key, value);
            }

            // Perform get operations with hot keys being accessed more frequently
            for (int i = 0; i < numOperations; i++) {
                String key;
                if (random.nextInt(100) < hotKeysPercentage) {
                    key = hotKeys.iterator().next();
                } else {
                    key = String.format("%07d", random.nextInt(numOperations));
                }
                byte[] value = client.get(key);
                // System.out.println("Retrieved value for " + key + ": " + new String(value));
            }

            // Measure the end time and calculate the duration
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println(
                    "Total execution time for " + numOperations + " operations: " + duration + " milliseconds");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}