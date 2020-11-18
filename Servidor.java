
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientemagro;

import com.github.jaiimageio.impl.plugins.tiff.TIFFImageWriterSpi;
import imagemapping.Image;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
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
public class Servidor extends Thread {

    private static ServerSocket server;
    private static ArrayList<BufferedWriter> clientes;
    private static double similarityCosine;
    private Socket socket;
    private static Socket con;
    private InputStream in;
    private InputStreamReader inr;
    private BufferedReader bfr;
    private static int contador;
    private static boolean status;
    private String nome;
    private static File caminho2;
//construtor
    public Servidor(Socket con) {
        this.socket = con;
        try {
            in = con.getInputStream();
            inr = new InputStreamReader(in);
            bfr = new BufferedReader(inr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {

        server = new ServerSocket(13085);
        System.out.println("Servidor esperando o envio da imagem");
        clientes = new ArrayList<BufferedWriter>();

        while (true) {

            System.out.println("Aguardando conexão!");
            con = server.accept();
            
            status = true;
            System.out.println("Cliente conectando!");
            Thread s = new Servidor(con);
            s.start();
            System.out.println(con.isClosed());

        }
    }
//envio os dados para o cliente
    public void enviarAll() throws IOException {
        OutputStream out = this.socket.getOutputStream();       
        Writer ouw = new OutputStreamWriter(out);
        BufferedWriter bfw = new BufferedWriter(ouw);

        String resposta = Double.toString(similarityCosine);
        bfw.write(resposta + System.lineSeparator());

        bfw.flush();
        if (status == false) {
            resposta = "Finalizado";
            status = true;
            bfw.write(resposta + System.lineSeparator());
            

            bfw.flush();
        }
    }
//onde a magica acontece
    @Override
    public void run() {
        try {
            String caminho = "";
            OutputStream out = this.socket.getOutputStream();//enviar dados para o cliente (writer)
            Writer ouw = new OutputStreamWriter(out);
            BufferedWriter bfw = new BufferedWriter(ouw);
            nome =  bfr.readLine();//recebe dados do cliente (reader)
            System.out.println("\n" + nome + " utilizando o servidor!");


            while (true) {
                if (bfr.ready()) {
                    caminho = bfr.readLine();
                    if (!caminho.equalsIgnoreCase("Sair")) {
     
                        caminho2 = new File(caminho);
                        
                        //filtro para null
                        if (verificaImg(caminho2) == true) {
                            String filename01 = caminho2.getPath();
                            String filename02 = "image/comparable.tif";
                            Image image01 = null;
                            Image image02 = null;

                            image01 = new Image(Image.componentExtract(filename01), 255);
                            image02 = new Image(Image.componentExtract(filename02), 255);
                            similarityCosine = image01.compareCosineSimilarity(image02);
                            similarityCosine = similarityCosine * 100;
                            System.out.println("Arquivo: " + caminho2.getName());
                            System.out.println("");
                            enviarAll();
                            
                        }

                    } else {
                        status = false;
                        enviarAll();
                        System.out.println("Conexão fechada!\n");
                    }
                }
            }

        } catch (Exception e) {

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
                            caminho2 = new File(convertTiffandCrop(img.getPath()));
                            System.out.println("A imagem:" + img.getName() + " já é quadrada!");
                        }
                        return true;

                    } else {
                        caminho2 = new File(convertTiffandCrop(img.getPath()));
                        return true;
                    }
                } else {
                    caminho2 = new File(img.getPath());
                    return true;
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(ClienteMagro.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;

    }


}