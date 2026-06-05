package com.fabrica.repositories;

import com.fabrica.exceptions.PersistenciaException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

/**
 * Repositório responsável por registrar e consultar os alertas do sistema
 * (ex: itens abaixo do limite crítico de estoque).
 *
 * É o mais simples dos três repositórios — cada linha do arquivo é uma
 * mensagem de alerta pura, sem separação por colunas nem escape de caracteres.
 *
 * Os métodos são sincronizados (synchronized) pra garantir que o arquivo
 * não seja corrompido caso múltiplas threads tentem gravar ou ler ao mesmo tempo.
 */
public class AlertLogRepository {

    // Caminho do arquivo onde os alertas vão ser registrados
    private final Path alertLogPath;

    /**
     * Construtor — recebe o caminho do arquivo e já garante que ele existe.
     */
    public AlertLogRepository(Path alertLogPath) throws PersistenciaException {
        this.alertLogPath = alertLogPath;
        initializeStorage();
    }

    /**
     * Registra um novo alerta no final do arquivo de log.
     *
     * O synchronized garante que, se duas threads tentarem gravar um alerta
     * ao mesmo tempo, elas vão esperar a vez — evitando entradas misturadas
     * ou corrompidas no arquivo.
     *
     * @param alertMessage mensagem de alerta a ser registrada
     */
    public synchronized void appendAlert(String alertMessage) throws PersistenciaException {
        try {
            // APPEND: adiciona a mensagem no final sem mexer no que já existe
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

    /**
     * Lê e retorna todos os alertas registrados no arquivo.
     *
     * Também é synchronized pra evitar que uma thread leia o arquivo
     * enquanto outra ainda está no meio de uma escrita.
     *
     * @return lista com todas as mensagens de alerta, na ordem em que foram registradas
     */
    public synchronized List<String> readAlerts() throws PersistenciaException {
        try {
            // Lê todas as linhas de uma vez — cada linha é um alerta
            return Files.readAllLines(alertLogPath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel ler os alertas do sistema.", exception);
        }
    }

    /**
     * Garante que o arquivo de alertas existe antes de tentar usá-lo.
     * Cria os diretórios necessários e, se o arquivo não existir, cria um vazio.
     */
    private void initializeStorage() throws PersistenciaException {
        try {
            Path parent = alertLogPath.getParent();

            // Cria os diretórios pai caso não existam
            if (parent != null) {
                Files.createDirectories(parent);
            }

            // Cria o arquivo vazio se ele ainda não existir
            if (!Files.exists(alertLogPath)) {
                Files.write(alertLogPath, new byte[0], StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException exception) {
            throw new PersistenciaException("Nao foi possivel preparar o arquivo de alertas.", exception);
        }
    }
}