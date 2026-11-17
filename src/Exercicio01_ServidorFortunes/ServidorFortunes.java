package Exercicio01_ServidorFortunes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ServidorFortunes implements AutoCloseable {
    private final ServerSocket serverSocket;
    private final ExecutorService clientes = Executors.newCachedThreadPool();
    private final List<String> fortunes = Collections.synchronizedList(new ArrayList<String>(Arrays.asList(
            "A persistência abre caminhos inesperados.",
            "Grandes projetos começam com pequenos passos.",
            "Aprender em conjunto multiplica resultados."
    )));
    private final AtomicBoolean executando = new AtomicBoolean(true);
    private final Thread acceptor;

    public ServidorFortunes(int porta) throws IOException {
        // O ServerSocket cria o ponto passivo do servidor TCP e reserva uma porta
        // logica na maquina e aguarda solicitacoes, enquanto o processo continua identificavel
        // pelo endereco IP da interface de rede e por essa porta.
        serverSocket = new ServerSocket(porta);
        acceptor = new Thread(this::aceitarClientes, "fortunes-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    public int getPorta() {
        return serverSocket.getLocalPort();
    }

    private void aceitarClientes() {
        while (executando.get()) {
            try {
                // O accept() bloqueia o fluxo ate um cliente iniciar o handshake TCP.
                // O resultado e um novo Socket ativo para conversar com aquele cliente
                // por um canal bidirecional; o ServerSocket continua escutando.
                Socket socket = serverSocket.accept();
                System.out.printf("[CONEXAO] Novo cliente fortunes: IP %s / Porta remota %d%n",
                        socket.getInetAddress().getHostAddress(), socket.getPort());
                clientes.execute(() -> atender(socket));
            } catch (IOException e) {
                if (executando.get()) {
                    System.err.println("Falha ao aceitar cliente de fortunes: " + e.getMessage());
                }
            }
        }
    }

    private void atender(Socket socket) {
        try (Socket cliente = socket;
             // O InputStream recebe bytes da rede e o BufferedReader os transforma
             // em texto UTF-8, linha por linha.
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     cliente.getInputStream(), StandardCharsets.UTF_8));
             // Pelo OutputStream sao enviados os dados serializados como texto em sentido inverso.
             // O PrintWriter adiciona o formato de linhas e permite a confirmacao de respostas.
             PrintWriter saida = new PrintWriter(new OutputStreamWriter(
                     cliente.getOutputStream(), StandardCharsets.UTF_8), true)) {
            System.out.println("[SERVIDOR] Conexao fortunes pronta para receber comandos.");
            String comando;
            while ((comando = entrada.readLine()) != null) {
                System.out.println("[REQUISICAO] Comando recebido: " + comando);
                String resposta = processar(comando);
                saida.println(resposta);
                System.out.println("[RESPOSTA] Dados enviados ao cliente fortunes.");
                if ("QUIT".equalsIgnoreCase(comando.trim())) {
                    break;
                }
                if ("LST-FORTUNE".equalsIgnoreCase(comando.trim())) {
                    saida.println("END");
                }
            }
        } catch (IOException e) {
            // Falhas de rede, leitura ou escrita aparecem como IOException; o erro e registrado
            // sem encerrar todo o servidor.
            System.err.println("Conexão de fortunes encerrada com erro: " + e.getMessage());
        }
    }

    private String processar(String comando) {
        String texto = comando.trim();
        if ("GET-FORTUNE".equalsIgnoreCase(texto)) {
            synchronized (fortunes) {
                return fortunes.get(ThreadLocalRandom.current().nextInt(fortunes.size()));
            }
        }
        if ("LST-FORTUNE".equalsIgnoreCase(texto)) {
            synchronized (fortunes) {
                return fortunes.isEmpty() ? "(nenhuma fortune cadastrada)" : listarFortunes();
            }
        }
        if (texto.regionMatches(true, 0, "ADD-FORTUNE ", 0, 12)) {
            String frase = texto.substring(12).trim();
            if (frase.isEmpty()) {
                return "ERRO: informe a frase.";
            }
            synchronized (fortunes) {
                fortunes.add(frase);
                return "OK: fortune adicionada na posição " + fortunes.size() + ".";
            }
        }
        if (texto.regionMatches(true, 0, "UPD-FORTUNE ", 0, 12)) {
            int separador = texto.indexOf(' ', 12);
            if (separador < 0) {
                return "ERRO: use UPD-FORTUNE <pos> <frase>.";
            }
            try {
                int posicao = Integer.parseInt(texto.substring(12, separador));
                String frase = texto.substring(separador + 1).trim();
                synchronized (fortunes) {
                    if (posicao < 1 || posicao > fortunes.size() || frase.isEmpty()) {
                        return "ERRO: posição inválida ou frase vazia.";
                    }
                    fortunes.set(posicao - 1, frase);
                }
                return "OK: fortune atualizada.";
            } catch (NumberFormatException e) {
                return "ERRO: posição deve ser um inteiro.";
            }
        }
        if ("QUIT".equalsIgnoreCase(texto)) {
            return "BYE";
        }
        return "ERRO: comando desconhecido.";
    }

    private String listarFortunes() {
        StringBuilder resposta = new StringBuilder();
        for (int i = 0; i < fortunes.size(); i++) {
            if (i > 0) {
                resposta.append('\n');
            }
            resposta.append(i + 1).append(": ").append(fortunes.get(i));
        }
        return resposta.toString();
    }

    @Override
    public void close() throws IOException {
        executando.set(false);
        // Uso close() para liberar o socket do servidor e a porta, encerrando de forma segura
        // os recursos de rede desta instancia.
        serverSocket.close();
        clientes.shutdownNow();
    }

    public static void executar(Scanner scanner) throws IOException {
        try (ServidorFortunes servidor = new ServidorFortunes(0);
             // Aqui o Socket atua ativamente: 127.0.0.1 identifica a maquina local
             // (localhost) e servidor.getPorta() identifica o processo de destino.
             Socket cliente = new Socket("127.0.0.1", servidor.getPorta());
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     cliente.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter saida = new PrintWriter(new OutputStreamWriter(
                     cliente.getOutputStream(), StandardCharsets.UTF_8), true)) {
            System.out.println("Servidor TCP de fortunes ativo na porta " + servidor.getPorta() + ".");
            System.out.println("Comandos: GET-FORTUNE, ADD-FORTUNE <frase>, UPD-FORTUNE <pos> <frase>,");
            System.out.println("          LST-FORTUNE, QUIT");
            boolean conectado = true;
            while (conectado) {
                System.out.print("fortune> ");
                String comando = scanner.nextLine().trim();
                saida.println(comando);
                if ("LST-FORTUNE".equalsIgnoreCase(comando)) {
                    String linha;
                    while ((linha = entrada.readLine()) != null && !"END".equals(linha)) {
                        System.out.println(linha);
                    }
                } else {
                    String resposta = entrada.readLine();
                    if (resposta == null) {
                        throw new IOException("O servidor encerrou a conexão inesperadamente.");
                    }
                    System.out.println(resposta);
                }
                conectado = !"QUIT".equalsIgnoreCase(comando);
            }
        }
    }
}
