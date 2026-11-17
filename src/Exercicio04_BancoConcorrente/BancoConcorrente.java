package Exercicio04_BancoConcorrente;

import java.io.IOException;
import java.util.Scanner;

/**
 * Esta classe e o ponto de entrada didatico do Exercicio 4.
 * Aqui eu escolho iniciar o servidor ou abrir um cliente bancario.
 */
public final class BancoConcorrente {
    private BancoConcorrente() {
    }

    public static void executar(Scanner scanner) throws IOException {
        System.out.println();
        System.out.println("==========================================");
        System.out.println("[EXERCICIO 4] INSTITUICAO FINANCEIRA");
        System.out.println("1 - Iniciar servidor");
        System.out.println("2 - Conectar cliente");
        System.out.println("==========================================");
        System.out.print("Escolha o papel desta instancia: ");
        String papel = scanner.nextLine().trim();

        if ("1".equals(papel)) {
            executarServidor(scanner);
        } else if ("2".equals(papel)) {
            executarCliente(scanner);
        } else {
            System.out.println("[CLIENTE] Opcao invalida.");
        }
    }

    private static void executarServidor(Scanner scanner) throws IOException {
        System.out.print("Porta do servidor (pressione Enter para 5004): ");
        String textoPorta = scanner.nextLine().trim();
        int porta;
        try {
            porta = textoPorta.isEmpty() ? 5004 : Integer.parseInt(textoPorta);
        } catch (NumberFormatException e) {
            System.out.println("[SERVIDOR] Porta invalida; usando 5004.");
            porta = 5004;
        }

        try (ServidorBanco servidor = new ServidorBanco(porta)) {
            System.out.println("[SERVIDOR] Pressione Enter para encerrar o servidor...");
            scanner.nextLine();
        }
    }

    private static void executarCliente(Scanner scanner) throws IOException {
        System.out.print("IP do servidor (pressione Enter para 127.0.0.1): ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) {
            host = "127.0.0.1";
        }
        System.out.print("Porta do servidor (pressione Enter para 5004): ");
        String textoPorta = scanner.nextLine().trim();
        int porta;
        try {
            porta = textoPorta.isEmpty() ? 5004 : Integer.parseInt(textoPorta);
        } catch (NumberFormatException e) {
            System.out.println("[CLIENTE] Porta invalida; usando 5004.");
            porta = 5004;
        }
        ClienteBanco.executar(scanner, host, porta);
    }
}
