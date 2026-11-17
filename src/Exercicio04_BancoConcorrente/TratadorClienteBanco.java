package Exercicio04_BancoConcorrente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Aqui eu represento a thread dedicada a um unico cliente bancario.
 * O servidor cria uma instancia desta classe para cada Socket aceito,
 * mantendo atendimento independente e concorrente.
 */
public final class TratadorClienteBanco implements Runnable {
    private final Socket cliente;
    private final int terminalId;
    private final Map<String, Conta> contas;

    public TratadorClienteBanco(Socket cliente, int terminalId, Map<String, Conta> contas) {
        this.cliente = cliente;
        this.terminalId = terminalId;
        this.contas = contas;
    }

    @Override
    public void run() {
        // Aqui eu transformo os streams de entrada e saida em texto UTF-8,
        // criando o protocolo linha a linha usado pelo cliente e pelo servidor.
        try (Socket conexao = cliente;
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     conexao.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter saida = new PrintWriter(new OutputStreamWriter(
                     conexao.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String endereco = conexao.getInetAddress().getHostAddress();
            int portaRemota = conexao.getPort();
            System.out.printf("[CONEXAO] Terminal %d conectado: IP %s / Porta remota %d%n",
                    terminalId, endereco, portaRemota);
            saida.println("BEM-VINDO|Terminal " + terminalId
                    + "|Comandos: DEPOSITO|conta|valor, SAQUE|conta|valor, SALDO|conta, SAIR");

            String linha;
            while ((linha = entrada.readLine()) != null) {
                String comando = linha.trim();
                if (comando.isEmpty()) {
                    continue;
                }

                String[] partes = comando.split("\\|");
                String operacao = partes[0].toUpperCase();
                System.out.printf("[OPERACAO] Terminal %d enviou o comando \"%s\"%n",
                        terminalId, comando);

                if ("SAIR".equals(operacao)) {
                    System.out.printf("[DESCONECTADO] Terminal %d encerrou a sessao.%n", terminalId);
                    saida.println("OK|SAIR|Conexao encerrada.");
                    break;
                }

                if ("SALDO".equals(operacao)) {
                    processarConsulta(partes, saida);
                } else if ("DEPOSITO".equals(operacao)) {
                    processarDeposito(partes, saida);
                } else if ("SAQUE".equals(operacao)) {
                    processarSaque(partes, saida);
                } else {
                    System.out.printf("[ERRO] Terminal %d enviou operacao desconhecida.%n", terminalId);
                    saida.println("ERRO|Operacao desconhecida.");
                }
            }
        } catch (IOException e) {
            // Aqui eu trato falhas de rede sem derrubar o restante das conexoes.
            System.err.printf("[ERRO] Conexao do Terminal %d encerrada com IOException: %s%n",
                    terminalId, e.getMessage());
        } finally {
            System.out.printf("[DESCONECTADO] Recursos do Terminal %d foram fechados.%n", terminalId);
        }
    }

    private void processarConsulta(String[] partes, PrintWriter saida) {
        if (partes.length != 2) {
            saida.println("ERRO|Formato esperado: SALDO|conta");
            return;
        }
        String numeroConta = partes[1].trim();
        Conta conta = contas.get(numeroConta);
        if (conta == null) {
            System.out.printf("[ERRO] Terminal %d consultou conta inexistente %s.%n",
                    terminalId, numeroConta);
            saida.println("ERRO|Conta " + numeroConta + " nao encontrada.");
            return;
        }
        BigDecimal saldo = conta.consultarSaldo();
        System.out.printf("[CONSULTA] Terminal %d consultou a Conta %s. Saldo: R$ %s%n",
                terminalId, numeroConta, formatar(saldo));
        saida.println("OK|SALDO|" + numeroConta + "|" + formatar(saldo));
    }

    private void processarDeposito(String[] partes, PrintWriter saida) {
        if (partes.length != 3) {
            saida.println("ERRO|Formato esperado: DEPOSITO|conta|valor");
            return;
        }
        String numeroConta = partes[1].trim();
        Conta conta = contas.get(numeroConta);
        if (conta == null) {
            saida.println("ERRO|Conta " + numeroConta + " nao encontrada.");
            return;
        }
        try {
            BigDecimal valor = new BigDecimal(partes[2].trim().replace(',', '.'));
            if (valor.compareTo(BigDecimal.ZERO) <= 0) {
                saida.println("ERRO|O valor do deposito deve ser positivo.");
                return;
            }

            BigDecimal[] historico = conta.depositarComHistorico(valor);
            BigDecimal saldoAnterior = historico[0];
            BigDecimal novoSaldo = historico[1];
            System.out.printf("[OPERACAO] Terminal %d solicitou DEPOSITO de R$ %s na Conta %s.%n",
                    terminalId, formatar(valor), numeroConta);
            System.out.printf("[ATUALIZACAO] Saldo da Conta %s alterado de R$ %s para R$ %s.%n",
                    numeroConta, formatar(saldoAnterior), formatar(novoSaldo));
            saida.println("OK|DEPOSITO|" + numeroConta + "|" + formatar(novoSaldo));
        } catch (NumberFormatException e) {
            System.out.printf("[ERRO] Terminal %d enviou valor de deposito invalido.%n", terminalId);
            saida.println("ERRO|Valor monetario invalido.");
        }
    }

    private void processarSaque(String[] partes, PrintWriter saida) {
        if (partes.length != 3) {
            saida.println("ERRO|Formato esperado: SAQUE|conta|valor");
            return;
        }
        String numeroConta = partes[1].trim();
        Conta conta = contas.get(numeroConta);
        if (conta == null) {
            saida.println("ERRO|Conta " + numeroConta + " nao encontrada.");
            return;
        }
        try {
            BigDecimal valor = new BigDecimal(partes[2].trim().replace(',', '.'));
            if (valor.compareTo(BigDecimal.ZERO) <= 0) {
                saida.println("ERRO|O valor do saque deve ser positivo.");
                return;
            }

            System.out.printf("[OPERACAO] Terminal %d solicitou SAQUE de R$ %s na Conta %s.%n",
                    terminalId, formatar(valor), numeroConta);
            BigDecimal[] historico = conta.sacarComHistorico(valor);
            if (historico == null) {
                BigDecimal saldoAnterior = conta.consultarSaldo();
                System.out.printf("[ERRO] Saque recusado na Conta %s: saldo insuficiente (R$ %s).%n",
                        numeroConta, formatar(saldoAnterior));
                saida.println("ERRO|Saldo insuficiente|SALDO|" + formatar(saldoAnterior));
                return;
            }
            BigDecimal saldoAnterior = historico[0];
            BigDecimal novoSaldo = historico[1];
            System.out.printf("[ATUALIZACAO] Saldo da Conta %s alterado de R$ %s para R$ %s.%n",
                    numeroConta, formatar(saldoAnterior), formatar(novoSaldo));
            saida.println("OK|SAQUE|" + numeroConta + "|" + formatar(novoSaldo));
        } catch (NumberFormatException e) {
            System.out.printf("[ERRO] Terminal %d enviou valor de saque invalido.%n", terminalId);
            saida.println("ERRO|Valor monetario invalido.");
        }
    }

    private static String formatar(BigDecimal valor) {
        return valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
