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

public class MovementLogRepository {
    private final Path logPath;

    public MovementLogRepository(Path logPath) throws PersistenciaException {
        this.logPath = logPath;
        initializeStorage();
    }

    public void appendEntry(String itemCode, String message) throws PersistenciaException {
        String line = escape(itemCode) + "\t" + escape(message);
        try {
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

    public List<String> readHistoryForItem(String itemCode) throws PersistenciaException {
        try {
            List<String> history = new ArrayList<String>();
            List<String> lines = Files.readAllLines(logPath, StandardCharsets.UTF_8);

            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] columns = line.split("\t", 2);
                if (columns.length != 2) {
                    continue;
                }

                if (unescape(columns[0]).equals(itemCode)) {
                    history.add(unescape(columns[1]));
                }
            }

            return history;
        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel ler o historico do item.", exception);
        }
    }

    private void initializeStorage() throws PersistenciaException {
        try {
            Path parent = logPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(logPath)) {
                Files.write(logPath, new byte[0], StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel preparar o arquivo de movimentacoes.", exception);
        }
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
