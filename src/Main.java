// Main.java

import Server.Server;

public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Main <port> <maxSessions>");
            System.out.println("Example: java Main 8080 10");
            return;
        }

        try {
            // Parse command line arguments
            int port = Integer.parseInt(args[0]);
            int maxSessions = Integer.parseInt(args[1]);

            // Validate arguments
            if (port < 1024 || port > 65535) {
                System.out.println("Error: Port must be between 1024 and 65535");
                return;
            }

            if (maxSessions < 1) {
                System.out.println("Error: maxSessions must be at least 1");
                return;
            }

            // Create and start the server
            System.out.println("Starting server on port " + port);
            System.out.println("Maximum concurrent sessions: " + maxSessions);

            Server server = new Server(maxSessions);

            // Add shutdown hook to handle graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down server...");
                // Aqui você pode adicionar lógica adicional de cleanup se necessário
            }));

            // Start the server (this will block until the server is stopped)
            server.start(port);

        } catch (NumberFormatException e) {
            System.out.println("Error: Port and maxSessions must be valid numbers");
        } catch (Exception e) {
            System.out.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to print usage instructions
    private static void printUsage() {
        System.out.println("\nKey-Value Store Server");
        System.out.println("=====================");
        System.out.println("\nCommand line arguments:");
        System.out.println("  <port>        - The port number to listen on (1024-65535)");
        System.out.println("  <maxSessions> - Maximum number of concurrent client sessions");
        System.out.println("\nExample:");
        System.out.println("  java Main 8080 10");
        System.out.println("\nControls:");
        System.out.println("  Ctrl+C to shutdown the server");
    }
}


