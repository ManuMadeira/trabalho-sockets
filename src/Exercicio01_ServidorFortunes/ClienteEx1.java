package Exercicio01_ServidorFortunes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Esta classe implementa a ponta cliente do Exercício 1.
 * O cliente solicita ao servidor TCP o handshake na porta 5001 e envia
 * comandos textuais pela rede usando UTF-8.
 */
public final class ClienteEx1 {
    public static final String HOST = "127.0.0.1";
    public static final int PORTA = 5001;

    private ClienteEx1() {
    }

    public static void main(String[] args) {
        // O Socket atua ativamente: o IP do servidor e a porta logica do processo
        // de destino estabelecem o canal bidirecional.
        try (Socket cliente = new Socket(HOST, PORTA);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     cliente.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter saida = new PrintWriter(new OutputStreamWriter(
                     cliente.getOutputStream(), StandardCharsets.UTF_8), true);
             Scanner scanner = new Scanner(System.in, "UTF-8")) {

            System.out.println("[CLIENTE] Conectado ao Servidor de Fortunes.");
            System.out.printf("[CONEXAO] Servidor %s / Porta %d%n", HOST, PORTA);
            System.out.println("[SERVIDOR] " + entrada.readLine());
            exibirMenu();

            boolean conectado = true;
            while (conectado) {
                System.out.print("fortune> ");
                String comando = scanner.nextLine().trim();
                if (comando.isEmpty()) {
                    continue;
                }

                // Neste ponto o texto e serializado no OutputStream do Socket.
                // Cada println inclui uma quebra de linha que delimita o comando.
                System.out.println("[CLIENTE] Enviando comando: " + comando);
                saida.println(comando);

                if ("LST-FORTUNE".equalsIgnoreCase(comando)) {
                    System.out.println("[SERVIDOR RESPONDEU]");
                    String linha;
                    while ((linha = entrada.readLine()) != null && !"END".equals(linha)) {
                        System.out.println(linha);
                    }
                } else {
                    String resposta = entrada.readLine();
                    if (resposta == null) {
                        throw new IOException("O servidor encerrou a conexao inesperadamente.");
                    }
                    System.out.println("[SERVIDOR RESPONDEU] " + resposta);
                }

                conectado = !"QUIT".equalsIgnoreCase(comando);
            }
            System.out.println("[CLIENTE] Encerrando a conexao com close().");
        } catch (IOException e) {
            // Falhas de conexao, envio ou leitura sao informadas como IOException.
            System.err.println("[ERRO] Falha na comunicacao com o servidor: " + e.getMessage());
        }
    }

    private static void exibirMenu() {
        System.out.println("==============================================");
        System.out.println("Comandos disponiveis:");
        System.out.println("GET-FORTUNE");
        System.out.println("ADD-FORTUNE <frase>");
        System.out.println("UPD-FORTUNE <posicao> <frase>");
        System.out.println("LST-FORTUNE");
        System.out.println("QUIT");
        System.out.println("==============================================");
    }
}
