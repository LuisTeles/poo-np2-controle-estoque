package com.fabrica.classes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StockAlert {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String codigoItem;
    private final String nomeItem;
    private final int quantidadeAtual;
    private final int limiteCritico;
    private final String mensagem;
    private final String timestamp;

    public StockAlert(String codigoItem, String nomeItem, int quantidadeAtual, int limiteCritico, String mensagem) {
        this(codigoItem, nomeItem, quantidadeAtual, limiteCritico, mensagem, LocalDateTime.now().format(FORMATTER));
    }

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

    public static StockAlert fromPayload(String payload) {
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 6) {
            throw new IllegalArgumentException("Payload de alerta invalido: " + payload);
        }

        return new StockAlert(
                unescape(parts[1]),
                unescape(parts[2]),
                Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4]),
                unescape(parts[5]),
                unescape(parts[0])
        );
    }

    public String toPayload() {
        return escape(timestamp)
                + "|" + escape(codigoItem)
                + "|" + escape(nomeItem)
                + "|" + quantidadeAtual
                + "|" + limiteCritico
                + "|" + escape(mensagem);
    }

    public String formatForDisplay() {
        return "[" + timestamp + "] " + mensagem;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\p");
    }

    private static String unescape(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaped) {
                if (current == 'p') {
                    builder.append('|');
                } else {
                    builder.append(current);
                }
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else {
                builder.append(current);
            }
        }

        if (escaped) {
            builder.append('\\');
        }

        return builder.toString();
    }
}
