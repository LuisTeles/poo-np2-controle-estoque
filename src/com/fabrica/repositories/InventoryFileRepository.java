package com.fabrica.repositories;

import com.fabrica.abstractclasses.Item;
import com.fabrica.classes.ItemSnapshot;
import com.fabrica.classes.MateriaPrima;
import com.fabrica.classes.ProdutoAcabado;
import com.fabrica.exceptions.PersistenciaException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Repositório responsável por salvar e carregar o inventário num arquivo de texto
 * usando o formato TSV (valores separados por tabulação — aquele '\t' que aparece
 * bastante pelo código).
 *
 * Cada linha do arquivo representa um item do estoque, e a primeira linha é
 * sempre o cabeçalho com o nome das colunas.
 */
public class InventoryFileRepository {

    // Cabeçalho fixo que vai na primeira linha do arquivo — serve como "título das colunas"
    private static final String HEADER = "tipo\tcodigo\tnome\tquantidade\tlimiteCritico\tdetalhe";

    // Caminho do arquivo onde o snapshot do estoque vai ser lido/gravado
    private final Path snapshotPath;

    /**
     * Construtor — recebe o caminho do arquivo e já inicializa o armazenamento.
     * Se o arquivo não existir ainda, ele é criado aqui mesmo.
     */
    public InventoryFileRepository(Path snapshotPath) throws PersistenciaException {
        this.snapshotPath = snapshotPath;
        initializeStorage();
    }

    /**
     * Lê o arquivo de snapshot e retorna uma lista com todos os itens do estoque.
     * A primeira linha (cabeçalho) é ignorada, assim como linhas em branco.
     */
    public List<Item> loadInventory() throws PersistenciaException {
        try {
            List<Item> items = new ArrayList<Item>();

            // Lê todas as linhas do arquivo de uma vez
            List<String> lines = Files.readAllLines(snapshotPath, StandardCharsets.UTF_8);

            // Começa do índice 1 pra pular o cabeçalho que está na linha 0
            for (int index = 1; index < lines.size(); index++) {
                String line = lines.get(index);

                // Pula linhas vazias que possam estar no arquivo
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Converte a linha de texto em um objeto Item e adiciona na lista
                items.add(parseLine(line));
            }

            return items;

        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel carregar o snapshot do estoque.", exception);
        }
    }

    /**
     * Recebe uma coleção de snapshots e sobrescreve o arquivo com os dados atuais.
     * Sempre começa com o cabeçalho, depois uma linha por item.
     */
    public void saveInventory(Collection<ItemSnapshot> snapshots) throws PersistenciaException {
        List<String> lines = new ArrayList<String>();

        // Primeira linha sempre é o cabeçalho
        lines.add(HEADER);

        // Converte cada snapshot em uma linha formatada e adiciona na lista
        for (ItemSnapshot snapshot : snapshots) {
            lines.add(formatLine(snapshot));
        }

        try {
            // Escreve tudo no arquivo, apagando o conteúdo anterior (TRUNCATE_EXISTING)
            Files.write(
                    snapshotPath,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel salvar o snapshot do estoque.", exception);
        }
    }

    /**
     * Garante que o arquivo de snapshot existe antes de tentar usá-lo.
     * Cria os diretórios necessários no caminho e, se o arquivo não existir,
     * cria um novo já com o cabeçalho pra não ficar vazio.
     */
    private void initializeStorage() throws PersistenciaException {
        try {
            Path parent = snapshotPath.getParent();

            // Cria os diretórios pai caso não existam (ex: "data/snapshots/")
            if (parent != null) {
                Files.createDirectories(parent);
            }

            // Se o arquivo ainda não existir, cria com o cabeçalho pra já ter estrutura
            if (!Files.exists(snapshotPath)) {
                Files.write(
                        snapshotPath,
                        java.util.Collections.singletonList(HEADER),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW
                );
            }
        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel preparar o arquivo de snapshot.", exception);
        }
    }

    /**
     * Converte uma linha do arquivo em um objeto Item (MateriaPrima ou ProdutoAcabado).
     *
     * Formato esperado (6 colunas separadas por tabulação):
     *   tipo | codigo | nome | quantidade | limiteCritico | detalhe
     */
    private Item parseLine(String line) {

        // Divide a linha pelas tabulações — o -1 garante que colunas vazias no final sejam mantidas
        String[] columns = line.split("\t", -1);

        // Valida se a linha tem exatamente as 6 colunas esperadas
        if (columns.length != 6) {
            throw new IllegalArgumentException("Linha de snapshot invalida: " + line);
        }

        // Extrai e desescapa cada campo da linha
        String tipo          = unescape(columns[0]);
        String codigo        = unescape(columns[1]);
        String nome          = unescape(columns[2]);
        int quantidade       = Integer.parseInt(columns[3]);
        int limiteCritico    = Integer.parseInt(columns[4]);
        String detalhe       = unescape(columns[5]);

        // Instancia o tipo correto de item com base no campo "tipo"
        if ("MATERIA_PRIMA".equals(tipo)) {
            return new MateriaPrima(codigo, nome, quantidade, limiteCritico, detalhe);
        }
        if ("PRODUTO_ACABADO".equals(tipo)) {
            return new ProdutoAcabado(codigo, nome, quantidade, limiteCritico, detalhe);
        }

        // Se o tipo não for reconhecido, lança exceção — melhor travar aqui do que silenciar o erro
        throw new IllegalArgumentException("Tipo de item desconhecido no snapshot: " + tipo);
    }

    /**
     * Formata um ItemSnapshot como uma linha TSV pronta pra ser gravada no arquivo.
     * Todos os campos de texto passam pelo escape pra não quebrar o formato.
     */
    private String formatLine(ItemSnapshot snapshot) {
        return escape(snapshot.getTipo())
                + "\t" + escape(snapshot.getCodigo())
                + "\t" + escape(snapshot.getNome())
                + "\t" + snapshot.getQuantidade()       // número inteiro, não precisa de escape
                + "\t" + snapshot.getLimiteCritico()    // número inteiro, não precisa de escape
                + "\t" + escape(snapshot.getDetalheEspecifico());
    }

    /**
     * Escapa caracteres especiais nos valores de texto antes de gravar no arquivo.
     *
     * Isso é necessário porque o arquivo usa tabulação como separador de colunas —
     * se um valor de texto tiver uma tabulação dentro, quebraria a leitura.
     * O mesmo vale pra barra invertida e quebra de linha.
     *
     * Conversões feitas:
     *   \  →  \\   (escapa a própria barra pra não confundir na hora de ler)
     *   TAB →  \t  (tab literal vira a sequência de texto "\t")
     *   \n →  \n   (newline literal vira a sequência de texto "\n")
     */
    private String escape(String value) {
        return value
                .replace("\\", "\\\\")  // sempre o primeiro! evita duplo-escape
                .replace("\t", "\\t")
                .replace("\n", "\\n");
    }

    /**
     * Faz o processo inverso do escape — converte as sequências de texto de volta
     * pros caracteres especiais reais na hora de carregar o arquivo.
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
                // Caractere que vem depois de uma barra — decide o que inserir no resultado
                if (current == 't') {
                    builder.append('\t');       // \t → tab real
                } else if (current == 'n') {
                    builder.append('\n');       // \n → newline real
                } else {
                    builder.append(current);   // qualquer outro (ex: \\) → só o caractere mesmo
                }
                escaped = false;

            } else if (current == '\\') {
                // Encontrou barra invertida — ativa a flag e espera o próximo caractere
                escaped = true;

            } else {
                // Caractere normal, adiciona direto no resultado
                builder.append(current);
            }
        }

        // Caso especial: se o valor terminar com uma barra solta, ela vai como caractere literal
        if (escaped) {
            builder.append('\\');
        }

        return builder.toString();
    }
}