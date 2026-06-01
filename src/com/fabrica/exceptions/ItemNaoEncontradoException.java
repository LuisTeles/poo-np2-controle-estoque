package com.fabrica.exceptions;

// Exceção personalizada para indicar que um item não foi encontrado no sistema
public class ItemNaoEncontradoException extends Exception {

    // Recebe a mensagem de erro e repassa para a classe Exception
    public ItemNaoEncontradoException(String message) {
        super(message);
    }
}
