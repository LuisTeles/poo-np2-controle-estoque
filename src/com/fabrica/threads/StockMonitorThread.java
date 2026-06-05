package com.fabrica.threads;

import com.fabrica.classes.ItemSnapshot;
import com.fabrica.classes.StockAlert;
import com.fabrica.exceptions.PersistenciaException;
import com.fabrica.exceptions.TcpAlertException;
import com.fabrica.repositories.AlertLogRepository;
import com.fabrica.services.InventoryService;
import com.fabrica.tcp.TcpAlertClient;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StockMonitorThread extends Thread {
    private final InventoryService inventoryService;
    private final TcpAlertClient tcpAlertClient;
    private final AlertLogRepository alertLogRepository;
    private final long intervalMillis;
    private final Set<String> criticalItemsAlreadyNotified;
    private volatile boolean running;

    public StockMonitorThread(InventoryService inventoryService,
                              TcpAlertClient tcpAlertClient,
                              AlertLogRepository alertLogRepository,
                              long intervalMillis) {
        super("stock-monitor-thread");
        // Injecao de dependencias: a thread recebe os objetos de que precisa em vez de cria-los internamente.
        this.inventoryService = inventoryService;
        this.tcpAlertClient = tcpAlertClient;
        this.alertLogRepository = alertLogRepository;
        this.intervalMillis = intervalMillis;
        this.criticalItemsAlreadyNotified = new HashSet<String>();
        this.running = true;
        // Thread daemon: nao impede o encerramento da aplicacao principal.
        setDaemon(true);
    }

    @Override
    public void run() {
        // Concorrencia: esta Thread roda em paralelo com o menu principal monitorando o estoque.
        while (running) {
            try {
                monitorCriticalStock();
                Thread.sleep(intervalMillis);
            } catch (InterruptedException exception) {
                if (!running) {
                    return;
                }
                Thread.currentThread().interrupt();
                return;
            } catch (PersistenciaException exception) {
                System.err.println("Falha ao registrar alerta de estoque: " + exception.getMessage());
            }
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }

    private void monitorCriticalStock() throws PersistenciaException {
        List<ItemSnapshot> snapshot = inventoryService.listarEstoque();
        Set<String> currentCriticalCodes = new HashSet<String>();

        for (ItemSnapshot item : snapshot) {
            if (!item.isCritical()) {
                continue;
            }

            currentCriticalCodes.add(item.getCodigo());
            if (criticalItemsAlreadyNotified.contains(item.getCodigo())) {
                continue;
            }

            StockAlert alert = StockAlert.fromSnapshot(item);
            try {
                // Composicao entre objetos: a Thread delega o envio TCP para TcpAlertClient.
                tcpAlertClient.sendAlert(alert);
                alertLogRepository.appendAlert(alert.formatForDisplay());
            } catch (TcpAlertException exception) {
                alertLogRepository.appendAlert(
                        alert.formatForDisplay() + " Falha no envio TCP: " + exception.getMessage()
                );
            }

            criticalItemsAlreadyNotified.add(item.getCodigo());
        }

        criticalItemsAlreadyNotified.retainAll(currentCriticalCodes);
    }
}
