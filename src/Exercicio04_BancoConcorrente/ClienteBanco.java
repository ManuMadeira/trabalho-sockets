package Exercicio04_BancoConcorrente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Aqui eu implemento o terminal cliente que solicita depositos, saques
 * e consultas ao servidor por meio de um Socket TCP ativo.
 */
public final class ClienteBanco {
    private ClienteBanco() {
    }

    public static void executar(Scanner scanner, String host, int porta) throws IOException {
        // O Socket conecta o IP da maquina ao processo identificado pela porta TCP.
        // localhost/127.0.0.1 indica esta mesma maquina; em outra rede posso informar seu IP.
        try (Socket socket = new Socket(host, porta);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter saida = new PrintWriter(new OutputStreamWriter(
                     socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String boasVindas = entrada.readLine();
            System.out.println("[CONEXAO] Conectado ao servidor bancario " + host + ":" + porta);
            System.out.println("[SERVIDOR] " + boasVindas);

            boolean conectado = true;
            while (conectado) {
                exibirMenu();
                String opcao = lerTexto(scanner, "Escolha uma opcao: ");
                if ("1".equals(opcao)) {
                    processarOperacao(scanner, saida, entrada, "DEPOSITO");
                } else if ("2".equals(opcao)) {
                    processarOperacao(scanner, saida, entrada, "SAQUE");
                } else if ("3".equals(opcao)) {
                    String conta = lerTexto(scanner, "Numero da conta: ");
                    enviarComando(saida, entrada, "SALDO|" + conta,
                            "[CLIENTE] Enviando solicitacao de CONSULTA da Conta " + conta + "...");
                } else if ("4".equals(opcao)) {
                    enviarComando(saida, entrada, "SAIR",
                            "[CLIENTE] Solicitando encerramento da conexao...");
                    conectado = false;
                } else {
                    System.out.println("[CLIENTE] Opcao invalida. Escolha 1, 2, 3 ou 4.");
                }
            }
        } catch (IOException e) {
            // Aqui eu informo erros de conexao, envio ou leitura da rede.
            throw new IOException("[CLIENTE] Falha na comunicacao bancaria: " + e.getMessage(), e);
        }
    }

    private static void processarOperacao(Scanner scanner, PrintWriter saida,
                                          BufferedReader entrada, String operacao) throws IOException {
        String conta = lerTexto(scanner, "Numero da conta: ");
        String valor = lerTexto(scanner, "Valor (ex.: 100.00): ");
        String protocolo = operacao + "|" + conta + "|" + valor;
        enviarComando(saida, entrada, protocolo,
                "[CLIENTE] Enviando solicitacao de " + operacao + " de R$ "
                        + formatarEntrada(valor) + " na Conta " + conta + "...");
    }

    private static void enviarComando(PrintWriter saida, BufferedReader entrada,
                                      String protocolo, String descricao) throws IOException {
        System.out.println(descricao);
        // Aqui eu serializo o texto no OutputStream do Socket. A quebra de linha
        // define o fim da mensagem no protocolo implementado com o servidor.
        saida.println(protocolo);
        String resposta = entrada.readLine();
        if (resposta == null) {
            throw new IOException("O servidor bancario encerrou a conexao inesperadamente.");
        }
        exibirResposta(resposta);
    }

    private static void exibirMenu() {
        System.out.println();
        System.out.println("==========================================");
        System.out.println("[CLIENTE] MENU DO BANCO");
        System.out.println("1 - Depositar");
        System.out.println("2 - Sacar");
        System.out.println("3 - Consultar Saldo");
        System.out.println("4 - Sair");
        System.out.println("==========================================");
    }

    private static void exibirResposta(String resposta) {
        String[] partes = resposta.split("\\|");
        if (partes.length > 0 && "ERRO".equals(partes[0])) {
            System.out.print("[SERVIDOR RESPONDEU] ERRO: ");
            for (int i = 1; i < partes.length; i++) {
                if (i > 1) {
                    System.out.print(" | ");
                }
                System.out.print(partes[i]);
            }
            System.out.println();
            return;
        }
        if (partes.length >= 4 && "OK".equals(partes[0])
                && ("DEPOSITO".equals(partes[1]) || "SAQUE".equals(partes[1]))) {
            String acao = "DEPOSITO".equals(partes[1])
                    ? "Deposito realizado"
                    : "Saque realizado";
            System.out.printf("[SERVIDOR RESPONDEU] %s! Conta %s. Novo saldo: R$ %s%n",
                    acao, partes[2], partes[3]);
        } else if (partes.length >= 4 && "OK".equals(partes[0]) && "SALDO".equals(partes[1])) {
            System.out.printf("[SERVIDOR RESPONDEU] Conta %s. Saldo atual: R$ %s%n",
                    partes[2], partes[3]);
        } else if (partes.length >= 3 && "OK".equals(partes[0]) && "SAIR".equals(partes[1])) {
            System.out.println("[SERVIDOR RESPONDEU] " + partes[2]);
        } else {
            System.out.println("[SERVIDOR RESPONDEU] " + resposta);
        }
    }

    private static String lerTexto(Scanner scanner, String mensagem) {
        System.out.print(mensagem);
        return scanner.nextLine().trim();
    }

    private static String formatarEntrada(String valor) {
        try {
            java.math.BigDecimal numero =
                    new java.math.BigDecimal(valor.replace(',', '.'));
            return numero.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            return valor;
        }
    }
}
