package com.fabrica;

import java.util.Scanner;

public class Main {
    private static final Scanner SCANNER;

    public static void main(String[] var0) {
        executarMenu();
    }

    private static void executarMenu() {
        int var0;
        do {
            exibirMenu();
            var0 = lerOpcao();
            processarOpcao(var0);
        } while(var0 != 0);

    }

    private static void exibirMenu() {
        System.out.println();
        System.out.println("=== FABRILOOP - CONTROLE DE ESTOQUE ===");
        System.out.println("1. \ud83d\udce6 Cadastrar Nova Matéria-Prima");
        System.out.println("2. \ud83c\udfed Iniciar Produção (Consumir Lote)");
        System.out.println("3. \ud83d\ude9a Despachar Produto Acabado");
        System.out.println("4. \ud83d\udd0d Rastrear Item por Código");
        System.out.println("5. ⚠️ Visualizar Alertas do Sistema");
        System.out.println("0. Sair");
        System.out.println("=======================================");
        System.out.print("Escolha uma opção: ");
    }

    private static int lerOpcao() {
        while(!SCANNER.hasNextInt()) {
            System.out.print("Opção inválida. Digite um número do menu: ");
            SCANNER.nextLine();
        }

        int var0 = SCANNER.nextInt();
        SCANNER.nextLine();
        return var0;
    }

    private static void processarOpcao(int var0) {
        switch (var0) {
            case 0 -> System.out.println("Encerrando o sistema...");
            case 1 -> System.out.println("Cadastro de nova matéria-prima selecionado.");
            case 2 -> System.out.println("Início de produção selecionado.");
            case 3 -> System.out.println("Despacho de produto acabado selecionado.");
            case 4 -> System.out.println("Rastreamento por código selecionado.");
            case 5 -> System.out.println("Visualização de alertas selecionada.");
            default -> System.out.println("Opção inexistente. Tente novamente.");
        }

    }

    static {
        SCANNER = new Scanner(System.in);
    }
}
