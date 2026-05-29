package com.fabrica.tcp;

import com.fabrica.classes.StockAlert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MonitorServer {
    private final int port;

    public MonitorServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Painel de monitoramento aguardando alertas na porta " + port + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread handlerThread = new Thread(
                        new ClientHandler(clientSocket),
                        "monitor-client-" + clientSocket.getPort()
                );
                handlerThread.start();
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        private ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (Socket socket = clientSocket;
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                 )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    StockAlert alert = StockAlert.fromPayload(line);
                    System.out.println(alert.formatForDisplay());
                }
            } catch (IOException | IllegalArgumentException exception) {
                System.err.println("Falha ao processar conexao do monitor: " + exception.getMessage());
            }
        }
    }
}
