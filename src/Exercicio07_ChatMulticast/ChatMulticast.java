package Exercicio07_ChatMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChatMulticast {
    private static final String GRUPO = "230.0.0.1";
    private static final int PORTA = 4446;
    private static final int TAMANHO_MAXIMO = 8192;

    private ChatMulticast() {
    }

    public static void executar(Scanner scanner) throws IOException {
        InetAddress grupo = InetAddress.getByName(GRUPO);
        AtomicBoolean conectado = new AtomicBoolean(true);
        try (MulticastSocket socket = new MulticastSocket(PORTA)) {
            socket.joinGroup(grupo);
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
                socket.send(new DatagramPacket(dados, dados.length, grupo, PORTA));
            }
            conectado.set(false);
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
                System.out.print("> ");
            } catch (IOException e) {
                if (conectado.get()) {
                    System.err.println("Erro ao receber mensagem multicast: " + e.getMessage());
                }
                return;
            }
        }
    }
}
