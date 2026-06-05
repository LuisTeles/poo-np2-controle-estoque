package com.fabrica.tcp;

import com.fabrica.config.AppConfig;

import java.io.IOException;

public class MonitorMain {
    public static void main(String[] args) {
        // Ponto de entrada separado: este terminal executa apenas o servidor de monitoramento.
        MonitorServer monitorServer = new MonitorServer(AppConfig.MONITOR_PORT);
        try {
            monitorServer.start();
        } catch (IOException exception) {
            System.err.println("Nao foi possivel iniciar o painel de monitoramento: " + exception.getMessage());
        }
    }
}
