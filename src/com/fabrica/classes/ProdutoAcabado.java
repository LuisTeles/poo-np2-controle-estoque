package com.fabrica.classes;

import com.fabrica.abstractclasses.Item;

// Herda os atributos e comportamentos comuns da classe abstrata Item
public class ProdutoAcabado extends Item {

    // Atributo data de fabricação do produto acabado
    private final String dataFabricacao;

    // Construtor responsável por criar um produto acabado com seus dados obrigatórios
    public ProdutoAcabado(String codigo, String nome, int quantidade, int limiteCritico, String dataFabricacao) {

        // Chama o construtor da classe mãe Item para inicializar os dados comuns
        super(codigo, nome, quantidade, limiteCritico);

        // Valida se a data de fabricação foi informada corretamente
        if (dataFabricacao == null || dataFabricacao.trim().isEmpty()) {
            throw new IllegalArgumentException("data de fabricacao nao pode ser vazia.");
        }


        // Armazena a data de fabricação removendo espaços extras no início e no final
        this.dataFabricacao = dataFabricacao.trim();
    }

    // Retorna a data de fabricação do produto acabado
    public String getDataFabricacao() {
        return dataFabricacao;
    }

    // Sobrescreve o método abstrato de Item para informar o tipo do item
    @Override
    public String getTipo() {
        return "PRODUTO_ACABADO";
    }

    // Sobrescreve o método abstrato de Item para informar a data de fabricação
    @Override
    protected String getDetalheEspecifico() {
        return dataFabricacao;
    }
}
