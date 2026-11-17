package Exercicio02_ProcessadorInteiros;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class ProcessadorInteiros {
    // O servidor aceita uma conexao sequencial e o cliente envia a sequencia
    // de inteiros pelo mesmo canal TCP.
    private ProcessadorInteiros() {
    }

    public static void executar(Scanner scanner) throws IOException {
        System.out.println("Digite inteiros, um por linha. Digite 'fim' para enviar a sequência.");
        List<Integer> numeros = new ArrayList<Integer>();
        while (true) {
            System.out.print("inteiro> ");
            String linha = scanner.nextLine().trim();
            if ("fim".equalsIgnoreCase(linha)) {
                break;
            }
            try {
                numeros.add(Integer.parseInt(linha));
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida: informe um inteiro ou 'fim'.");
            }
        }

        // O ServerSocket fica passivo na porta escolhida pelo sistema e aceita o cliente TCP.
        try (ServerSocket servidor = new ServerSocket(0)) {
            Thread worker = new Thread(() -> processarUmaConexao(servidor), "integer-server");
            worker.setDaemon(true);
            worker.start();
            System.out.println("[SERVIDOR] Aguardando um unico cliente na porta "
                    + servidor.getLocalPort() + ".");
            try (Socket cliente = new Socket("127.0.0.1", servidor.getLocalPort());
                 // Cada inteiro e serializado como texto no OutputStream do Socket.
                 BufferedWriter saida = new BufferedWriter(new OutputStreamWriter(
                         cliente.getOutputStream(), StandardCharsets.UTF_8));
                 // A entrada corresponde ao fluxo recebido pelo processo remoto, decodificado
                 // de bytes para texto pelo BufferedReader.
                 BufferedReader entrada = new BufferedReader(new InputStreamReader(
                         cliente.getInputStream(), StandardCharsets.UTF_8))) {
                for (Integer numero : numeros) {
                    saida.write(numero.toString());
                    saida.newLine();
                    System.out.println("[CLIENTE] Enviando inteiro: " + numero);
                }
                saida.flush();
                // shutdownOutput() informa ao servidor que este cliente terminou de enviar dados,
                // mas ainda preserva a leitura da resposta no mesmo canal TCP.
                cliente.shutdownOutput();
                String resultado;
                while ((resultado = entrada.readLine()) != null) {
                    System.out.println("[SERVIDOR RESPONDEU] " + resultado);
                    System.out.println(resultado);
                }
            }
        }
    }

    private static void processarUmaConexao(ServerSocket servidor) {
        try (Socket cliente = servidor.accept();
             // O accept() bloqueia ate chegar uma conexao e devolve o Socket que representa
             // o canal ativo entre servidor e cliente. Nao confundo este socket com o ServerSocket.
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     cliente.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter saida = new BufferedWriter(new OutputStreamWriter(
                     cliente.getOutputStream(), StandardCharsets.UTF_8))) {
            System.out.printf("[CONEXAO] Cliente recebido: IP %s / Porta remota %d%n",
                    cliente.getInetAddress().getHostAddress(), cliente.getPort());
            List<Long> numeros = new ArrayList<Long>();
            String linha;
            while ((linha = entrada.readLine()) != null) {
                try {
                    numeros.add(Long.parseLong(linha.trim()));
                } catch (NumberFormatException e) {
                    saida.write("ERRO: valor inválido recebido: " + linha);
                    saida.newLine();
                    saida.flush();
                }
            }

            if (numeros.isEmpty()) {
                escrever(saida, "Quantidade: 0");
                escrever(saida, "SOMA: 0");
                escrever(saida, "MEDIA: indefinida (nenhum valor recebido)");
                escrever(saida, "MAX: indefinido");
                escrever(saida, "MIN: indefinido");
                return;
            }

            BigInteger soma = BigInteger.ZERO;
            long maximo = Long.MIN_VALUE;
            long minimo = Long.MAX_VALUE;
            for (Long numero : numeros) {
                soma = soma.add(BigInteger.valueOf(numero));
                maximo = Math.max(maximo, numero);
                minimo = Math.min(minimo, numero);
            }
            escrever(saida, "Quantidade: " + numeros.size());
            escrever(saida, "SOMA: " + soma);
            escrever(saida, "MEDIA: " + new java.math.BigDecimal(soma)
                    .divide(java.math.BigDecimal.valueOf(numeros.size()),
                            java.math.MathContext.DECIMAL64).stripTrailingZeros().toPlainString());
            escrever(saida, "MAX: " + maximo);
            escrever(saida, "MIN: " + minimo);
        } catch (IOException e) {
            System.err.println("Erro no servidor de inteiros: " + e.getMessage());
        }
    }

    private static void escrever(BufferedWriter saida, String linha) throws IOException {
        saida.write(linha);
        saida.newLine();
        saida.flush();
    }
}
