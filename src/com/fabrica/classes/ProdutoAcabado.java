package com.fabrica.classes;

import com.fabrica.abstractclasses.Item;

public class ProdutoAcabado extends Item {
    private final String dataFabricacao;

    public ProdutoAcabado(String codigo, String nome, int quantidade, int limiteCritico, String dataFabricacao) {
        super(codigo, nome, quantidade, limiteCritico);
        if (dataFabricacao == null || dataFabricacao.trim().isEmpty()) {
            throw new IllegalArgumentException("data de fabricacao nao pode ser vazia.");
        }
        this.dataFabricacao = dataFabricacao.trim();
    }

    public String getDataFabricacao() {
        return dataFabricacao;
    }

    @Override
    public String getTipo() {
        return "PRODUTO_ACABADO";
    }

    @Override
    protected String getDetalheEspecifico() {
        return dataFabricacao;
    }
}
