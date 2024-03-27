package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyServer {
    static int PORT = 8082;

    public static void main(String[] args) {

        try {
            ServerSocket serverSocket =new ServerSocket(PORT);
            System.out.println("Proxy server running on port " + PORT);

            while (true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());

                new ProxyThread(clientSocket).start();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }


}
