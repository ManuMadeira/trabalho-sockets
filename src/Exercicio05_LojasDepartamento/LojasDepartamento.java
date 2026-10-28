package Exercicio05_LojasDepartamento;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

public final class LojasDepartamento {
    private static final int QUANTIDADE_FILIAIS = 5;
    private static final int TRANSACOES_POR_FILIAL = 1500;

    private LojasDepartamento() {
    }

    public static void executar() throws InterruptedException, ExecutionException, IOException {
        System.out.println("Simulando 5 filiais com 1500 compras/vendas por filial...");
        try (ServerSocket central = new ServerSocket(0)) {
            ExecutorService servidorCentral = Executors.newSingleThreadExecutor();
            ExecutorService filiais = Executors.newFixedThreadPool(QUANTIDADE_FILIAIS);
            try {
                Future<Consolidado> consolidado = servidorCentral.submit(() -> receberFiliais(central));
                List<Future<ResultadoFilial>> execucoes =
                        new ArrayList<Future<ResultadoFilial>>(QUANTIDADE_FILIAIS);
                for (int filial = 1; filial <= QUANTIDADE_FILIAIS; filial++) {
                    final int numeroFilial = filial;
                    final int porta = central.getLocalPort();
                    execucoes.add(filiais.submit(new Callable<ResultadoFilial>() {
                        @Override
                        public ResultadoFilial call() throws Exception {
                            ResultadoFilial resultado = simularFilial(numeroFilial);
                            enviarResultado(porta, resultado);
                            return resultado;
                        }
                    }));
                }

                for (Future<ResultadoFilial> execucao : execucoes) {
                    ResultadoFilial resultado = execucao.get();
                    System.out.printf("Filial %d: compras = R$ %d, vendas = R$ %d, operações = %d%n",
                            resultado.filial, resultado.compras, resultado.vendas, resultado.transacoes);
                }

                Consolidado resultadoCentral = consolidado.get();
                System.out.println("--------------------------------------------");
                System.out.printf("Consolidado recebido por TCP: compras = R$ %d, vendas = R$ %d%n",
                        resultadoCentral.compras, resultadoCentral.vendas);
                System.out.println("Transações consolidadas: " + resultadoCentral.transacoes);
            } finally {
                filiais.shutdownNow();
                servidorCentral.shutdownNow();
            }
        }
    }

    private static ResultadoFilial simularFilial(int filial) {
        long compras = 0L;
        long vendas = 0L;
        for (int i = 0; i < TRANSACOES_POR_FILIAL; i++) {
            long valor = ThreadLocalRandom.current().nextLong(10, 1001);
            if (ThreadLocalRandom.current().nextBoolean()) {
                compras += valor;
            } else {
                vendas += valor;
            }
            LockSupport.parkNanos(ThreadLocalRandom.current().nextLong(100_000, 500_001));
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("Simulação da filial interrompida.");
            }
        }
        return new ResultadoFilial(filial, compras, vendas, TRANSACOES_POR_FILIAL);
    }

    private static void enviarResultado(int porta, ResultadoFilial resultado) throws IOException {
        try (Socket cliente = new Socket("127.0.0.1", porta);
             DataOutputStream saida = new DataOutputStream(cliente.getOutputStream())) {
            saida.writeInt(resultado.filial);
            saida.writeLong(resultado.compras);
            saida.writeLong(resultado.vendas);
            saida.writeInt(resultado.transacoes);
        }
    }

    private static Consolidado receberFiliais(ServerSocket servidor) throws IOException {
        Consolidado total = new Consolidado();
        for (int i = 0; i < QUANTIDADE_FILIAIS; i++) {
            try (Socket filial = servidor.accept();
                 DataInputStream entrada = new DataInputStream(filial.getInputStream())) {
                int numero = entrada.readInt();
                long compras = entrada.readLong();
                long vendas = entrada.readLong();
                int transacoes = entrada.readInt();
                if (numero < 1 || numero > QUANTIDADE_FILIAIS || compras < 0 || vendas < 0
                        || transacoes != TRANSACOES_POR_FILIAL) {
                    throw new IOException("Dados inválidos recebidos de uma filial.");
                }
                total.compras += compras;
                total.vendas += vendas;
                total.transacoes += transacoes;
            }
        }
        return total;
    }

    private static final class ResultadoFilial {
        private final int filial;
        private final long compras;
        private final long vendas;
        private final int transacoes;

        private ResultadoFilial(int filial, long compras, long vendas, int transacoes) {
            this.filial = filial;
            this.compras = compras;
            this.vendas = vendas;
            this.transacoes = transacoes;
        }
    }

    private static final class Consolidado {
        private long compras;
        private long vendas;
        private int transacoes;
    }
}
