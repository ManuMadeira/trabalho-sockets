# Chat UDP Multicast

## Visao Geral

Esta atividade implementa um participante de chat usando UDP multicast. Todos
os processos que entram no mesmo grupo podem receber os datagramas enviados
pelos demais participantes, sem manter conexões persistentes.

## Tecnologias e Ferramentas

- Java SE e JDK 8 ou superior
- `MulticastSocket`
- `DatagramPacket`
- UDP multicast
- UTF-8
- `AtomicBoolean`
- Threads Java

## Como foi Solucionado (Resumo Tecnico)

`executar` resolve o endereço `230.0.0.1` e cria um `MulticastSocket` na porta
`4446`. `joinGroup` faz o participante entrar no grupo. Uma thread executa
`receberMensagens`, recebendo continuamente os pacotes pelo método `receive`.

O usuário informa um nome e digita mensagens. `executar` verifica o limite de
8192 bytes, serializa `nome: mensagem` em UTF-8 e chama `send` com um
`DatagramPacket` destinado ao grupo e à porta.

O comando `/sair` encerra o envio, altera `conectado` para `false` e executa
`leaveGroup` para remover o participante do grupo.

## Diagrama da Solucao (com Funcoes)

```mermaid
flowchart TD
    A[ChatMulticast.executar] --> B[Resolve grupo 230.0.0.1]
    B --> C[Abre MulticastSocket porta 4446]
    C --> D[joinGroup]
    D --> E[Inicia receberMensagens]
    D --> F[Solicita nome]
    F --> G[Le mensagem do usuario]
    G --> H{Comando sair}
    H -->|Sim| I[conectado false]
    I --> J[leaveGroup]
    H -->|Nao| K[Serializa nome e mensagem]
    K --> L{Tamanho menor que 8192}
    L -->|Nao| G
    L -->|Sim| M[Cria DatagramPacket]
    M --> N[send para grupo]
    E --> O[receive pacote]
    O --> P[Decodifica UTF-8]
    P --> Q[Exibe mensagem]
    Q --> E
```

## Como Executar

Na raiz do repositório:

```powershell
javac -encoding UTF-8 -d out src\Exercicio07_ChatMulticast\ChatMulticast.java
java -cp out Exercicio07_ChatMulticast.ChatMulticast
```

Como `ChatMulticast` não possui um método `main`, execute-o pelo menu principal:

```powershell
javac -encoding UTF-8 -d out src\Main.java src\Exercicio07_ChatMulticast\ChatMulticast.java
java -cp out Main
```

Escolha a opção `7`. Para testar a comunicação entre processos, execute o
programa em dois ou mais terminais na mesma rede que permita o tráfego UDP
multicast. Digite `/sair` para voltar ao menu.
