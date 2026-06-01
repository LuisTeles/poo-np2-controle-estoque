// Exceção personalizada para indicar tentativa de retirada maior que o estoque disponível
package com.fabrica.exceptions;

public class EstoqueInsuficienteException extends Exception {

    // Recebe a mensagem de erro e repassa para a classe Exception
    public EstoqueInsuficienteException(String message) {
        super(message);
    }
}
