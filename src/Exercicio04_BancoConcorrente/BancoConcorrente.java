package Exercicio04_BancoConcorrente;

import java.io.IOException;
import java.util.Scanner;

/**
 * Ponto de entrada do Exercício 4: permite iniciar o servidor bancário
 * ou conectar um terminal cliente. Para testar concorrência, execute o
 * servidor em um terminal e clientes em outros.
 */
public final class BancoConcorrente {
    private BancoConcorrente() {
    }

    public static void executar(Scanner scanner) throws IOException {
        // Lembrete: o servidor aguarda conexões TCP; cada cliente precisa do IP
        // e da porta corretos para encontrar esse processo.
        System.out.println();
        System.out.println("==========================================");
        System.out.println("[EXERCICIO 4] INSTITUICAO FINANCEIRA");
        System.out.println("1 - Iniciar servidor");
        System.out.println("2 - Conectar cliente");
        System.out.println("==========================================");
        System.out.print("Escolha o papel desta instancia: ");
        String papel = scanner.nextLine().trim();

        // A escolha define o papel desta execução; servidor e clientes podem
        // rodar em processos e terminais separados.
        if ("1".equals(papel)) {
            executarServidor(scanner);
        } else if ("2".equals(papel)) {
            executarCliente(scanner);
        } else {
            System.out.println("[CLIENTE] Opcao invalida.");
        }
    }

    private static void executarServidor(Scanner scanner) throws IOException {
        // Porta lógica de escuta do ServerSocket. O cliente deve usar a mesma porta.
        System.out.print("Porta do servidor (pressione Enter para 5004): ");
        String textoPorta = scanner.nextLine().trim();
        int porta;
        try {
            porta = textoPorta.isEmpty() ? 5004 : Integer.parseInt(textoPorta);
        } catch (NumberFormatException e) {
            System.out.println("[SERVIDOR] Porta invalida; usando 5004.");
            porta = 5004;
        }

        // O try-with-resources fecha o servidor e libera a porta ao sair deste bloco.
        // IOException é propagada para que a camada principal informe falhas de rede.
        try (ServidorBanco servidor = new ServidorBanco(porta)) {
            System.out.println("[SERVIDOR] Pressione Enter para encerrar o servidor...");
            scanner.nextLine();
        }
    }

    private static void executarCliente(Scanner scanner) throws IOException {
        // 127.0.0.1 indica localhost. Para conectar de outra máquina, use o IP
        // da interface de rede do computador que executa o servidor.
        System.out.print("IP do servidor (pressione Enter para 127.0.0.1): ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) {
            host = "127.0.0.1";
        }
        // Lembrete: informe a mesma porta configurada no servidor para abrir o canal TCP.
        System.out.print("Porta do servidor (pressione Enter para 5004): ");
        String textoPorta = scanner.nextLine().trim();
        int porta;
        try {
            porta = textoPorta.isEmpty() ? 5004 : Integer.parseInt(textoPorta);
        } catch (NumberFormatException e) {
            System.out.println("[CLIENTE] Porta invalida; usando 5004.");
            porta = 5004;
        }
        // O cliente envia comandos e recebe respostas pelo mesmo canal TCP bidirecional.
        ClienteBanco.executar(scanner, host, porta);
    }
}
