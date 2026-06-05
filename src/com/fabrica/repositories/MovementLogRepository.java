package com.fabrica.repositories;

import com.fabrica.exceptions.PersistenciaException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repositório responsável por registrar e consultar o histórico de
 * movimentações do estoque num arquivo de log.
 *
 * Diferente do repositório de inventário, aqui não tem cabeçalho —
 * cada linha do arquivo é uma entrada de log no formato:
 *   codigoDoItem TAB mensagem
 *
 * E as entradas nunca são apagadas: o arquivo só cresce (append-only).
 */
public class MovementLogRepository {

    // Caminho do arquivo de log onde as movimentações vão ser registradas
    private final Path logPath;

    /**
     * Construtor — recebe o caminho do arquivo e já garante que ele existe.
     */
    public MovementLogRepository(Path logPath) throws PersistenciaException {
        this.logPath = logPath;
        initializeStorage();
    }

    /**
     * Registra uma nova entrada no final do arquivo de log.
     *
     * Nunca apaga o que já está lá — usa o modo APPEND pra sempre adicionar
     * no final, preservando todo o histórico anterior.
     *
     * @param itemCode código do item que sofreu movimentação
     * @param message  descrição da movimentação (ex: "Entrada de 10 unidades")
     */
    public void appendEntry(String itemCode, String message) throws PersistenciaException {

        // Monta a linha escapando os dois campos pra não quebrar o formato TSV
        String line = escape(itemCode) + "\t" + escape(message);

        try {
            // APPEND: adiciona a linha no final sem mexer no que já existe
            Files.write(
                    logPath,
                    Collections.singletonList(line),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel registrar a movimentacao.", exception);
        }
    }

    /**
     * Lê o arquivo de log e retorna apenas as mensagens referentes ao item informado.
     *
     * Percorre todas as linhas do arquivo e filtra as que têm o código do item
     * na primeira coluna — as demais são simplesmente ignoradas.
     *
     * @param itemCode código do item cujo histórico se quer consultar
     * @return lista de mensagens de movimentação daquele item, na ordem que foram registradas
     */
    public List<String> readHistoryForItem(String itemCode) throws PersistenciaException {
        try {
            List<String> history = new ArrayList<String>();
            List<String> lines = Files.readAllLines(logPath, StandardCharsets.UTF_8);

            for (String line : lines) {

                // Ignora linhas em branco que possam estar no arquivo
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Divide a linha em no máximo 2 partes: [codigo, mensagem]
                // O limite 2 garante que tabulações dentro da mensagem não quebrem a leitura
                String[] columns = line.split("\t", 2);

                // Linha malformada (sem tabulação) — ignora e continua
                if (columns.length != 2) {
                    continue;
                }

                // Se o código do item bater, adiciona a mensagem no histórico
                if (unescape(columns[0]).equals(itemCode)) {
                    history.add(unescape(columns[1]));
                }
            }

            return history;

        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel ler o historico do item.", exception);
        }
    }

    /**
     * Garante que o arquivo de log existe antes de tentar usá-lo.
     * Cria os diretórios necessários e, se o arquivo não existir, cria um arquivo vazio.
     *
     * Diferente do repositório de inventário, aqui não precisa escrever nenhum
     * cabeçalho — o arquivo de log começa vazio mesmo.
     */
    private void initializeStorage() throws PersistenciaException {
        try {
            Path parent = logPath.getParent();

            // Cria os diretórios pai caso não existam
            if (parent != null) {
                Files.createDirectories(parent);
            }

            // Cria o arquivo vazio se ele ainda não existir
            if (!Files.exists(logPath)) {
                Files.write(logPath, new byte[0], StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel preparar o arquivo de movimentacoes.", exception);
        }
    }

    /**
     * Escapa caracteres especiais nos valores de texto antes de gravar no arquivo.
     *
     * Conversões feitas:
     *   \   →  \\   (sempre o primeiro! evita duplo-escape)
     *   TAB →  \t
     *   \n  →  \n
     */
    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\n", "\\n");
    }

    /**
     * Faz o processo inverso do escape — converte as sequências de texto de volta
     * pros caracteres reais na hora de ler o arquivo.
     *
     * Sequências reconhecidas:
     *   \t  →  tabulação real
     *   \n  →  quebra de linha real
     *   \\  →  barra invertida real
     */
    private String unescape(String value) {
        StringBuilder builder = new StringBuilder();

        // Flag que indica se o caractere anterior foi uma barra invertida
        boolean escaped = false;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);

            if (escaped) {
                // Caractere que vem depois de uma barra — decide o que inserir
                if (current == 't') {
                    builder.append('\t');       // \t → tab real
                } else if (current == 'n') {
                    builder.append('\n');       // \n → newline real
                } else {
                    builder.append(current);   // qualquer outro → só o caractere
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

        // Barra solta no final do valor vai como caractere literal
        if (escaped) {
            builder.append('\\');
        }

        return builder.toString();
    }
}