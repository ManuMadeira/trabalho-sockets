package Exercicio04_BancoConcorrente;

import java.math.BigDecimal;

/**
 * Aqui eu represento uma conta bancaria mantida pelo servidor.
 * Os metodos sincronizados garantem que depositos, saques e consultas
 * desta conta nao se sobreponham de forma inconsistente.
 */
public final class Conta {
    private final String numero;
    private BigDecimal saldo;

    // Aqui eu crio a conta com um numero identificador e um saldo inicial.
    public Conta(String numero, String saldoInicial) {
        this.numero = numero;
        this.saldo = new BigDecimal(saldoInicial);
    }

    // Nesta operacao synchronized eu deposito o valor usando BigDecimal,
    // que evita erros de precisao em valores monetarios.
    public synchronized BigDecimal depositar(BigDecimal valor) {
        saldo = saldo.add(valor);
        return saldo;
    }

    // Aqui eu realizo o saque dentro da mesma exclusao synchronized:
    // primeiro verifico o saldo e somente depois subtraio o valor.
    public synchronized BigDecimal sacar(BigDecimal valor) {
        if (valor.compareTo(saldo) > 0) {
            return null;
        }
        saldo = saldo.subtract(valor);
        return saldo;
    }

    // Eu sincronizo a leitura para que ela tenha acesso ao mesmo estado
    // protegido das operacoes de alteracao da conta.
    public synchronized BigDecimal consultarSaldo() {
        return saldo;
    }

    // Aqui eu realizo todo o historico do deposito dentro de uma unica secao
    // sincronizada, para que o log da operacao corresponda ao mesmo estado protegido.
    public synchronized BigDecimal[] depositarComHistorico(BigDecimal valor) {
        BigDecimal saldoAnterior = saldo;
        BigDecimal novoSaldo = saldoAnterior.add(valor);
        saldo = novoSaldo;
        return new BigDecimal[]{saldoAnterior, novoSaldo};
    }

    // Aqui eu verifico e altero o saque dentro da mesma secao sincronizada.
    // Se nao houver saldo suficiente, eu retorno null sem modificar o estado.
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
