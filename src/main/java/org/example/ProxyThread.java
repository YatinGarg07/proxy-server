package org.example;

import okhttp3.*;

import java.io.*;
import java.net.Socket;

public class ProxyThread extends Thread{
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


            // Checking if Connection is TLS
            isConnectionTLS = Utils.isConnectionTLS(requestLine);

            //Extracting Server Address & Server Port from Request Line
            if(isConnectionTLS){
                serverPort = 443;
                serverAddress = requestLine
                        .split("CONNECT")[1]
                        .split(" ")[1]
                        .split(":")[0];
            }
            else{
                serverPort = 80;
                //reading 2nd Line Host:
                serverAddress = requestLine.split(" ")[1];
            }

            //Checking Cache
            if(!cache.get(serverAddress).equals("-1")){
                //Cache Hit
                //Getting Response From Cache
                synchronized (cache){
                    String resp = cache.get(serverAddress);
                    System.out.println("Current Cache");
                    //cache.printCache();
                    System.out.println("Cache Hit for URL: " + serverAddress  + ": " + resp);
                    clientWriter.println(resp);
                }


            }
            else{
                System.out.println("Cache Miss: Calling Target Server...");
                requestToTargetServer(requestLine,clientReader,clientWriter);
            }

            //Closing Client Socket
            clientSocket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    void requestToTargetServer(String requestLine, BufferedReader clientReader, PrintWriter clientWriter) throws IOException {
        // Sending request to target server
        if(isConnectionTLS){

        }
        else{
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
            if(response.code() == 200){
                System.out.println("Got Response for target server: "+serverAddress);
                String resp = response.body().string();

                //Updating Cache
                synchronized (cache){
                    cache.put(serverAddress,resp);
                }

                //Passing Response To Client
                clientWriter.println(resp);
                response.close();
            }

        }
    }

}
