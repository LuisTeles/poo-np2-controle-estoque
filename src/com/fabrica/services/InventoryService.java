package com.fabrica.services;

import com.fabrica.abstractclasses.Item;
import com.fabrica.classes.ItemSnapshot;
import com.fabrica.classes.MateriaPrima;
import com.fabrica.classes.ProdutoAcabado;
import com.fabrica.exceptions.EstoqueInsuficienteException;
import com.fabrica.exceptions.ItemNaoEncontradoException;
import com.fabrica.exceptions.PersistenciaException;
import com.fabrica.repositories.InventoryFileRepository;
import com.fabrica.repositories.MovementLogRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Classe de serviço: centraliza toda a lógica de negócio do inventário
public class InventoryService {

    private final Map<String, Item> inventoryByCode;              // Mapa que armazena os itens usando o código como chave
    private final InventoryFileRepository inventoryFileRepository; // Repositório responsável por salvar/carregar o inventário em arquivo
    private final MovementLogRepository movementLogRepository;     // Repositório responsável por registrar o log de movimentações

    // Construtor: inicializa os repositórios e carrega o inventário já salvo em disco
    public InventoryService(InventoryFileRepository inventoryFileRepository,
                            MovementLogRepository movementLogRepository) throws PersistenciaException {
        this.inventoryByCode = new LinkedHashMap<String, Item>(); // LinkedHashMap mantém a ordem de inserção dos itens
        this.inventoryFileRepository = inventoryFileRepository;
        this.movementLogRepository = movementLogRepository;
        loadSnapshot();                                           // Carrega os dados persistidos ao iniciar o serviço
    }

    // Método sincronizado: cadastra uma nova MateriaPrima no inventário e persiste o estado
    public synchronized void cadastrarMateriaPrima(String codigo,
                                                   String nome,
                                                   int quantidade,
                                                   int limiteCritico,
                                                   String fornecedor) throws PersistenciaException {
        ensureCodeIsAvailable(codigo);                            // Garante que o código ainda não está em uso

        MateriaPrima materiaPrima = new MateriaPrima(codigo, nome, quantidade, limiteCritico, fornecedor); // Cria o objeto MateriaPrima
        materiaPrima.registrarMovimentacao("Cadastro inicial com quantidade " + quantidade + "."); // Registra a movimentação inicial

        inventoryByCode.put(materiaPrima.getCodigo(), materiaPrima);                              // Adiciona o item ao mapa de inventário
        movementLogRepository.appendEntry(materiaPrima.getCodigo(), materiaPrima.getHistoricoMovimentacoes().get(0)); // Salva a movimentação no log
        persistirEstado();                                        // Salva o inventário atualizado em disco
    }

    // Método sincronizado: consome matéria-prima e produz um produto acabado
    public synchronized void iniciarProducao(String codigoMateriaPrima,
                                             int quantidadeConsumida,
                                             String codigoProduto,
                                             String nomeProduto,
                                             int quantidadeProduzida,
                                             int limiteCriticoProduto)
            throws PersistenciaException, EstoqueInsuficienteException, ItemNaoEncontradoException {

        Item itemBase = getItemOrThrow(codigoMateriaPrima);       // Busca o item pelo código; lança exceção se não encontrar
        if (!(itemBase instanceof MateriaPrima)) {                // Verifica se o item é realmente uma MateriaPrima (polimorfismo)
            throw new IllegalArgumentException("o codigo informado nao pertence a uma materia-prima.");
        }

        // Remove a quantidade consumida da matéria-prima e registra no log
        itemBase.removerQuantidade(
                quantidadeConsumida,
                "Consumo na producao de " + quantidadeProduzida + " unidade(s) do produto " + codigoProduto + "."
        );
        movementLogRepository.appendEntry(
                itemBase.getCodigo(),
                itemBase.getHistoricoMovimentacoes().get(itemBase.getHistoricoMovimentacoes().size() - 1) // Pega a última movimentação registrada
        );

        Item itemProduto = inventoryByCode.get(codigoProduto);    // Busca o produto acabado no inventário pelo código
        if (itemProduto == null) {                                // Se o produto não existe, cria um novo
            ProdutoAcabado novoProduto = new ProdutoAcabado(
                    codigoProduto,
                    nomeProduto,
                    0,                                            // Começa com quantidade zero; será adicionada logo abaixo
                    limiteCriticoProduto,
                    LocalDate.now().toString()                    // Define a data de fabricação como hoje
            );
            inventoryByCode.put(novoProduto.getCodigo(), novoProduto); // Adiciona o novo produto ao inventário
            itemProduto = novoProduto;
        } else if (!(itemProduto instanceof ProdutoAcabado)) {    // Se o código já existe mas não é ProdutoAcabado, lança erro
            throw new IllegalArgumentException("o codigo do produto ja esta em uso por outro tipo de item.");
        }

        // Adiciona a quantidade produzida ao produto e registra no log
        itemProduto.adicionarQuantidade(
                quantidadeProduzida,
                "Entrada por producao usando materia-prima " + codigoMateriaPrima + "."
        );
        movementLogRepository.appendEntry(
                itemProduto.getCodigo(),
                itemProduto.getHistoricoMovimentacoes().get(itemProduto.getHistoricoMovimentacoes().size() - 1) // Pega a última movimentação registrada
        );

        persistirEstado();                                        // Salva o inventário atualizado em disco
    }

    // Método sincronizado: remove quantidade de um ProdutoAcabado simulando um despacho para expedição
    public synchronized void despacharProduto(String codigoProduto, int quantidadeDespachada)
            throws PersistenciaException, EstoqueInsuficienteException, ItemNaoEncontradoException {

        Item item = getItemOrThrow(codigoProduto);                // Busca o item pelo código; lança exceção se não encontrar
        if (!(item instanceof ProdutoAcabado)) {                  // Verifica se o item é realmente um ProdutoAcabado (polimorfismo)
            throw new IllegalArgumentException("o codigo informado nao pertence a um produto acabado.");
        }

        // Remove a quantidade despachada do estoque e registra no log
        item.removerQuantidade(
                quantidadeDespachada,
                "Despacho de " + quantidadeDespachada + " unidade(s) para expedicao."
        );
        movementLogRepository.appendEntry(
                item.getCodigo(),
                item.getHistoricoMovimentacoes().get(item.getHistoricoMovimentacoes().size() - 1) // Pega a última movimentação registrada
        );

        persistirEstado();                                        // Salva o inventário atualizado em disco
    }

    // Método sincronizado: retorna uma lista de snapshots de todos os itens, ordenada por código
    public synchronized List<ItemSnapshot> listarEstoque() {
        List<ItemSnapshot> snapshots = new ArrayList<ItemSnapshot>();

        for (Item item : inventoryByCode.values()) {              // Percorre todos os itens do inventário
            snapshots.add(item.toSnapshot());                     // Converte cada item em um snapshot imutável
        }

        snapshots.sort(Comparator.comparing(ItemSnapshot::getCodigo)); // Ordena a lista pelo código do item
        return snapshots;
    }

    // Método sincronizado: retorna o snapshot de um único item pelo código
    public synchronized ItemSnapshot consultarItem(String codigo) throws ItemNaoEncontradoException {
        return getItemOrThrow(codigo).toSnapshot();               // Busca o item e retorna sua foto atual
    }

    // Lê o histórico de movimentações de um item diretamente do arquivo de log
    public List<String> listarHistoricoItem(String codigo) throws PersistenciaException {
        return movementLogRepository.readHistoryForItem(codigo);
    }

    // Salva o estado atual de todo o inventário em disco via repositório
    public synchronized void persistirEstado() throws PersistenciaException {
        inventoryFileRepository.saveInventory(listarEstoque());
    }

    // Carrega os itens salvos em disco e os insere no mapa de inventário
    private void loadSnapshot() throws PersistenciaException {
        List<Item> persistedItems = inventoryFileRepository.loadInventory();
        for (Item item : persistedItems) {
            inventoryByCode.put(item.getCodigo(), item);          // Reconstrói o mapa com os itens persistidos
        }
    }

    // Busca um item pelo código; lança ItemNaoEncontradoException se não existir
    private Item getItemOrThrow(String codigo) throws ItemNaoEncontradoException {
        Item item = inventoryByCode.get(codigo);
        if (item == null) {
            throw new ItemNaoEncontradoException("item com codigo " + codigo + " nao encontrado.");
        }
        return item;
    }

    // Verifica se o código já está em uso; lança exceção se já existir no inventário
    private void ensureCodeIsAvailable(String codigo) {
        if (inventoryByCode.containsKey(codigo)) {
            throw new IllegalArgumentException("ja existe um item cadastrado com o codigo informado.");
        }
    }
}