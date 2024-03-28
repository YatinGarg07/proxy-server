package org.example;

import okhttp3.*;

import java.io.*;
import java.net.Socket;

public class ProxyThread extends Thread {
    private final LRUCache cache;
    private final Socket clientSocket;
    String serverAddress;
    int serverPort;

    boolean isConnectionTLS = false;

    public ProxyThread(Socket clientSocket, LRUCache cache) {
        this.clientSocket = clientSocket;
        this.cache = cache;
    }

    @Override
    public void run() {
        try {
            //Client socket reader & writer
            BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);

            //Reading only 1st line in Request called requestLine
            String requestLine = clientReader.readLine();
            if(requestLine == null) return;

            // Checking if Connection is TLS
            isConnectionTLS = Utils.isConnectionTLS(requestLine);

            //Extracting Server Address & Server Port from Request Line
            if (isConnectionTLS) {
                serverPort = 443;
                serverAddress =  requestLine
                        .split("CONNECT")[1]
                        .split(" ")[1]
                        .split(":")[0];
            } else {
                serverPort = 80;
                //reading 2nd Line Host:
                serverAddress = requestLine.split(" ")[1];
            }

            //Checking Cache only in case of HTTP
            String cacheResponse = "";
            synchronized (cache) {
                //Getting Response From Cache
                if(!isConnectionTLS) cacheResponse = cache.get(serverAddress);
            }
            if (!isConnectionTLS && !cacheResponse.equals("-1")) {
                //Cache Hit
                //System.out.println("Current Cache");
                //cache.printCache();
                System.out.println("Cache Hit for URL: " + serverAddress);
                clientWriter.println(cacheResponse);

            } else {
                requestToTargetServer(requestLine, clientReader, clientWriter);
            }
            System.out.println("Client Socket Closing for: " + serverAddress);
            clientSocket.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    void requestToTargetServer(String requestLine, BufferedReader clientReader, PrintWriter clientWriter) throws IOException, InterruptedException {
        // Sending request to target server
        if (isConnectionTLS) {
            // Establish a connection to the target server
            Socket serverSocket = new Socket(serverAddress,serverPort);

            //System.out.println(serverSocket.isConnected());

            // Send a success response to the client
            clientWriter.println("HTTP/1.1 200 Connection Established\r\n\r\n");
            clientWriter.flush();

            // Start tunnelling data
            // Start forwarding data between client and server
            Utils.startTunneling(clientSocket,serverSocket);

            //Closing Server Socket
            System.out.println("Closing Server Socket");
            serverSocket.close();

        } else {
            System.out.println("Cache Miss: Calling Target Server... " + serverAddress);
            StringBuilder reqBody = new StringBuilder(requestLine + "\n");

            //Temporary String Used for reading From Buffer
            String reqTemp;
            while ((reqTemp = clientReader.readLine()) != null && !reqTemp.isEmpty()) {
                reqBody.append(reqTemp).append("\n");
            }

            Request clientFormattedRequest = Utils.parseHttpRequest(reqBody.toString());

            //Client Request Being Forwarded to Target Server
            OkHttpClient client = new OkHttpClient();
            Call call = client.newCall(clientFormattedRequest);
            Response response = call.execute();
            if (response.code() == 200) {
                System.out.println("Got Response for target server: " + serverAddress);
                String resp = response.body().string();

                //Updating Cache
                synchronized (cache) {
                    cache.put(serverAddress, resp);
                }

                //Passing Response To Client
                clientWriter.println(resp);
                clientWriter.flush();
                response.close();
            }


        }


    }

}
