package com.fabrica.repositories;

import com.fabrica.exceptions.PersistenciaException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

public class AlertLogRepository {
    private final Path alertLogPath;

    public AlertLogRepository(Path alertLogPath) throws PersistenciaException {
        this.alertLogPath = alertLogPath;
        initializeStorage();
    }

    public synchronized void appendAlert(String alertMessage) throws PersistenciaException {
        try {
            Files.write(
                    alertLogPath,
                    Collections.singletonList(alertMessage),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel registrar o alerta.", exception);
        }
    }

    public synchronized List<String> readAlerts() throws PersistenciaException {
        try {
            return Files.readAllLines(alertLogPath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel ler os alertas do sistema.", exception);
        }
    }

    private void initializeStorage() throws PersistenciaException {
        try {
            Path parent = alertLogPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(alertLogPath)) {
                Files.write(alertLogPath, new byte[0], StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel preparar o arquivo de alertas.", exception);
        }
    }
}
