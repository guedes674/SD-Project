package Server;

import java.net.*;
import java.io.*;

public class ConnectionManager extends Thread {
    private final Socket clientSocket;
    private final Server server;

    public ConnectionManager(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            Demultiplexer demultiplexer = new Demultiplexer(in, out, server);

            while (true) {
                try {
                    demultiplexer.handleRequest();
                } catch (EOFException e) {
                    System.out.println("Client disconnected");
                    break;
                } catch (SocketException e) {
                    System.out.println("Connection aborted: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}