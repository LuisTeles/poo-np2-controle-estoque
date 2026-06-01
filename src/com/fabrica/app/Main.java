package com.fabrica.app;

import com.fabrica.exceptions.ItemNaoEncontradoException;
import com.fabrica.exceptions.PersistenciaException;
import com.fabrica.exceptions.TcpAlertException;
import com.fabrica.exceptions.EstoqueInsuficienteException;
import com.fabrica.config.AppConfig;
import com.fabrica.classes.ItemSnapshot;
import com.fabrica.repositories.AlertLogRepository;
import com.fabrica.repositories.InventoryFileRepository;
import com.fabrica.repositories.MovementLogRepository;
import com.fabrica.services.InventoryService;
import com.fabrica.tcp.TcpAlertClient;
import com.fabrica.threads.StockMonitorThread;

import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner SCANNER = new Scanner(System.in);

    private final InventoryService inventoryService;
    private final AlertLogRepository alertLogRepository;
    private final StockMonitorThread stockMonitorThread;

    private Main(InventoryService inventoryService,
                 AlertLogRepository alertLogRepository,
                 StockMonitorThread stockMonitorThread) {
        this.inventoryService = inventoryService;
        this.alertLogRepository = alertLogRepository;
        this.stockMonitorThread = stockMonitorThread;
    }

    public static void main(String[] args) {
        try {
            InventoryFileRepository inventoryRepository = new InventoryFileRepository(AppConfig.SNAPSHOT_PATH);
            MovementLogRepository movementLogRepository = new MovementLogRepository(AppConfig.MOVEMENT_LOG_PATH);
            AlertLogRepository alertLogRepository = new AlertLogRepository(AppConfig.ALERT_LOG_PATH);

            InventoryService inventoryService = new InventoryService(inventoryRepository, movementLogRepository);
            TcpAlertClient tcpAlertClient = new TcpAlertClient(AppConfig.MONITOR_HOST, AppConfig.MONITOR_PORT);
            StockMonitorThread stockMonitorThread = new StockMonitorThread(
                    inventoryService,
                    tcpAlertClient,
                    alertLogRepository,
                    AppConfig.MONITOR_INTERVAL_MS
            );

            Main application = new Main(inventoryService, alertLogRepository, stockMonitorThread);
            stockMonitorThread.start();
            application.executarMenu();
        } catch (PersistenciaException exception) {
            System.err.println("Falha ao iniciar o sistema: " + exception.getMessage());
        }
    }

    private void executarMenu() {
        int opcao;

        do {
            exibirMenu();
            opcao = lerOpcao();
            processarOpcao(opcao);
        } while (opcao != 0);

        encerrarSistema();
    }

    private void exibirMenu() {
        System.out.println();
        System.out.println("=== TRACECORE - CONTROLE DE FABRICA ===");
        System.out.println("1. Cadastrar nova materia-prima");
        System.out.println("2. Iniciar producao");
        System.out.println("3. Despachar produto acabado");
        System.out.println("4. Consultar estoque atual");
        System.out.println("5. Rastrear item por codigo");
        System.out.println("6. Visualizar alertas do sistema");
        System.out.println("0. Sair");
        System.out.println("=======================================");
        System.out.print("Escolha uma opcao: ");
    }

    private int lerOpcao() {
        while (!SCANNER.hasNextInt()) {
            System.out.print("Opcao invalida. Digite um numero do menu: ");
            SCANNER.nextLine();
        }

        int opcao = SCANNER.nextInt();
        SCANNER.nextLine();
        return opcao;
    }

    private void processarOpcao(int opcao) {
        try {
            switch (opcao) {
                case 1:
                    cadastrarMateriaPrima();
                    break;
                case 2:
                    iniciarProducao();
                    break;
                case 3:
                    despacharProduto();
                    break;
                case 4:
                    consultarEstoque();
                    break;
                case 5:
                    rastrearItem();
                    break;
                case 6:
                    visualizarAlertas();
                    break;
                case 0:
                    System.out.println("Encerrando o sistema...");
                    break;
                default:
                    System.out.println("Opcao inexistente. Tente novamente.");
                    break;
            }
        } catch (IllegalArgumentException exception) {
            System.out.println("Entrada invalida: " + exception.getMessage());
        } catch (ItemNaoEncontradoException | EstoqueInsuficienteException | PersistenciaException exception) {
            System.out.println("Operacao nao concluida: " + exception.getMessage());
        }
    }

    private void cadastrarMateriaPrima() throws PersistenciaException {
        System.out.print("Codigo da materia-prima: ");
        String codigo = lerTextoObrigatorio();

        System.out.print("Nome da materia-prima: ");
        String nome = lerTextoObrigatorio();

        System.out.print("Quantidade inicial: ");
        int quantidade = lerInteiroPositivo();

        System.out.print("Limite critico: ");
        int limiteCritico = lerInteiroNaoNegativo();

        System.out.print("Fornecedor: ");
        String fornecedor = lerTextoObrigatorio();

        inventoryService.cadastrarMateriaPrima(codigo, nome, quantidade, limiteCritico, fornecedor);
        System.out.println("Materia-prima cadastrada com sucesso.");
    }

    private void iniciarProducao()
            throws PersistenciaException, EstoqueInsuficienteException, ItemNaoEncontradoException {
        System.out.print("Codigo da materia-prima consumida: ");
        String codigoMateriaPrima = lerTextoObrigatorio();

        System.out.print("Quantidade consumida da materia-prima: ");
        int quantidadeConsumida = lerInteiroPositivo();

        System.out.print("Codigo do produto acabado: ");
        String codigoProduto = lerTextoObrigatorio();

        System.out.print("Nome do produto acabado: ");
        String nomeProduto = lerTextoObrigatorio();

        System.out.print("Quantidade produzida: ");
        int quantidadeProduzida = lerInteiroPositivo();

        System.out.print("Limite critico do produto acabado: ");
        int limiteCriticoProduto = lerInteiroNaoNegativo();

        inventoryService.iniciarProducao(
                codigoMateriaPrima,
                quantidadeConsumida,
                codigoProduto,
                nomeProduto,
                quantidadeProduzida,
                limiteCriticoProduto
        );

        System.out.println("Producao registrada com sucesso.");
    }

    private void despacharProduto()
            throws PersistenciaException, EstoqueInsuficienteException, ItemNaoEncontradoException {
        System.out.print("Codigo do produto acabado: ");
        String codigoProduto = lerTextoObrigatorio();

        System.out.print("Quantidade a despachar: ");
        int quantidadeDespacho = lerInteiroPositivo();

        inventoryService.despacharProduto(codigoProduto, quantidadeDespacho);
        System.out.println("Despacho registrado com sucesso.");
    }

    private void consultarEstoque() {
        List<ItemSnapshot> estoque = inventoryService.listarEstoque();

        if (estoque.isEmpty()) {
            System.out.println("Nenhum item cadastrado no estoque.");
            return;
        }

        System.out.println();
        System.out.println("TIPO | CODIGO | NOME | QUANTIDADE | LIMITE | DETALHE");
        System.out.println("----------------------------------------------------");
        for (ItemSnapshot item : estoque) {
            System.out.printf(
                    "%s | %s | %s | %d | %d | %s%n",
                    item.getTipo(),
                    item.getCodigo(),
                    item.getNome(),
                    item.getQuantidade(),
                    item.getLimiteCritico(),
                    item.getDetalheEspecifico()
            );
        }
    }

    private void rastrearItem() throws ItemNaoEncontradoException, PersistenciaException {
        System.out.print("Codigo do item: ");
        String codigo = lerTextoObrigatorio();

        ItemSnapshot item = inventoryService.consultarItem(codigo);
        List<String> historico = inventoryService.listarHistoricoItem(codigo);

        System.out.println();
        System.out.println("Detalhes do item");
        System.out.println("----------------");
        System.out.println("Tipo: " + item.getTipo());
        System.out.println("Codigo: " + item.getCodigo());
        System.out.println("Nome: " + item.getNome());
        System.out.println("Quantidade: " + item.getQuantidade());
        System.out.println("Limite critico: " + item.getLimiteCritico());
        System.out.println("Detalhe: " + item.getDetalheEspecifico());
        System.out.println("Em nivel critico: " + (item.isCritical() ? "Sim" : "Nao"));

        System.out.println();
        System.out.println("Historico de movimentacoes");
        System.out.println("--------------------------");
        if (historico.isEmpty()) {
            System.out.println("Nenhuma movimentacao registrada.");
            return;
        }

        for (String entrada : historico) {
            System.out.println(entrada);
        }
    }

    private void visualizarAlertas() throws PersistenciaException {
        List<String> alertas = alertLogRepository.readAlerts();

        if (alertas.isEmpty()) {
            System.out.println("Nenhum alerta registrado ate o momento.");
            return;
        }

        System.out.println();
        System.out.println("Alertas do sistema");
        System.out.println("------------------");
        for (String alerta : alertas) {
            System.out.println(alerta);
        }
    }

    private void encerrarSistema() {
        stockMonitorThread.shutdown();
        try {
            stockMonitorThread.join(AppConfig.MONITOR_SHUTDOWN_TIMEOUT_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        try {
            inventoryService.persistirEstado();
        } catch (PersistenciaException exception) {
            System.err.println("Falha ao salvar o estado final: " + exception.getMessage());
        }

        SCANNER.close();
    }

    private String lerTextoObrigatorio() {
        String valor = SCANNER.nextLine().trim();
        if (valor.isEmpty()) {
            throw new IllegalArgumentException("o campo nao pode ficar vazio.");
        }
        return valor;
    }

    private int lerInteiroPositivo() {
        while (!SCANNER.hasNextInt()) {
            System.out.print("Digite um numero inteiro valido: ");
            SCANNER.nextLine();
        }

        int valor = SCANNER.nextInt();
        SCANNER.nextLine();
        if (valor <= 0) {
            throw new IllegalArgumentException("o valor deve ser maior que zero.");
        }
        return valor;
    }

    private int lerInteiroNaoNegativo() {
        while (!SCANNER.hasNextInt()) {
            System.out.print("Digite um numero inteiro valido: ");
            SCANNER.nextLine();
        }

        int valor = SCANNER.nextInt();
        SCANNER.nextLine();
        if (valor < 0) {
            throw new IllegalArgumentException("o valor nao pode ser negativo.");
        }
        return valor;
    }
}
