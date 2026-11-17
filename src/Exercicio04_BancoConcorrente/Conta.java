package Exercicio04_BancoConcorrente;

import java.math.BigDecimal;

/**
 * Representa uma conta bancaria mantida pelo servidor.
 * Os metodos sincronizados garantem que depositos, saques e consultas
 * desta conta nao se sobreponham de forma inconsistente.
 */
public final class Conta {
    private final String numero;
    private BigDecimal saldo;

    // Inicializa a conta com um numero identificador e um saldo inicial.
    public Conta(String numero, String saldoInicial) {
        this.numero = numero;
        this.saldo = new BigDecimal(saldoInicial);
    }

    // Esta operacao synchronized deposita o valor usando BigDecimal,
    // que evita erros de precisao em valores monetarios.
    public synchronized BigDecimal depositar(BigDecimal valor) {
        saldo = saldo.add(valor);
        return saldo;
    }

    // O saque ocorre dentro da mesma exclusao synchronized:
    // primeiro o saldo e verificado e somente depois o valor e subtraido.
    public synchronized BigDecimal sacar(BigDecimal valor) {
        if (valor.compareTo(saldo) > 0) {
            return null;
        }
        saldo = saldo.subtract(valor);
        return saldo;
    }

    // A leitura sincronizada acessa o mesmo estado
    // protegido das operacoes de alteracao da conta.
    public synchronized BigDecimal consultarSaldo() {
        return saldo;
    }

    // O deposito e o registro dos saldos anterior e atualizado ocorrem numa unica
    // secao sincronizada, mantendo o log correspondente ao estado protegido.
    public synchronized BigDecimal[] depositarComHistorico(BigDecimal valor) {
        BigDecimal saldoAnterior = saldo;
        BigDecimal novoSaldo = saldoAnterior.add(valor);
        saldo = novoSaldo;
        return new BigDecimal[]{saldoAnterior, novoSaldo};
    }

    // A verificacao e a alteracao do saque ocorrem na mesma secao sincronizada.
    // Sem saldo suficiente, o metodo retorna null sem modificar o estado.
    public synchronized BigDecimal[] sacarComHistorico(BigDecimal valor) {
        BigDecimal saldoAnterior = saldo;
        if (valor.compareTo(saldoAnterior) > 0) {
            return null;
        }
        BigDecimal novoSaldo = saldoAnterior.subtract(valor);
        saldo = novoSaldo;
        return new BigDecimal[]{saldoAnterior, novoSaldo};
    }

    public String getNumero() {
        return numero;
    }
}
