package com.fabrica.classes;

import com.fabrica.abstractclasses.Item;

public class MateriaPrima extends Item {
    private final String fornecedor;

    public MateriaPrima(String codigo, String nome, int quantidade, int limiteCritico, String fornecedor) {
        super(codigo, nome, quantidade, limiteCritico);
        if (fornecedor == null || fornecedor.trim().isEmpty()) {
            throw new IllegalArgumentException("fornecedor nao pode ser vazio.");
        }
        this.fornecedor = fornecedor.trim();
    }

    public String getFornecedor() {
        return fornecedor;
    }

    @Override
    public String getTipo() {
        return "MATERIA_PRIMA";
    }

    @Override
    protected String getDetalheEspecifico() {
        return fornecedor;
    }
}
