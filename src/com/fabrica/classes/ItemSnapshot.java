package com.fabrica.classes;

/**
 * Representa uma "foto" imutável do estado de um item do estoque
 * num determinado momento.
 *
 * A ideia é simples: em vez de passar o objeto Item diretamente pro
 * repositório, a gente cria um snapshot com os dados que importam pra
 * persistência. Assim o repositório não precisa conhecer a hierarquia
 * de classes (MateriaPrima, ProdutoAcabado...) — ele só lida com este objeto.
 *
 * Uma vez criado, o snapshot não pode ser alterado (todos os campos são final).
 */
public class ItemSnapshot {

    private final String tipo;              // tipo do item: "MATERIA_PRIMA" ou "PRODUTO_ACABADO"
    private final String codigo;            // código único que identifica o item
    private final String nome;              // nome legível do item
    private final int quantidade;           // quantidade atual em estoque
    private final int limiteCritico;        // quantidade mínima antes de disparar alerta (0 = sem limite)
    private final String detalheEspecifico; // informação extra que varia por tipo (ex: unidade, categoria)

    /**
     * Construtor — recebe todos os dados do item e os armazena como imutáveis.
     * Não tem setters: o que entrou aqui não muda mais.
     */
    public ItemSnapshot(String tipo, String codigo, String nome, int quantidade, int limiteCritico, String detalheEspecifico) {
        this.tipo = tipo;
        this.codigo = codigo;
        this.nome = nome;
        this.quantidade = quantidade;
        this.limiteCritico = limiteCritico;
        this.detalheEspecifico = detalheEspecifico;
    }

    /** Retorna o tipo do item ("MATERIA_PRIMA" ou "PRODUTO_ACABADO"). */
    public String getTipo() {
        return tipo;
    }

    /** Retorna o código único do item. */
    public String getCodigo() {
        return codigo;
    }

    /** Retorna o nome do item. */
    public String getNome() {
        return nome;
    }

    /** Retorna a quantidade atual em estoque. */
    public int getQuantidade() {
        return quantidade;
    }

    /** Retorna o limite crítico de estoque (0 significa que não há limite configurado). */
    public int getLimiteCritico() {
        return limiteCritico;
    }

    /** Retorna o detalhe específico do item (varia conforme o tipo). */
    public String getDetalheEspecifico() {
        return detalheEspecifico;
    }

    /**
     * Verifica se o item está em situação crítica de estoque.
     *
     * Um item é considerado crítico quando:
     *   - tem um limite configurado (limiteCritico > 0), E
     *   - a quantidade atual está igual ou abaixo desse limite
     *
     * O guard "limiteCritico > 0" é importante: itens sem limite definido
     * (valor 0) nunca são marcados como críticos, independente da quantidade.
     *
     * @return true se o estoque está no limite crítico ou abaixo dele
     */
    public boolean isCritical() {
        return limiteCritico > 0 && quantidade <= limiteCritico;
    }
}