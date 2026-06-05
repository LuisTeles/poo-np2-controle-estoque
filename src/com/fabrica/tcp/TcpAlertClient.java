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

    // Encapsulamento: o cliente TCP guarda host e porta como estado interno do objeto.
    public TcpAlertClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void sendAlert(StockAlert alert) throws TcpAlertException {
        // Cliente TCP: a aplicacao abre um Socket, envia a mensagem e fecha a conexao.
        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
            // O alerta e convertido para texto para trafegar pela rede de forma simples.
            writer.println(alert.toPayload());
            writer.flush();
        } catch (IOException exception) {
            // Excecao customizada: traduz erro tecnico de rede para uma regra do dominio.
            throw new TcpAlertException(
                    "Nao foi possivel enviar alerta TCP para " + host + ":" + port + ".",
                    exception
            );
        }
    }
}
