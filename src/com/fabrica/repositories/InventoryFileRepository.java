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

public class InventoryFileRepository {
    private static final String HEADER = "tipo\tcodigo\tnome\tquantidade\tlimiteCritico\tdetalhe";
    private final Path snapshotPath;

    public InventoryFileRepository(Path snapshotPath) throws PersistenciaException {
        this.snapshotPath = snapshotPath;
        initializeStorage();
    }

    public List<Item> loadInventory() throws PersistenciaException {
        try {
            List<Item> items = new ArrayList<Item>();
            List<String> lines = Files.readAllLines(snapshotPath, StandardCharsets.UTF_8);

            for (int index = 1; index < lines.size(); index++) {
                String line = lines.get(index);
                if (line.trim().isEmpty()) {
                    continue;
                }
                items.add(parseLine(line));
            }

            return items;
        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel carregar o snapshot do estoque.", exception);
        }
    }

    public void saveInventory(Collection<ItemSnapshot> snapshots) throws PersistenciaException {
        List<String> lines = new ArrayList<String>();
        lines.add(HEADER);

        for (ItemSnapshot snapshot : snapshots) {
            lines.add(formatLine(snapshot));
        }

        try {
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

    private void initializeStorage() throws PersistenciaException {
        try {
            Path parent = snapshotPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

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

    private Item parseLine(String line) {
        String[] columns = line.split("\t", -1);
        if (columns.length != 6) {
            throw new IllegalArgumentException("Linha de snapshot invalida: " + line);
        }

        String tipo = unescape(columns[0]);
        String codigo = unescape(columns[1]);
        String nome = unescape(columns[2]);
        int quantidade = Integer.parseInt(columns[3]);
        int limiteCritico = Integer.parseInt(columns[4]);
        String detalhe = unescape(columns[5]);

        if ("MATERIA_PRIMA".equals(tipo)) {
            return new MateriaPrima(codigo, nome, quantidade, limiteCritico, detalhe);
        }
        if ("PRODUTO_ACABADO".equals(tipo)) {
            return new ProdutoAcabado(codigo, nome, quantidade, limiteCritico, detalhe);
        }

        throw new IllegalArgumentException("Tipo de item desconhecido no snapshot: " + tipo);
    }

    private String formatLine(ItemSnapshot snapshot) {
        return escape(snapshot.getTipo())
                + "\t" + escape(snapshot.getCodigo())
                + "\t" + escape(snapshot.getNome())
                + "\t" + snapshot.getQuantidade()
                + "\t" + snapshot.getLimiteCritico()
                + "\t" + escape(snapshot.getDetalheEspecifico());
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n");
    }

    private String unescape(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaped) {
                if (current == 't') {
                    builder.append('\t');
                } else if (current == 'n') {
                    builder.append('\n');
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
