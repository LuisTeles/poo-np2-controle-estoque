package com.fabrica.classes;

import com.fabrica.abstractclasses.Item;

// Herda os atributos e comportamentos comuns da classe abstrata Item
public class MateriaPrima extends Item {

    //Atributo específico da matéria-prima
    // final indica que o fornecedor não poderá ser alterado depois da criação do objeto
    private final String fornecedor;

    // Construtor responsável por criar uma matéria-prima com seus dados obrigatórios
    public MateriaPrima(String codigo, String nome, int quantidade, int limiteCritico, String fornecedor) {

        // Chama o construtor da classe mãe Item para inicializar os dados comuns
        super(codigo, nome, quantidade, limiteCritico);

        // Valida se o fornecedor foi informado corretamente
        if (fornecedor == null || fornecedor.trim().isEmpty()) {
            throw new IllegalArgumentException("fornecedor nao pode ser vazio.");
        }

        // Armazena o fornecedor removendo espaços extras no início e no final
        this.fornecedor = fornecedor.trim();
    }

    // Retorna o fornecedor da matéria-prima
    public String getFornecedor() {
        return fornecedor;
    }


    // Sobrescreve o método abstrato de Item para informar o tipo do item
    @Override
    public String getTipo() {
        return "MATERIA_PRIMA";
    }

    // Sobrescreve o método abstrato de Item para informar o detalhe específico(fornecedor)
    @Override
    protected String getDetalheEspecifico() {
        return fornecedor;
    }
}