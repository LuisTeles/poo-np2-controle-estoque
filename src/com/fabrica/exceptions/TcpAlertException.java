package com.fabrica.exceptions;

// Exceção personalizada para erros relacionados ao envio de alertas via TCP
public class TcpAlertException extends Exception {

    // Recebe uma mensagem e a causa original do erro
    public TcpAlertException(String message, Throwable cause) {
        super(message, cause);
    }
}
