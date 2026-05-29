package com.fabrica.classes;

public class ItemSnapshot {
    private final String tipo;
    private final String codigo;
    private final String nome;
    private final int quantidade;
    private final int limiteCritico;
    private final String detalheEspecifico;

    public ItemSnapshot(String tipo, String codigo, String nome, int quantidade, int limiteCritico, String detalheEspecifico) {
        this.tipo = tipo;
        this.codigo = codigo;
        this.nome = nome;
        this.quantidade = quantidade;
        this.limiteCritico = limiteCritico;
        this.detalheEspecifico = detalheEspecifico;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public int getLimiteCritico() {
        return limiteCritico;
    }

    public String getDetalheEspecifico() {
        return detalheEspecifico;
    }

    public boolean isCritical() {
        return limiteCritico > 0 && quantidade <= limiteCritico;
    }
}
