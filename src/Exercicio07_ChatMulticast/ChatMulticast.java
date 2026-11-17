package Exercicio07_ChatMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChatMulticast {
    // Esta classe representa um participante de um chat UDP multicast.
    // Nao existe ServerSocket nem handshake: os participantes entram no grupo IP e trocam DatagramPacket.
    private static final String GRUPO = "230.0.0.1";
    private static final int PORTA = 4446;
    private static final int TAMANHO_MAXIMO = 8192;

    private ChatMulticast() {
    }

    public static void executar(Scanner scanner) throws IOException {
        // O endereco multicast e o IP logico do grupo 230.0.0.1; a porta 4446
        // identifica o canal UDP usado pelos processos participantes.
        InetAddress grupo = InetAddress.getByName(GRUPO);
        AtomicBoolean conectado = new AtomicBoolean(true);
        try (MulticastSocket socket = new MulticastSocket(PORTA)) {
            // Ao entrar no grupo, o socket passa a receber datagramas UDP destinados
            // ao grupo. Ao contrario do TCP, nao existe handshake ou conexao persistente.
            socket.joinGroup(grupo);
            System.out.printf("[MULTICAST] Participante entrou no grupo %s:%d.%n", GRUPO, PORTA);
            Thread receptor = new Thread(() -> receberMensagens(socket, conectado), "multicast-receiver");
            receptor.setDaemon(true);
            receptor.start();

            System.out.print("Seu nome no chat: ");
            String nome = scanner.nextLine().trim();
            if (nome.isEmpty()) {
                nome = "participante";
            }
            System.out.println("Chat no grupo " + GRUPO + ":" + PORTA
                    + ". Digite mensagens; use /sair para retornar ao menu.");
            while (true) {
                String mensagem = scanner.nextLine();
                if ("/sair".equalsIgnoreCase(mensagem.trim())) {
                    break;
                }
                byte[] dados = (nome + ": " + mensagem).getBytes(StandardCharsets.UTF_8);
                if (dados.length > TAMANHO_MAXIMO) {
                    System.out.println("Mensagem excede o limite de " + TAMANHO_MAXIMO + " bytes.");
                    continue;
                }
                // A mensagem e serializada em bytes e enviada em um DatagramPacket contendo dados,
                // endereco IP do grupo e porta UDP de destino.
                socket.send(new DatagramPacket(dados, dados.length, grupo, PORTA));
                System.out.println("[CLIENTE] Mensagem enviada ao grupo: " + mensagem);
            }
            conectado.set(false);
            // close() e leaveGroup() encerram de forma segura o uso dos recursos do socket
            // e removem este processo do grupo multicast.
            socket.leaveGroup(grupo);
        } finally {
            conectado.set(false);
        }
    }

    private static void receberMensagens(MulticastSocket socket, AtomicBoolean conectado) {
        byte[] buffer = new byte[TAMANHO_MAXIMO];
        while (conectado.get()) {
            DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(pacote);
                String mensagem = new String(pacote.getData(), pacote.getOffset(), pacote.getLength(),
                        StandardCharsets.UTF_8);
                System.out.println();
                System.out.println(mensagem);
                System.out.println("[MULTICAST] Datagram recebido de "
                        + pacote.getAddress().getHostAddress() + ":" + pacote.getPort());
                System.out.print("> ");
            } catch (IOException e) {
                if (conectado.get()) {
                    // A falha de recepcao aparece como IOException e fica limitada ao receptor
                    // multicast, sem desaparecer silenciosamente.
                    System.err.println("Erro ao receber mensagem multicast: " + e.getMessage());
                }
                return;
            }
        }
    }
}
