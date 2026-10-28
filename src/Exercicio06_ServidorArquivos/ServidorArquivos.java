package Exercicio06_ServidorArquivos;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class ServidorArquivos implements AutoCloseable {
    private static final long LIMITE_ARQUIVO = 50L * 1024L * 1024L;
    private final ServerSocket serverSocket;
    private final Path diretorio;
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> bloqueios =
            new ConcurrentHashMap<String, ReentrantReadWriteLock>();
    private final ExecutorService clientes = Executors.newCachedThreadPool();
    private volatile boolean executando = true;
    private final Thread acceptor;

    private ServidorArquivos(Path diretorio) throws IOException {
        this.diretorio = diretorio;
        Files.createDirectories(diretorio);
        serverSocket = new ServerSocket(0);
        acceptor = new Thread(this::aceitarClientes, "file-server-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    private void aceitarClientes() {
        while (executando) {
            try {
                Socket socket = serverSocket.accept();
                clientes.execute(() -> atender(socket));
            } catch (IOException e) {
                if (executando) {
                    System.err.println("Falha ao aceitar cliente do servidor de arquivos: " + e.getMessage());
                }
            }
        }
    }

    private void atender(Socket socket) {
        try (Socket cliente = socket;
             DataInputStream entrada = new DataInputStream(cliente.getInputStream());
             DataOutputStream saida = new DataOutputStream(cliente.getOutputStream())) {
            String operacao = entrada.readUTF();
            String nome = entrada.readUTF();
            if (!nome.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) {
                saida.writeUTF("ERRO: nome de arquivo inválido.");
                return;
            }
            ReentrantReadWriteLock bloqueio = bloqueios.computeIfAbsent(nome,
                    chave -> new ReentrantReadWriteLock());
            if ("UPLOAD".equalsIgnoreCase(operacao)) {
                receberArquivo(nome, bloqueio, entrada, saida);
            } else if ("DOWNLOAD".equalsIgnoreCase(operacao)) {
                enviarArquivo(nome, bloqueio, saida);
            } else {
                saida.writeUTF("ERRO: operação desconhecida.");
            }
        } catch (IOException e) {
            System.err.println("Erro na operação de arquivo: " + e.getMessage());
        }
    }

    private void receberArquivo(String nome, ReentrantReadWriteLock bloqueio,
                                DataInputStream entrada, DataOutputStream saida) throws IOException {
        long tamanho = entrada.readLong();
        if (tamanho < 0 || tamanho > LIMITE_ARQUIVO) {
            saida.writeUTF("ERRO: tamanho inválido (limite de 50 MiB).");
            return;
        }
        bloqueio.writeLock().lock();
        Path temporario = null;
        try {
            temporario = Files.createTempFile(diretorio, "upload-", ".tmp");
            try (java.io.OutputStream arquivo = Files.newOutputStream(temporario)) {
                byte[] buffer = new byte[8192];
                long restante = tamanho;
                while (restante > 0) {
                    int lido = entrada.read(buffer, 0, (int) Math.min(buffer.length, restante));
                    if (lido < 0) {
                        throw new IOException("Upload incompleto: conexão encerrada antes do fim do arquivo.");
                    }
                    arquivo.write(buffer, 0, lido);
                    restante -= lido;
                }
            }
            try {
                Files.move(temporario, diretorio.resolve(nome), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporario, diretorio.resolve(nome), StandardCopyOption.REPLACE_EXISTING);
            }
            saida.writeUTF("UPLOAD OK: " + tamanho + " bytes.");
        } finally {
            try {
                if (temporario != null) {
                    Files.deleteIfExists(temporario);
                }
            } finally {
                bloqueio.writeLock().unlock();
            }
        }
    }

    private void enviarArquivo(String nome, ReentrantReadWriteLock bloqueio,
                               DataOutputStream saida) throws IOException {
        bloqueio.readLock().lock();
        try {
            Path arquivo = diretorio.resolve(nome);
            if (!Files.isRegularFile(arquivo)) {
                saida.writeLong(-1L);
                saida.writeUTF("ERRO: arquivo não encontrado.");
                return;
            }
            long tamanho = Files.size(arquivo);
            saida.writeLong(tamanho);
            saida.writeUTF("DOWNLOAD OK");
            try (java.io.InputStream conteudo = Files.newInputStream(arquivo)) {
                byte[] buffer = new byte[8192];
                int lido;
                while ((lido = conteudo.read(buffer)) != -1) {
                    saida.write(buffer, 0, lido);
                }
            }
        } finally {
            bloqueio.readLock().unlock();
        }
    }

    private int getPorta() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void close() throws IOException {
        executando = false;
        serverSocket.close();
        clientes.shutdownNow();
    }

    public static void executar() throws IOException {
        Path diretorioTemporario = Files.createTempDirectory("servidor-arquivos-");
        try (ServidorArquivos servidor = new ServidorArquivos(diretorioTemporario)) {
            byte[] original = "Conteúdo de demonstração do servidor corporativo.".getBytes(
                    java.nio.charset.StandardCharsets.UTF_8);
            fazerUpload(servidor.getPorta(), "relatorio.txt", original);
            byte[] recebido = fazerDownload(servidor.getPorta(), "relatorio.txt");
            System.out.println("Servidor de arquivos ativo na porta " + servidor.getPorta() + ".");
            System.out.println("Upload e download concluídos; conteúdo: "
                    + new String(recebido, java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("Upload usa bloqueio exclusivo e download usa bloqueio compartilhado por arquivo.");
        } finally {
            Files.deleteIfExists(diretorioTemporario.resolve("relatorio.txt"));
            Files.deleteIfExists(diretorioTemporario);
        }
    }

    private static void fazerUpload(int porta, String nome, byte[] dados) throws IOException {
        try (Socket cliente = new Socket("127.0.0.1", porta);
             DataOutputStream saida = new DataOutputStream(cliente.getOutputStream());
             DataInputStream entrada = new DataInputStream(cliente.getInputStream())) {
            saida.writeUTF("UPLOAD");
            saida.writeUTF(nome);
            saida.writeLong(dados.length);
            saida.write(dados);
            saida.flush();
            validarResposta(entrada.readUTF());
        }
    }

    private static byte[] fazerDownload(int porta, String nome) throws IOException {
        try (Socket cliente = new Socket("127.0.0.1", porta);
             DataOutputStream saida = new DataOutputStream(cliente.getOutputStream());
             DataInputStream entrada = new DataInputStream(cliente.getInputStream())) {
            saida.writeUTF("DOWNLOAD");
            saida.writeUTF(nome);
            saida.flush();
            long tamanho = entrada.readLong();
            String resposta = entrada.readUTF();
            validarResposta(resposta);
            if (tamanho < 0 || tamanho > LIMITE_ARQUIVO) {
                throw new IOException("Tamanho de download inválido: " + tamanho);
            }
            byte[] dados = new byte[(int) tamanho];
            entrada.readFully(dados);
            return dados;
        }
    }

    private static void validarResposta(String resposta) throws IOException {
        if (resposta.startsWith("ERRO:")) {
            throw new IOException(resposta);
        }
        System.out.println(resposta);
    }
}
