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
 * Aqui eu implemento a ponta cliente do Exercício 1.
 * Eu solicito ao servidor TCP o handshake na porta 5001 e envio comandos
 * textuais pela rede UTF-8.
 */
public final class ClienteEx1 {
    public static final String HOST = "127.0.0.1";
    public static final int PORTA = 5001;

    private ClienteEx1() {
    }

    public static void main(String[] args) {
        // Aqui o Socket atua ativamente: eu informo o IP do servidor e a porta
        // logica do processo destino para estabelecer o canal bidirecional.
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

                // Neste ponto eu serializo o texto no OutputStream do Socket.
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
            // Aqui eu informo falhas de conexao, envio ou leitura como IOException.
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
