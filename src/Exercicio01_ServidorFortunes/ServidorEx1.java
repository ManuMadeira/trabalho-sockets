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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Esta classe implementa a ponta servidor do Exercício 1.
 * O servidor aguarda clientes TCP na porta 5001 e responde
 * aos comandos de texto do protocolo de fortunes.
 */
public final class ServidorEx1 {
    public static final int PORTA = 5001;

    private ServidorEx1() {
    }

    public static void main(String[] args) {
        // O ServerSocket representa a porta passiva na qual
        // o servidor escuta conexoes e nao conversa diretamente com os clientes.
        try (ServerSocket servidor = new ServerSocket(PORTA)) {
            ExecutorService threads = Executors.newCachedThreadPool();
            List<String> fortunes = Collections.synchronizedList(new ArrayList<String>(Arrays.asList(
                    "A persistencia abre caminhos inesperados.",
                    "Grandes projetos começam com pequenos passos.",
                    "Aprender em conjunto multiplica resultados."
            )));

            System.out.println("[SERVIDOR] Servidor de Fortunes iniciado.");
            System.out.println("[SERVIDOR] IP local: 127.0.0.1");
            System.out.println("[SERVIDOR] Porta TCP: " + PORTA);
            System.out.println("[SERVIDOR] Aguardando clientes...");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> threads.shutdownNow()));

            while (true) {
                // Aqui o accept() fica bloqueado ate um cliente iniciar o handshake TCP.
                // Quando chega uma conexao, um Socket ativo e criado para aquele terminal.
                Socket cliente = servidor.accept();
                System.out.printf("[CONEXAO] Novo cliente: IP %s / Porta remota %d%n",
                        cliente.getInetAddress().getHostAddress(), cliente.getPort());
                threads.execute(() -> atenderCliente(cliente, fortunes));
            }
        } catch (IOException e) {
            // Falhas de abertura da porta, accept() ou encerramento da rede sao tratadas aqui.
            System.err.println("[ERRO] Falha no servidor de Fortunes: " + e.getMessage());
        }
    }

    private static void atenderCliente(Socket cliente, List<String> fortunes) {
        // Cada thread atende um unico cliente, mas varias threads podem
        // atender varios clientes ao mesmo tempo no mesmo ServerSocket.
        try (Socket conexao = cliente;
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     conexao.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter saida = new PrintWriter(new OutputStreamWriter(
                     conexao.getOutputStream(), StandardCharsets.UTF_8), true)) {

            System.out.printf("[SERVIDOR] Canal TCP pronto para o cliente %s:%d.%n",
                    conexao.getInetAddress().getHostAddress(), conexao.getPort());
            saida.println("Bem-vindo ao Servidor de Fortunes.");

            String comando;
            while ((comando = entrada.readLine()) != null) {
                System.out.printf("[REQUISICAO] Cliente %s:%d enviou: %s%n",
                        conexao.getInetAddress().getHostAddress(),
                        conexao.getPort(), comando);

                String resposta = processarComando(comando, fortunes);
                System.out.println("[RESPOSTA] Enviando ao cliente: " + resposta);
                saida.println(resposta);

                if ("QUIT".equalsIgnoreCase(comando.trim())) {
                    break;
                }
                if ("LST-FORTUNE".equalsIgnoreCase(comando.trim())) {
                    System.out.println("[RESPOSTA] Enviando marcador de fim: END");
                    saida.println("END");
                }
            }
            System.out.printf("[DESCONECTADO] Cliente %s:%d encerrou a sessao.%n",
                    conexao.getInetAddress().getHostAddress(), conexao.getPort());
        } catch (IOException e) {
            System.err.printf("[ERRO] Conexao do cliente encerrada com IOException: %s%n",
                    e.getMessage());
        }
    }

    private static String processarComando(String comando, List<String> fortunes) {
        String texto = comando.trim();

        if ("GET-FORTUNE".equalsIgnoreCase(texto)) {
            synchronized (fortunes) {
                return fortunes.get(ThreadLocalRandom.current().nextInt(fortunes.size()));
            }
        }

        if ("LST-FORTUNE".equalsIgnoreCase(texto)) {
            synchronized (fortunes) {
                if (fortunes.isEmpty()) {
                    return "(nenhuma fortune cadastrada)";
                }
                StringBuilder resposta = new StringBuilder();
                for (int i = 0; i < fortunes.size(); i++) {
                    if (i > 0) {
                        resposta.append('\n');
                    }
                    resposta.append(i + 1).append(": ").append(fortunes.get(i));
                }
                return resposta.toString();
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
                    return "OK: fortune atualizada.";
                }
            } catch (NumberFormatException e) {
                return "ERRO: posição deve ser um inteiro.";
            }
        }

        if ("QUIT".equalsIgnoreCase(texto)) {
            return "BYE";
        }
        return "ERRO: comando desconhecido.";
    }
}
