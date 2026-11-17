package Exercicio04_BancoConcorrente;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Aqui eu implemento o servidor passivo da instituicao financeira.
 * O ServerSocket escuta uma porta; a cada accept(), eu crio uma thread
 * independente para atender o Socket ativo do cliente.
 */
public final class ServidorBanco implements AutoCloseable {
    private final ServerSocket serverSocket;
    private final ExecutorService threadsClientes = Executors.newCachedThreadPool();
    private final AtomicBoolean executando = new AtomicBoolean(true);
    private final AtomicInteger sequenciaTerminal = new AtomicInteger(1);
    private final Map<String, Conta> contas = new LinkedHashMap<String, Conta>();
    private final Thread acceptor;

    public ServidorBanco(int porta) throws IOException {
        // Aqui eu crio o ServerSocket, que fica aguardando solicitacoes de conexao.
        serverSocket = new ServerSocket(porta);
        contas.put("123", new Conta("123", "1000.00"));
        contas.put("456", new Conta("456", "500.00"));
        acceptor = new Thread(this::aceitarClientes, "servidor-banco-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();

        System.out.println("[SERVIDOR] Servidor bancario iniciado.");
        System.out.println("[SERVIDOR] Contas disponiveis: 123 (R$ 1000.00) e 456 (R$ 500.00).");
        System.out.printf("[SERVIDOR] Escutando na porta TCP %d. Aguardando conexoes...%n",
                serverSocket.getLocalPort());
    }

    private void aceitarClientes() {
        // Aqui eu continuo esperando novas conexoes. Cada cliente recebe um Socket
        // ativo em accept() e uma thread propria no ExecutorService.
        while (executando.get()) {
            try {
                Socket socket = serverSocket.accept();
                int terminalId = sequenciaTerminal.getAndIncrement();
                TratadorClienteBanco tratador =
                        new TratadorClienteBanco(socket, terminalId, contas);
                threadsClientes.execute(tratador);
            } catch (IOException e) {
                if (executando.get()) {
                    System.err.println("[ERRO] Falha ao aceitar cliente bancario: " + e.getMessage());
                }
            }
        }
    }

    public int getPorta() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void close() throws IOException {
        // Aqui eu encerro a escuta, desativo o loop de accept() e fechamos as threads
        // de atendimento junto com a liberacao da porta logica.
        executando.set(false);
        serverSocket.close();
        threadsClientes.shutdownNow();
        System.out.println("[SERVIDOR] Servidor bancario encerrado.");
    }
}
