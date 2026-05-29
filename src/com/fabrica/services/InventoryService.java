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

public class InventoryService {
    private final Map<String, Item> inventoryByCode;
    private final InventoryFileRepository inventoryFileRepository;
    private final MovementLogRepository movementLogRepository;

    public InventoryService(InventoryFileRepository inventoryFileRepository,
                            MovementLogRepository movementLogRepository) throws PersistenciaException {
        this.inventoryByCode = new LinkedHashMap<String, Item>();
        this.inventoryFileRepository = inventoryFileRepository;
        this.movementLogRepository = movementLogRepository;
        loadSnapshot();
    }

    public synchronized void cadastrarMateriaPrima(String codigo,
                                                   String nome,
                                                   int quantidade,
                                                   int limiteCritico,
                                                   String fornecedor) throws PersistenciaException {
        ensureCodeIsAvailable(codigo);

        MateriaPrima materiaPrima = new MateriaPrima(codigo, nome, quantidade, limiteCritico, fornecedor);
        materiaPrima.registrarMovimentacao("Cadastro inicial com quantidade " + quantidade + ".");

        inventoryByCode.put(materiaPrima.getCodigo(), materiaPrima);
        movementLogRepository.appendEntry(materiaPrima.getCodigo(), materiaPrima.getHistoricoMovimentacoes().get(0));
        persistirEstado();
    }

    public synchronized void iniciarProducao(String codigoMateriaPrima,
                                             int quantidadeConsumida,
                                             String codigoProduto,
                                             String nomeProduto,
                                             int quantidadeProduzida,
                                             int limiteCriticoProduto)
            throws PersistenciaException, EstoqueInsuficienteException, ItemNaoEncontradoException {
        Item itemBase = getItemOrThrow(codigoMateriaPrima);
        if (!(itemBase instanceof MateriaPrima)) {
            throw new IllegalArgumentException("o codigo informado nao pertence a uma materia-prima.");
        }

        itemBase.removerQuantidade(
                quantidadeConsumida,
                "Consumo na producao de " + quantidadeProduzida + " unidade(s) do produto " + codigoProduto + "."
        );
        movementLogRepository.appendEntry(
                itemBase.getCodigo(),
                itemBase.getHistoricoMovimentacoes().get(itemBase.getHistoricoMovimentacoes().size() - 1)
        );

        Item itemProduto = inventoryByCode.get(codigoProduto);
        if (itemProduto == null) {
            ProdutoAcabado novoProduto = new ProdutoAcabado(
                    codigoProduto,
                    nomeProduto,
                    0,
                    limiteCriticoProduto,
                    LocalDate.now().toString()
            );
            inventoryByCode.put(novoProduto.getCodigo(), novoProduto);
            itemProduto = novoProduto;
        } else if (!(itemProduto instanceof ProdutoAcabado)) {
            throw new IllegalArgumentException("o codigo do produto ja esta em uso por outro tipo de item.");
        }

        itemProduto.adicionarQuantidade(
                quantidadeProduzida,
                "Entrada por producao usando materia-prima " + codigoMateriaPrima + "."
        );
        movementLogRepository.appendEntry(
                itemProduto.getCodigo(),
                itemProduto.getHistoricoMovimentacoes().get(itemProduto.getHistoricoMovimentacoes().size() - 1)
        );

        persistirEstado();
    }

    public synchronized void despacharProduto(String codigoProduto, int quantidadeDespachada)
            throws PersistenciaException, EstoqueInsuficienteException, ItemNaoEncontradoException {
        Item item = getItemOrThrow(codigoProduto);
        if (!(item instanceof ProdutoAcabado)) {
            throw new IllegalArgumentException("o codigo informado nao pertence a um produto acabado.");
        }

        item.removerQuantidade(
                quantidadeDespachada,
                "Despacho de " + quantidadeDespachada + " unidade(s) para expedicao."
        );
        movementLogRepository.appendEntry(
                item.getCodigo(),
                item.getHistoricoMovimentacoes().get(item.getHistoricoMovimentacoes().size() - 1)
        );

        persistirEstado();
    }

    public synchronized List<ItemSnapshot> listarEstoque() {
        List<ItemSnapshot> snapshots = new ArrayList<ItemSnapshot>();

        for (Item item : inventoryByCode.values()) {
            snapshots.add(item.toSnapshot());
        }

        snapshots.sort(Comparator.comparing(ItemSnapshot::getCodigo));
        return snapshots;
    }

    public synchronized ItemSnapshot consultarItem(String codigo) throws ItemNaoEncontradoException {
        return getItemOrThrow(codigo).toSnapshot();
    }

    public List<String> listarHistoricoItem(String codigo) throws PersistenciaException {
        return movementLogRepository.readHistoryForItem(codigo);
    }

    public synchronized void persistirEstado() throws PersistenciaException {
        inventoryFileRepository.saveInventory(listarEstoque());
    }

    private void loadSnapshot() throws PersistenciaException {
        List<Item> persistedItems = inventoryFileRepository.loadInventory();
        for (Item item : persistedItems) {
            inventoryByCode.put(item.getCodigo(), item);
        }
    }

    private Item getItemOrThrow(String codigo) throws ItemNaoEncontradoException {
        Item item = inventoryByCode.get(codigo);
        if (item == null) {
            throw new ItemNaoEncontradoException("item com codigo " + codigo + " nao encontrado.");
        }
        return item;
    }

    private void ensureCodeIsAvailable(String codigo) {
        if (inventoryByCode.containsKey(codigo)) {
            throw new IllegalArgumentException("ja existe um item cadastrado com o codigo informado.");
        }
    }
}
