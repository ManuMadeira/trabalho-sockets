import Exercicio01_ServidorFortunes.ServidorFortunes;
import Exercicio02_ProcessadorInteiros.ProcessadorInteiros;
import Exercicio03_JogoDaForca.JogoDaForca;
import Exercicio04_BancoConcorrente.BancoConcorrente;
import Exercicio05_LojasDepartamento.LojasDepartamento;
import Exercicio06_ServidorArquivos.ServidorArquivos;
import Exercicio07_ChatMulticast.ChatMulticast;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean executando = true;

        while (executando) {
            exibirMenu();
            System.out.print("Escolha uma opção: ");
            String opcao = scanner.nextLine().trim();

            try {
                switch (opcao) {
                    case "1":
                        ServidorFortunes.executar(scanner);
                        break;
                    case "2":
                        ProcessadorInteiros.executar(scanner);
                        break;
                    case "3":
                        JogoDaForca.executar(scanner);
                        break;
                    case "4":
                        BancoConcorrente.executar(scanner);
                        break;
                    case "5":
                        LojasDepartamento.executar();
                        break;
                    case "6":
                        ServidorArquivos.executar();
                        break;
                    case "7":
                        ChatMulticast.executar(scanner);
                        break;
                    case "0":
                        executando = false;
                        System.out.println("Programa encerrado.");
                        break;
                    default:
                        System.out.println("Opção inválida. Digite um número de 0 a 7.");
                }
            } catch (IOException e) {
                System.err.println("Erro de I/O ao executar o exercício: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Execução interrompida.");
            } catch (ExecutionException e) {
                System.err.println("Erro em uma tarefa concorrente: " + e.getCause().getMessage());
            } catch (NoSuchElementException e) {
                System.out.println("Entrada encerrada; retornando ao menu principal.");
                executando = false;
            }

            if (executando && !"0".equals(opcao)) {
                System.out.println();
                System.out.println("Pressione Enter para voltar ao menu principal.");
                scanner.nextLine();
            }
        }
    }

    private static void exibirMenu() {
        System.out.println("============================================");
        System.out.println(" Sistemas Distribuídos - Exercícios Socket");
        System.out.println("============================================");
        System.out.println("[1] Servidor de fortunes (Exercício 01)");
        System.out.println("[2] Processador de inteiros (Exercício 02)");
        System.out.println("[3] Jogo da Forca (Exercício 03)");
        System.out.println("[4] Banco concorrente (Exercício 04)");
        System.out.println("[5] Lojas de departamento (Exercício 05)");
        System.out.println("[6] Servidor de arquivos (Exercício 06)");
        System.out.println("[7] Chat multicast (Exercício 07)");
        System.out.println("[0] Sair");
    }
}
