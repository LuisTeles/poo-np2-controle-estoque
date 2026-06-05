package com.fabrica.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centraliza todas as configurações globais da aplicação num lugar só.
 *
 * A classe é final e tem construtor privado — isso impede que alguém
 * a estenda ou instancie. Ela nunca vai virar objeto: é só um agrupador
 * de constantes estáticas acessíveis direto pelo nome da classe.
 *
 * Padrão comum em Java pra classes utilitárias (como a própria Math do JDK).
 */
public final class AppConfig {

    // -------------------------------------------------------------------------
    // Configurações do monitor de estoque
    // -------------------------------------------------------------------------

    /** Endereço do servidor onde o monitor de estoque vai escutar conexões. */
    public static final String MONITOR_HOST = "127.0.0.1";

    /** Porta usada pelo monitor de estoque. */
    public static final int MONITOR_PORT = 5050;

    /**
     * Intervalo entre cada verificação do monitor, em milissegundos.
     * O underscore no número (2_000L) é só pra facilitar a leitura — equivale a 2000.
     * O sufixo L indica que é um long (necessário pra métodos como Thread.sleep).
     */
    public static final long MONITOR_INTERVAL_MS = 2_000L;

    /**
     * Tempo máximo que o sistema espera o monitor encerrar antes de forçar a parada,
     * em milissegundos.
     */
    public static final long MONITOR_SHUTDOWN_TIMEOUT_MS = 2_000L;

    // -------------------------------------------------------------------------
    // Caminhos dos arquivos de dados
    // -------------------------------------------------------------------------

    /**
     * Diretório base onde todos os arquivos de dados ficam.
     * O caminho é relativo ao diretório de execução da aplicação.
     */
    public static final Path DATA_DIRECTORY = Paths.get("data");

    /**
     * Arquivo TSV com o snapshot atual do estoque.
     * Usando resolve() a partir de DATA_DIRECTORY — se o diretório base
     * mudar, todos os caminhos abaixo atualizam automaticamente.
     */
    public static final Path SNAPSHOT_PATH = DATA_DIRECTORY.resolve("estoque-snapshot.tsv");

    /** Arquivo de log com o histórico de todas as movimentações de estoque. */
    public static final Path MOVEMENT_LOG_PATH = DATA_DIRECTORY.resolve("movimentacoes.log");

    /** Arquivo de log com todos os alertas de estoque crítico registrados. */
    public static final Path ALERT_LOG_PATH = DATA_DIRECTORY.resolve("alertas.log");

    /**
     * Construtor privado — impede instanciação.
     * Não faz sentido criar um objeto AppConfig; as constantes são acessadas
     * diretamente como AppConfig.SNAPSHOT_PATH, AppConfig.MONITOR_PORT, etc.
     */
    private AppConfig() {
    }
}