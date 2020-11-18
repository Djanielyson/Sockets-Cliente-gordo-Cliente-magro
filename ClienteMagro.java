/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientemagro;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.Scanner;


/**
 *
 * @author Djani
 */
//criando classe cliente gordo
public class ClienteMagro {

    private static final long serialVersionUID = 1L;

    private static Socket socket;
    private static OutputStream outputStream;
    private static BufferedImage newImg;
    private static File caminho;
    private String nome;
    private Writer ouw;
    private BufferedWriter bfw;
    private static String msg1 = "";
//contrutor do cliente gordo

    public ClienteMagro() {
        Scanner in = new Scanner(System.in);
        System.out.print("Diga seu nome: ");
        this.nome = in.nextLine();
    }
//metodo que garante a conexão do cliente atraves do socket

    public void conectar() throws IOException {

        socket = new Socket("localhost", 13085);
        outputStream = socket.getOutputStream();
        ouw = new OutputStreamWriter(outputStream);
        bfw = new BufferedWriter(ouw);

        bfw.write(this.nome + "\r\n");
        bfw.flush();

    }

    public static void main(String[] args) throws Exception {
        ClienteMagro clienteGordo = new ClienteMagro();
        clienteGordo.conectar();
        Thread escuta = new Thread(() -> {
            clienteGordo.receberResposta();

        });

        escuta.start();

        File arquivos[];
        File diretorio = new File("image/");
        arquivos = diretorio.listFiles();
        for (int i = 1; i < arquivos.length; i++) {
            caminho = new File(arquivos[i].getPath());
            System.out.println("Imagem atual: " + caminho.getPath());
            clienteGordo.enviarMensagem(caminho.toString());

        }
        clienteGordo.enviarMensagem("sair");
        Scanner in = new Scanner(System.in);
        String mensagem = in.nextLine();
        System.out.print("Processo finalizado:  ");
        //clienteGordo.sair(); 

        System.out.println(escuta.isAlive());

    }

    public void enviarMensagem(String msg) {

        try {
            //escrever dados no servidor
            if (msg.equalsIgnoreCase("Sair")) {
                msg1 = "sair";
            }
            bfw.write(msg + System.lineSeparator());
            bfw.flush();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
//aqui recebe as informações do servidor

    public void receberResposta() {
        InputStream in;
        try {
            in = socket.getInputStream();
            InputStreamReader inr = new InputStreamReader(in);

            //objeto para leitura
            BufferedReader bfr = new BufferedReader(inr);
            String msg = "";
            while (!msg.equalsIgnoreCase("Finalizado")) {
//garante que o bfr tenha sempre algo a ler quando invocado
                if (bfr.ready()) {
                    msg = bfr.readLine();
                    //garante a leitura até que o servidor me informe que o processo foi finalizado
                    if (!msg.equalsIgnoreCase("finalizado")) {
                        double resultado = Double.parseDouble(msg);
                        System.out.printf("\nSIMILARIDADE DO COSSENO: %.2f%%\n", (resultado));

                    } else {
                        System.out.println("Processo Finaliado");
                        sair();

                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sair() {

        try {
            bfw.close();
            ouw.close();
            outputStream.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
