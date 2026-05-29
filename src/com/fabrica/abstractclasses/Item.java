package com.fabrica.abstractclasses;

import com.fabrica.classes.ItemSnapshot;
import com.fabrica.exceptions.EstoqueInsuficienteException;
import com.fabrica.interfaces.Rastreavel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public abstract class Item implements Rastreavel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String codigo;
    private final String nome;
    private final int limiteCritico;
    private int quantidade;
    private final List<String> historicoMovimentacoes;

    protected Item(String codigo, String nome, int quantidade, int limiteCritico) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("codigo nao pode ser vazio.");
        }
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("nome nao pode ser vazio.");
        }
        if (quantidade < 0) {
            throw new IllegalArgumentException("quantidade nao pode ser negativa.");
        }
        if (limiteCritico < 0) {
            throw new IllegalArgumentException("limite critico nao pode ser negativo.");
        }

        this.codigo = codigo.trim();
        this.nome = nome.trim();
        this.quantidade = quantidade;
        this.limiteCritico = limiteCritico;
        this.historicoMovimentacoes = new ArrayList<String>();
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public synchronized int getQuantidade() {
        return quantidade;
    }

    public int getLimiteCritico() {
        return limiteCritico;
    }

    public synchronized void adicionarQuantidade(int quantidadeAdicional, String mensagem) {
        if (quantidadeAdicional <= 0) {
            throw new IllegalArgumentException("a quantidade adicionada deve ser maior que zero.");
        }

        quantidade += quantidadeAdicional;
        registrarMovimentacao(mensagem);
    }

    public synchronized void removerQuantidade(int quantidadeRemovida, String mensagem)
            throws EstoqueInsuficienteException {
        if (quantidadeRemovida <= 0) {
            throw new IllegalArgumentException("a quantidade removida deve ser maior que zero.");
        }

        if (quantidade < quantidadeRemovida) {
            throw new EstoqueInsuficienteException(
                    "Estoque insuficiente para " + codigo + ". Disponivel: " + quantidade
                            + ", solicitado: " + quantidadeRemovida + "."
            );
        }

        quantidade -= quantidadeRemovida;
        registrarMovimentacao(mensagem);
    }

    public synchronized boolean estaEmNivelCritico() {
        return limiteCritico > 0 && quantidade <= limiteCritico;
    }

    @Override
    public synchronized void registrarMovimentacao(String mensagem) {
        historicoMovimentacoes.add(LocalDateTime.now().format(FORMATTER) + " - " + mensagem);
    }

    @Override
    public synchronized List<String> getHistoricoMovimentacoes() {
        return new ArrayList<String>(historicoMovimentacoes);
    }

    public synchronized ItemSnapshot toSnapshot() {
        return new ItemSnapshot(
                getTipo(),
                codigo,
                nome,
                quantidade,
                limiteCritico,
                getDetalheEspecifico()
        );
    }

    public abstract String getTipo();

    protected abstract String getDetalheEspecifico();
}
