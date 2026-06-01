// Toda classe que implementar a interface Rastreável deve registrar e retornar movimentações
package com.fabrica.interfaces;

import java.util.List;

public interface Rastreavel {
    // Registra uma nova movimentação no histórico do item
    void registrarMovimentacao(String mensagem);

    // Retorna a lista com todas as movimentações registradas
    List<String> getHistoricoMovimentacoes();
}
