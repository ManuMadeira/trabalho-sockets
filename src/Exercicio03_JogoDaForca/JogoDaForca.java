package Exercicio03_JogoDaForca;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class JogoDaForca {
    private static final String[] PALAVRAS = {
            "concorrencia", "protocolo", "distribuido", "computador", "biblioteca"
    };
    private static final int MAX_ERROS = 6;

    private JogoDaForca() {
    }

    public static void executar(Scanner scanner) throws IOException {
        try (ServerSocket servidor = new ServerSocket(0)) {
            Thread worker = new Thread(() -> atenderJogo(servidor), "hangman-server");
            worker.setDaemon(true);
            worker.start();
            try (Socket cliente = new Socket("127.0.0.1", servidor.getLocalPort());
                 BufferedReader entrada = new BufferedReader(new InputStreamReader(
                         cliente.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter saida = new PrintWriter(new OutputStreamWriter(
                         cliente.getOutputStream(), StandardCharsets.UTF_8), true)) {
                String inicial = entrada.readLine();
                if (inicial == null) {
                    throw new IOException("Não foi possível iniciar a partida.");
                }
                System.out.println("Jogo da Forca remoto iniciado. " + inicial);
                System.out.println("Envie uma letra por tentativa.");
                while (true) {
                    System.out.print("Letra (ou 'sair'): ");
                    String palpite = scanner.nextLine().trim();
                    if ("sair".equalsIgnoreCase(palpite)) {
                        saida.println("QUIT");
                        System.out.println(entrada.readLine());
                        return;
                    }
                    saida.println(palpite);
                    String estado = entrada.readLine();
                    if (estado == null) {
                        throw new IOException("Conexão com o servidor do jogo encerrada.");
                    }
                    System.out.println(estado);
                    if (estado.startsWith("RESULT ")) {
                        break;
                    }
                }
            }
        }
    }

    private static void atenderJogo(ServerSocket servidor) {
        try (Socket cliente = servidor.accept();
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     cliente.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter saida = new PrintWriter(new OutputStreamWriter(
                     cliente.getOutputStream(), StandardCharsets.UTF_8), true)) {
            String palavra = PALAVRAS[ThreadLocalRandom.current().nextInt(PALAVRAS.length)];
            Set<Character> acertadas = new HashSet<Character>();
            Set<Character> tentadas = new HashSet<Character>();
            int erros = 0;
            saida.println("WORD_LENGTH " + palavra.length() + " | TENTATIVAS " + MAX_ERROS);

            String palpite;
            while ((palpite = entrada.readLine()) != null) {
                if ("QUIT".equalsIgnoreCase(palpite.trim())) {
                    saida.println("Partida encerrada pelo jogador. Palavra: " + palavra);
                    break;
                }
                String normalizado = normalizar(palpite);
                if (normalizado.length() != 1 || !Character.isLetter(normalizado.charAt(0))) {
                    saida.println("ERRO: envie exatamente uma letra.");
                    continue;
                }
                char letra = normalizado.charAt(0);
                if (!tentadas.add(letra)) {
                    saida.println("ERRO: essa letra já foi tentada.");
                    continue;
                }
                if (palavra.indexOf(letra) < 0) {
                    erros++;
                }
                if (palavra.indexOf(letra) >= 0) {
                    acertadas.add(letra);
                }
                String estado = montarEstado(palavra, acertadas);
                if (!estado.contains("_")) {
                    saida.println("RESULT VITORIA | " + estado + " | erros=" + erros + "/" + MAX_ERROS
                            + " | boneco=" + desenharBoneco(erros));
                    break;
                }
                if (erros >= MAX_ERROS) {
                    saida.println("RESULT DERROTA | palavra=" + palavra + " | erros=" + erros + "/" + MAX_ERROS
                            + " | boneco=" + desenharBoneco(erros));
                    break;
                }
                saida.println("STATE " + estado + " | erros=" + erros + "/" + MAX_ERROS
                        + " | tentadas=" + tentadas + " | boneco=" + desenharBoneco(erros));
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor do Jogo da Forca: " + e.getMessage());
        }
    }

    private static String montarEstado(String palavra, Set<Character> acertadas) {
        StringBuilder estado = new StringBuilder();
        for (char letra : palavra.toCharArray()) {
            if (estado.length() > 0) {
                estado.append(' ');
            }
            estado.append(acertadas.contains(letra) ? letra : '_');
        }
        return estado.toString();
    }

    private static String desenharBoneco(int erros) {
        String cabeca = erros >= 1 ? "O" : " ";
        String bracoEsquerdo = erros >= 3 ? "/" : " ";
        String tronco = erros >= 2 ? "|" : " ";
        String bracoDireito = erros >= 4 ? "\\" : " ";
        String pernaEsquerda = erros >= 5 ? "/" : " ";
        String pernaDireita = erros >= 6 ? "\\" : " ";
        return " +---+ / |   | / |   " + cabeca + " / |  " + bracoEsquerdo + tronco
                + bracoDireito + " / |  " + pernaEsquerda + " " + pernaDireita + " /_|_";
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
