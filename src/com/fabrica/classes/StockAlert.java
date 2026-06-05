package com.fabrica.classes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa um alerta de estoque crítico para um item específico.
 *
 * A classe cuida de três responsabilidades:
 *   1. Guardar os dados do alerta (imutável, como o ItemSnapshot)
 *   2. Serializar/deserializar pro formato de arquivo (toPayload / fromPayload)
 *   3. Formatar a mensagem pra exibição humana (formatForDisplay)
 *
 * O formato de serialização usa pipe (|) como separador de colunas —
 * diferente dos repositórios que usam tabulação.
 */
public class StockAlert {

    // Formato fixo de data/hora usado em todos os alertas: "2024-03-15 14:32:00"
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String codigoItem;     // código do item que entrou em situação crítica
    private final String nomeItem;       // nome legível do item
    private final int quantidadeAtual;   // quantidade no momento em que o alerta foi gerado
    private final int limiteCritico;     // limite configurado que foi atingido ou ultrapassado
    private final String mensagem;       // texto descritivo do alerta
    private final String timestamp;      // data e hora em que o alerta foi criado

    /**
     * Construtor simplificado — gera o timestamp automaticamente com a hora atual.
     * É o construtor usado na prática quando um alerta novo é disparado.
     */
    public StockAlert(String codigoItem, String nomeItem, int quantidadeAtual, int limiteCritico, String mensagem) {
        // Delega pro construtor completo, injetando o timestamp gerado agora
        this(codigoItem, nomeItem, quantidadeAtual, limiteCritico, mensagem, LocalDateTime.now().format(FORMATTER));
    }

    /**
     * Construtor completo — recebe todos os campos incluindo o timestamp.
     * Usado principalmente ao reconstruir um alerta salvo em arquivo (fromPayload),
     * onde o timestamp original precisa ser preservado.
     */
    public StockAlert(String codigoItem,
                      String nomeItem,
                      int quantidadeAtual,
                      int limiteCritico,
                      String mensagem,
                      String timestamp) {
        this.codigoItem = codigoItem;
        this.nomeItem = nomeItem;
        this.quantidadeAtual = quantidadeAtual;
        this.limiteCritico = limiteCritico;
        this.mensagem = mensagem;
        this.timestamp = timestamp;
    }

    /**
     * Factory method — cria um StockAlert a partir de um ItemSnapshot.
     *
     * Monta a mensagem de alerta automaticamente com os dados do snapshot
     * e usa o construtor simplificado (timestamp = agora).
     *
     * Ex de mensagem gerada:
     *   "Estoque critico para Parafuso M3 (PAR-001) - atual: 5, limite: 10."
     */
    public static StockAlert fromSnapshot(ItemSnapshot itemSnapshot) {
        String mensagem = "Estoque critico para " + itemSnapshot.getNome()
                + " (" + itemSnapshot.getCodigo() + ") - atual: " + itemSnapshot.getQuantidade()
                + ", limite: " + itemSnapshot.getLimiteCritico() + ".";

        return new StockAlert(
                itemSnapshot.getCodigo(),
                itemSnapshot.getNome(),
                itemSnapshot.getQuantidade(),
                itemSnapshot.getLimiteCritico(),
                mensagem
        );
    }

    /**
     * Factory method — reconstrói um StockAlert a partir de uma linha serializada do arquivo.
     *
     * Formato esperado (6 campos separados por pipe):
     *   timestamp|codigoItem|nomeItem|quantidadeAtual|limiteCritico|mensagem
     *
     * Par de fromPayload/toPayload: o que um grava, o outro consegue ler de volta.
     */
    public static StockAlert fromPayload(String payload) {
        // "\\|" é o regex pro pipe literal — split normal quebraria com o pipe
        // O -1 garante que campos vazios no final sejam mantidos
        String[] parts = payload.split("\\|", -1);

        if (parts.length != 6) {
            throw new IllegalArgumentException("Payload de alerta invalido: " + payload);
        }

        // Ordem dos campos no payload: timestamp(0) | codigo(1) | nome(2) | quantidade(3) | limite(4) | mensagem(5)
        return new StockAlert(
                unescape(parts[1]),              // codigoItem
                unescape(parts[2]),              // nomeItem
                Integer.parseInt(parts[3]),      // quantidadeAtual
                Integer.parseInt(parts[4]),      // limiteCritico
                unescape(parts[5]),              // mensagem
                unescape(parts[0])               // timestamp
        );
    }

    /**
     * Serializa o alerta numa string de uma linha pronta pra ser gravada em arquivo.
     *
     * Formato gerado (6 campos separados por pipe):
     *   timestamp|codigoItem|nomeItem|quantidadeAtual|limiteCritico|mensagem
     *
     * Campos de texto passam pelo escape; números não precisam.
     */
    public String toPayload() {
        return escape(timestamp)
                + "|" + escape(codigoItem)
                + "|" + escape(nomeItem)
                + "|" + quantidadeAtual       // inteiro, sem escape
                + "|" + limiteCritico         // inteiro, sem escape
                + "|" + escape(mensagem);
    }

    /**
     * Formata o alerta pra exibição humana — não serve pra persistência.
     *
     * Exemplo de saída:
     *   "[2024-03-15 14:32:00] Estoque critico para Parafuso M3 (PAR-001) - atual: 5, limite: 10."
     */
    public String formatForDisplay() {
        return "[" + timestamp + "] " + mensagem;
    }

    /**
     * Escapa caracteres especiais antes de gravar no payload.
     *
     * Aqui o separador é o pipe (|), não a tabulação — então só dois casos:
     *   \  →  \\   (sempre o primeiro! evita duplo-escape)
     *   |  →  \p   (\p foi escolhido como sequência de escape pro pipe)
     */
    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")  // escapa a barra primeiro
                .replace("|", "\\p");   // depois escapa o pipe
    }

    /**
     * Faz o processo inverso — converte as sequências de escape de volta
     * pros caracteres reais ao ler o payload do arquivo.
     *
     * Sequências reconhecidas:
     *   \p  →  pipe real (|)
     *   \\  →  barra invertida real
     *
     * Diferente dos outros repositórios, aqui não tem \t ou \n
     * porque o formato de payload não usa tabulação nem newline dentro dos campos.
     */
    private static String unescape(String value) {
        StringBuilder builder = new StringBuilder();

        // Flag que indica se o caractere anterior foi uma barra invertida
        boolean escaped = false;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);

            if (escaped) {
                if (current == 'p') {
                    builder.append('|');       // \p → pipe real
                } else {
                    builder.append(current);   // qualquer outro (ex: \\) → só o caractere
                }
                escaped = false;

            } else if (current == '\\') {
                // Encontrou barra invertida — ativa a flag e espera o próximo
                escaped = true;

            } else {
                // Caractere normal, adiciona direto
                builder.append(current);
            }
        }

        // Barra solta no final vai como caractere literal
        if (escaped) {
            builder.append('\\');
        }

        return builder.toString();
    }
}