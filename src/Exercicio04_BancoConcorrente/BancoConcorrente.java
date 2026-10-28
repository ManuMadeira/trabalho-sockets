package Exercicio04_BancoConcorrente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BancoConcorrente implements AutoCloseable {
    private final ServerSocket serverSocket;
    private final ExecutorService clientes = Executors.newCachedThreadPool();
    private final AtomicBoolean executando = new AtomicBoolean(true);
    private final Conta conta = new Conta(new BigDecimal("1000.00"));
    private final Thread acceptor;

    public BancoConcorrente(int porta) throws IOException {
        serverSocket = new ServerSocket(porta);
        acceptor = new Thread(this::aceitarClientes, "bank-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    private void aceitarClientes() {
        while (executando.get()) {
            try {
                Socket socket = serverSocket.accept();
                clientes.execute(() -> atender(socket));
            } catch (IOException e) {
                if (executando.get()) {
                    System.err.println("Falha ao aceitar cliente do banco: " + e.getMessage());
                }
            }
        }
    }

    private void atender(Socket socket) {
        try (Socket cliente = socket;
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     cliente.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter saida = new PrintWriter(new OutputStreamWriter(
                     cliente.getOutputStream(), StandardCharsets.UTF_8), true)) {
            saida.println("Banco conectado. Comandos: DEPOSITO <valor>, SAQUE <valor>, SALDO, SAIR");
            String comando;
            while ((comando = entrada.readLine()) != null) {
                String[] partes = comando.trim().split("\\s+", 2);
                String resposta;
                if ("SALDO".equalsIgnoreCase(partes[0])) {
                    resposta = "SALDO " + conta.saldo();
                } else if ("SAIR".equalsIgnoreCase(partes[0])) {
                    saida.println("BYE");
                    break;
                } else if (partes.length == 2 && "DEPOSITO".equalsIgnoreCase(partes[0])) {
                    resposta = movimentar(partes[1], true);
                } else if (partes.length == 2 && "SAQUE".equalsIgnoreCase(partes[0])) {
                    resposta = movimentar(partes[1], false);
                } else {
                    resposta = "ERRO: comando inválido.";
                }
                saida.println(resposta);
            }
        } catch (IOException e) {
            System.err.println("Conexão bancária encerrada com erro: " + e.getMessage());
        }
    }

    private String movimentar(String valorTexto, boolean deposito) {
        try {
            BigDecimal valor = new BigDecimal(valorTexto.replace(',', '.'));
            if (valor.signum() <= 0) {
                return "ERRO: o valor deve ser positivo.";
            }
            BigDecimal saldo = deposito ? conta.depositar(valor) : conta.sacar(valor);
            if (saldo == null) {
                return "ERRO: saldo insuficiente. SALDO " + conta.saldo();
            }
            return (deposito ? "DEPOSITO OK" : "SAQUE OK") + " | SALDO " + saldo;
        } catch (NumberFormatException e) {
            return "ERRO: valor monetário inválido.";
        }
    }

    @Override
    public void close() throws IOException {
        executando.set(false);
        serverSocket.close();
        clientes.shutdownNow();
    }

    public static void executar(Scanner scanner) throws IOException {
        try (BancoConcorrente servidor = new BancoConcorrente(0);
             Socket cliente = new Socket("127.0.0.1", servidor.serverSocket.getLocalPort());
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     cliente.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter saida = new PrintWriter(new OutputStreamWriter(
                     cliente.getOutputStream(), StandardCharsets.UTF_8), true)) {
            System.out.println(entrada.readLine());
            boolean conectado = true;
            while (conectado) {
                System.out.print("banco> ");
                String comando = scanner.nextLine().trim();
                saida.println(comando);
                String resposta = entrada.readLine();
                if (resposta == null) {
                    throw new IOException("O servidor bancário encerrou a conexão inesperadamente.");
                }
                System.out.println(resposta);
                conectado = !"SAIR".equalsIgnoreCase(comando);
            }
        }
    }

    private static final class Conta {
        private BigDecimal saldo;

        private Conta(BigDecimal saldoInicial) {
            saldo = saldoInicial;
        }

        private synchronized BigDecimal depositar(BigDecimal valor) {
            saldo = saldo.add(valor);
            return saldo;
        }

        private synchronized BigDecimal sacar(BigDecimal valor) {
            if (saldo.compareTo(valor) < 0) {
                return null;
            }
            saldo = saldo.subtract(valor);
            return saldo;
        }

        private synchronized BigDecimal saldo() {
            return saldo;
        }
    }
}
