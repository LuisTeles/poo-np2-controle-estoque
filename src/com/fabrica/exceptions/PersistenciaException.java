package com.fabrica.exceptions;

// Exceção personalizada para erros ao salvar ou carregar dados do sistema
public class PersistenciaException extends Exception {

    // Recebe uma mensagem e a causa original do erro
    public PersistenciaException(String message, Throwable cause) {
        super(message, cause);
    }
}