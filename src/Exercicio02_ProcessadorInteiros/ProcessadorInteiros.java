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

        try (ServerSocket servidor = new ServerSocket(0)) {
            Thread worker = new Thread(() -> processarUmaConexao(servidor), "integer-server");
            worker.setDaemon(true);
            worker.start();
            try (Socket cliente = new Socket("127.0.0.1", servidor.getLocalPort());
                 BufferedWriter saida = new BufferedWriter(new OutputStreamWriter(
                         cliente.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader entrada = new BufferedReader(new InputStreamReader(
                         cliente.getInputStream(), StandardCharsets.UTF_8))) {
                for (Integer numero : numeros) {
                    saida.write(numero.toString());
                    saida.newLine();
                }
                saida.flush();
                cliente.shutdownOutput();
                String resultado;
                while ((resultado = entrada.readLine()) != null) {
                    System.out.println(resultado);
                }
            }
        }
    }

    private static void processarUmaConexao(ServerSocket servidor) {
        try (Socket cliente = servidor.accept();
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     cliente.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter saida = new BufferedWriter(new OutputStreamWriter(
                     cliente.getOutputStream(), StandardCharsets.UTF_8))) {
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
