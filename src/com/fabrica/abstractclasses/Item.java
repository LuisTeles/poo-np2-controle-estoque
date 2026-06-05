package com.fabrica.abstractclasses;

import com.fabrica.classes.ItemSnapshot;
import com.fabrica.exceptions.EstoqueInsuficienteException;
import com.fabrica.interfaces.Rastreavel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Classe abstrata: serve de modelo para todos os tipos de Item, não pode ser instanciada diretamente
public abstract class Item implements Rastreavel {

    // Atributo estático e constante: pertence à classe, não ao objeto; define o formato da data
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String codigo;                           // Código único do item, não pode ser alterado após criação
    private final String nome;                             // Nome do item, não pode ser alterado após criação
    private final int limiteCritico;                       // Quantidade mínima antes de considerar estoque crítico, imutável
    private int quantidade;                                // Quantidade atual em estoque, pode ser alterada
    private final List<String> historicoMovimentacoes;     // Lista de registros de todas as movimentações do item

    // Construtor protegido: só subclasses podem chamar para inicializar os atributos herdados
    protected Item(String codigo, String nome, int quantidade, int limiteCritico) {

        if (codigo == null || codigo.trim().isEmpty()) {          // Valida se o código não é nulo nem vazio
            throw new IllegalArgumentException("codigo nao pode ser vazio.");
        }
        if (nome == null || nome.trim().isEmpty()) {              // Valida se o nome não é nulo nem vazio
            throw new IllegalArgumentException("nome nao pode ser vazio.");
        }
        if (quantidade < 0) {                                     // Valida se a quantidade não é negativa
            throw new IllegalArgumentException("quantidade nao pode ser negativa.");
        }
        if (limiteCritico < 0) {                                  // Valida se o limite crítico não é negativo
            throw new IllegalArgumentException("limite critico nao pode ser negativo.");
        }

        this.codigo = codigo.trim();                              // Atribui o código sem espaços extras
        this.nome = nome.trim();                                  // Atribui o nome sem espaços extras
        this.quantidade = quantidade;                             // Atribui a quantidade inicial
        this.limiteCritico = limiteCritico;                       // Atribui o limite crítico
        this.historicoMovimentacoes = new ArrayList<String>();    // Inicializa a lista de histórico vazia
    }

    public String getCodigo() {                  // Getter: permite acesso externo ao atributo privado codigo
        return codigo;
    }

    public String getNome() {                    // Getter: permite acesso externo ao atributo privado nome
        return nome;
    }

    public synchronized int getQuantidade() {    // Getter sincronizado: garante leitura segura em múltiplas threads
        return quantidade;
    }

    public int getLimiteCritico() {              // Getter: permite acesso externo ao atributo privado limiteCritico
        return limiteCritico;
    }

    // Método sincronizado: adiciona quantidade ao estoque e registra a movimentação
    public synchronized void adicionarQuantidade(int quantidadeAdicional, String mensagem) {
        if (quantidadeAdicional <= 0) {                           // Valida se o valor a adicionar é positivo
            throw new IllegalArgumentException("a quantidade adicionada deve ser maior que zero.");
        }
        quantidade += quantidadeAdicional;                        // Soma o valor recebido ao estoque atual
        registrarMovimentacao(mensagem);                          // Registra a operação no histórico
    }

    // Método sincronizado: remove quantidade do estoque; lança exceção personalizada se não houver saldo
    public synchronized void removerQuantidade(int quantidadeRemovida, String mensagem)
            throws EstoqueInsuficienteException {
        if (quantidadeRemovida <= 0) {                            // Valida se o valor a remover é positivo
            throw new IllegalArgumentException("a quantidade removida deve ser maior que zero.");
        }
        if (quantidade < quantidadeRemovida) {                    // Verifica se há estoque suficiente para remover
            throw new EstoqueInsuficienteException(
                    "Estoque insuficiente para " + codigo + ". Disponivel: " + quantidade
                            + ", solicitado: " + quantidadeRemovida + "."
            );
        }
        quantidade -= quantidadeRemovida;                         // Subtrai o valor removido do estoque atual
        registrarMovimentacao(mensagem);                          // Registra a operação no histórico
    }

    // Retorna true se a quantidade atual for menor ou igual ao limite crítico
    public synchronized boolean estaEmNivelCritico() {
        return limiteCritico > 0 && quantidade <= limiteCritico;
    }

    // Implementação da interface Rastreavel: adiciona data/hora atual + mensagem ao histórico
    @Override
    public synchronized void registrarMovimentacao(String mensagem) {
        historicoMovimentacoes.add(LocalDateTime.now().format(FORMATTER) + " - " + mensagem);
    }

    // Implementação da interface Rastreavel: retorna uma cópia do histórico para evitar alteração externa
    @Override
    public synchronized List<String> getHistoricoMovimentacoes() {
        return new ArrayList<String>(historicoMovimentacoes);
    }

    // Cria e retorna um ItemSnapshot com o estado atual do objeto (foto imutável do momento)
    public synchronized ItemSnapshot toSnapshot() {
        return new ItemSnapshot(
                getTipo(),              // Tipo definido pela subclasse
                codigo,
                nome,
                quantidade,
                limiteCritico,
                getDetalheEspecifico()  // Detalhe específico definido pela subclasse
        );
    }

    // Método abstrato: obriga cada subclasse a informar seu próprio tipo (ex: "PRODUTO_ACABADO")
    public abstract String getTipo();

    // Método abstrato: obriga cada subclasse a fornecer seu próprio detalhe específico
    protected abstract String getDetalheEspecifico();
}