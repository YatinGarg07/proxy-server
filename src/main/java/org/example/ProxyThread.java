package org.example;

import okhttp3.*;

import java.io.*;
import java.net.Socket;

public class ProxyThread extends Thread{
    private Socket clientSocket;
    String serverAddress;
    int serverPort;

    boolean isConnectionTLS = false;

    public ProxyThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            //Client socket reader & writer
            BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            //Reading only 1st line in Request called requestLine
            String requestLine = clientReader.readLine();

            //Marking ClientReader, marking to return to the current line in buffer when clientReader.reset() is called
            clientReader.mark(0);

            // Checking if Connection is TLS
            isConnectionTLS = isConnectionTLS(requestLine);

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
                serverAddress = "http://"+clientReader.readLine().split("Host: ")[1].split("\\n")[0];
            }

            // Sending request to target server
            if(isConnectionTLS){



            }
            else{

                clientReader.reset();
                StringBuilder reqBody = new StringBuilder(requestLine + "\n");

                //Temporary String Used for reading From Buffer
                String reqTemp;
                while ((reqTemp = clientReader.readLine()) != null && !reqTemp.isEmpty()) {
                    reqBody.append(reqTemp).append("\n");
                    //clientWriter.println(response + "\n");
                }

               Request clientFormattedRequest = parseHttpRequest(reqBody.toString());
                System.out.println(clientFormattedRequest);

                //Client Request Being Forwarded to Target Server
                OkHttpClient client = new OkHttpClient();
                Call call = client.newCall(clientFormattedRequest);
                Response response = call.execute();
                if(response.code() == 200){
                    System.out.println("Response from target server:");
                    String resp = response.body().string();
                    System.out.println(resp);

                    //Passing Response To Client
                    clientWriter.println(resp);
                    response.close();
                }

            }
            //Closing Client Socket
            clientSocket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    boolean isConnectionTLS(String data){
        return data.contains("CONNECT");
    }

    private static Request parseHttpRequest(String httpRequestString) {
        String[] lines = httpRequestString.split("\n");
        String[] requestLineParts = lines[0].split(" ");

        String method = requestLineParts[0];
        String url = requestLineParts[1];
        String httpVersion = requestLineParts[2];

        Request.Builder builder = new Request.Builder()
                .method(method, null)
                .url(url);

        // Parse headers
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                // End of headers
                break;
            }
            String[] headerParts = line.split(": ");
            if(headerParts[1].contains("gzip")) continue;
            builder.addHeader(headerParts[0], headerParts[1]);
        }

        // Parse body if present
        if (lines.length > 1) {
            String requestBody = lines[lines.length - 1];

            //In GET Method there is no BODY
            if(!method.equals("GET"))
            builder.method(method, RequestBody.create(MediaType.get("text/plain"), requestBody));
        }



        return builder.build();
    }




}
