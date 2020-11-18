/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cliente.gordo;

import com.github.jaiimageio.impl.plugins.tiff.TIFFImageWriterSpi;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import removeme.ImageConverter;

/**
 *
 * @author Djani
 */
//criando classe cliente gordo
public class ClienteGordo {

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
    public ClienteGordo() {
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
        ClienteGordo clienteGordo = new ClienteGordo();
        clienteGordo.conectar();
        Thread escuta = new Thread(() -> {
            clienteGordo.receberResposta();

        });
        
        escuta.start();

        File arquivos[];
        File diretorio = new File("image/");
        arquivos = diretorio.listFiles();

        for (int i = 0; i < arquivos.length; i++) {
            caminho = new File(arquivos[i].getPath());
            if (verificaImg(caminho) == true) {
                System.out.println("Imagem atual: " + caminho.getName());
                clienteGordo.enviarMensagem(caminho.toString());
            }
        }
        Scanner in = new Scanner(System.in);
        System.out.print("Processo finalizado:  ");
        String mensagem = in.nextLine();
        clienteGordo.enviarMensagem("sair");
        escuta.join();
        //clienteGordo.sair();        
        System.out.println(escuta.isAlive());
    }

    public void enviarMensagem(String msg) {

        try {
            //escrever dados no servidor
            if(msg.equalsIgnoreCase("Sair")){
                msg1 = "sair";
            }
            bfw.write(msg + System.lineSeparator());
            bfw.flush();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("\n Mensagem enviada");

    }
//aqui recebe as informações do servidor
    public void receberResposta() {
        InputStream in;
        try {
            in = socket.getInputStream();
            InputStreamReader inr = new InputStreamReader(in);

            //objeto para leitura e escrita de dados no cliente
            BufferedReader bfr = new BufferedReader(inr);
            String msg = "";
            while (!msg1.equalsIgnoreCase("Sair")) {
//garante que o bfr tenha sempre algo a ler quando invocado
                if (bfr.ready()) {
                    msg = bfr.readLine();
                    System.out.println(msg);
                    //garante a leitura até que o servidor me informe que o processo foi finalizado
                    if (!msg.equalsIgnoreCase("finalizado")) {
                        double resultado = Double.parseDouble(msg);
                        System.out.printf("\nSIMILARIDADE DO COSSENO: %.2f%%\n", (resultado));
                    } else {
                        sair();
                    }
                }sair();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean verificaImg(File img) {
        try {
            //filtro para imagem
            if (!img.toString().contains("comparable")) {
                if (!img.toString().contains("tiff")) {
                    BufferedImage image = ImageIO.read(img);
                    int largura = image.getWidth();
                    int altura = image.getHeight();

                    if (largura == altura) {
                        if (img.getPath().contains(".jpg")) {
                            caminho = new File(convertTiffandCrop(img.getPath()));
                            System.out.println("A imagem:" + img.getName() + " já é quadrada!");
                        }
                        return true;

                    } else {
                        caminho = new File(convertTiffandCrop(img.getPath()));
                        return true;
                    }
                } else{
                    caminho = new File(img.getPath());
                    return true;
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(ClienteGordo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;

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

    public static String convertTiffandCrop(String path) {

        BufferedImage image, outImage;
        String output = "";
        try {

            image = ImageIO.read(new File(path));

            //se a largura for maior que a altura, corta o quadrado com base na altura
            if (image.getHeight() < image.getWidth()) {
                outImage = ImageConverter.cropHorizontal(image, image.getHeight());
                System.out.println("A imagem: " + image.toString().toUpperCase() + "Foi cortada com base na largura!");
                //se a altura for maior que a largura, corta o quadrado com base na largura
            } else if (image.getHeight() > image.getWidth()) {
                System.out.println("A imagem: " + image.toString().toUpperCase() + "Foi cortada com base na altura!");
                outImage = ImageConverter.cropVertical(image, image.getWidth());
            } else {
                // se a imagem for quadrada, apenas converte em formato Tif
                outImage = image;
            }

            TIFFImageWriterSpi tiffspi = new TIFFImageWriterSpi();
            ImageWriter writer = tiffspi.createWriterInstance();

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

            /* Caso queira colocar em formato TIFF com modo de compressao
             param.setCompressionType("JPEG");
             param.setCompressionQuality(1);
             */
            output = path.replaceAll("jpg", "tiff");

            File fOutputFile = new File(output);
            ImageOutputStream ios = ImageIO.createImageOutputStream(fOutputFile);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(outImage, null, null), param);

            System.out.println("Convertido!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

}