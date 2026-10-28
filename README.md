# Trabalho Prático de Sistemas Distribuídos — Sockets

Implementação didática dos exercícios de sockets para a disciplina de Sistemas
Distribuídos da UTFPR, ministrada pelo Prof. Fernando Schütz. O projeto
demonstra comunicação TCP e UDP, atendimento concorrente de clientes,
sincronização de estado compartilhado e consolidação distribuída de resultados.

## Requisitos

- JDK 8 ou superior.
- Para o chat multicast, uma rede que permita tráfego UDP multicast entre os
  participantes. O firewall pode precisar liberar a porta UDP `4446`.

## Estrutura numerada dos exercícios

`src/Main.java` apresenta um menu interativo com as opções de 1 a 7 e a opção 0
para sair. Cada seleção executa o módulo correspondente e retorna ao menu ao
terminar.

| Opção | Pacote | Descrição |
|---:|---|---|
| 1 | `Exercicio01_ServidorFortunes` | Servidor TCP concorrente com `GET-FORTUNE`, `ADD-FORTUNE <frase>`, `UPD-FORTUNE <pos> <frase>`, `LST-FORTUNE` e `QUIT`. As posições da lista começam em 1. |
| 2 | `Exercicio02_ProcessadorInteiros` | Recebe inteiros em linhas TCP até EOF e calcula quantidade, soma, média, máximo e mínimo. |
| 3 | `Exercicio03_JogoDaForca` | Jogo TCP remoto para um jogador. A palavra é mantida no servidor, que envia o tamanho, as tentativas restantes, o estado e o boneco após cada palpite. |
| 4 | `Exercicio04_BancoConcorrente` | Servidor TCP concorrente para depósito, saque, consulta de saldo e saída. Operações da conta são sincronizadas; valores usam `BigDecimal`. |
| 5 | `Exercicio05_LojasDepartamento` | Simula cinco filiais concorrentes, cada uma com 1500 compras/vendas e atraso curto por operação. As filiais enviam seus totais por TCP ao servidor central, que consolida os resultados. |
| 6 | `Exercicio06_ServidorArquivos` | Demonstra upload e download TCP com controle de acesso por arquivo: lock exclusivo para gravação e compartilhado para leitura. |
| 7 | `Exercicio07_ChatMulticast` | Chat em tempo real por UDP multicast no grupo `230.0.0.1`, porta `4446`. Digite `/sair` para voltar ao menu. |

As opções 1 a 4 iniciam um servidor TCP local e permitem interagir com ele pelo
cliente incluído. A opção 5 executa a simulação de filiais; a opção 6 realiza
um ciclo demonstrativo de upload/download em diretório temporário. A opção 7
fica ativa até o usuário digitar `/sair`.

## Compilar e executar pelo terminal

Na raiz do repositório, crie o diretório de saída e compile todos os fontes:

```text
mkdir out
javac -encoding UTF-8 -d out src\Main.java src\Exercicio01_ServidorFortunes\ServidorFortunes.java src\Exercicio02_ProcessadorInteiros\ProcessadorInteiros.java src\Exercicio03_JogoDaForca\JogoDaForca.java src\Exercicio04_BancoConcorrente\BancoConcorrente.java src\Exercicio05_LojasDepartamento\LojasDepartamento.java src\Exercicio06_ServidorArquivos\ServidorArquivos.java src\Exercicio07_ChatMulticast\ChatMulticast.java
```

Inicie o menu:

```text
java -cp out Main
```

Os comandos acima usam o separador de diretórios do Windows. Em macOS/Linux,
substitua `\` por `/` no comando `javac`.

## Executar em uma IDE

1. Abra a pasta do repositório no IntelliJ IDEA, Eclipse ou VS Code com suporte
   a Java.
2. Configure um JDK 8 ou superior para o projeto.
3. Marque `src` como diretório de fontes, se a IDE não fizer isso
   automaticamente.
4. Execute a classe `Main` e use o menu no terminal/console integrado.

## Observações sobre os protocolos

- Os protocolos TCP textuais usam UTF-8 e delimitam comandos por quebra de
  linha. O processador de inteiros usa o fechamento da saída do cliente (EOF)
  como fim da sequência.
- O servidor de arquivos usa `DataInputStream`/`DataOutputStream`: comandos e
  nomes são `writeUTF`, upload inclui tamanho em bytes (`long`) e o conteúdo
  binário segue em seguida. O limite de upload é 50 MiB e os nomes aceitos são
  simples, sem caminhos de diretório.
- Os exemplos de servidor do menu escolhem uma porta TCP disponível e
  demonstram a comunicação em `localhost`. Para uso em rede, instancie o
  servidor com a porta desejada e configure firewall e endereço de interface
  conforme o ambiente.
