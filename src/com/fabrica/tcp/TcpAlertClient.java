package com.fabrica.tcp;

import com.fabrica.classes.StockAlert;
import com.fabrica.exceptions.TcpAlertException;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpAlertClient {
    private final String host;
    private final int port;

    public TcpAlertClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void sendAlert(StockAlert alert) throws TcpAlertException {
        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
            writer.println(alert.toPayload());
            writer.flush();
        } catch (IOException exception) {
            throw new TcpAlertException(
                    "Nao foi possivel enviar alerta TCP para " + host + ":" + port + ".",
                    exception
            );
        }
    }
}
