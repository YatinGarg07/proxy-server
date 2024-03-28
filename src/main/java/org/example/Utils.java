package org.example;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.*;
import java.net.Socket;

public class Utils {



    public static Request parseHttpRequest(String httpRequestString) {
        String[] lines = httpRequestString.split("\n");
        String[] requestLineParts = lines[0].split(" ");

        String method = requestLineParts[0];
        String url = requestLineParts[1];
        String httpVersion = requestLineParts[2];

        Request.Builder builder = new Request.Builder()
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
            else builder.method("GET",null);
        }



        return builder.build();
    }

    static Request parseHttpsRequest(String httpsRequestString) throws IOException {


        // Read the request line
        String lines[] = httpsRequestString.split("\n");
        String parts[] = lines[0].split(" ");
//        if (requestLine == null) {
//            clientSocket.close();
//            return;
//        }

        // Parse the request line to get method, URL, and HTTP version
        //String[] parts = requestLine.split(" ");
        String method = parts[0];
        String url = parts[1];
        String httpVersion = parts[2];

        // Read the request headers
        Headers.Builder headersBuilder = new Headers.Builder();
        //String line;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                // End of headers
                break;
            }
            String[] headerParts = line.split(": ");
            if (headerParts.length == 2) {
                if(headerParts[1].contains("gzip")) continue;
                headersBuilder.add(headerParts[0], headerParts[1]);
            }
        }
        // Parse body if present
        String requestBody = null;
        if (lines.length > 1) {
            requestBody = lines[lines.length - 1];

            //In GET Method there is no BODY
//            if(!method.equals("GET"))
//                builder.method(method, RequestBody.create(MediaType.get("text/plain"), requestBody));
//            else builder.method("GET",null);
        }

        // Build the OkHttp Request object
        Request.Builder requestBuilder = new Request.Builder()
                .method(method, requestBody != null ? RequestBody.create(MediaType.get("text/plain"), requestBody) : null)
                .url("https://" + url)
                .headers(headersBuilder.build());
//        while ((line = reader.readLine()) != null && !line.isEmpty()) {
//
//
//        }

        // Read the request body if present
//        String requestBody = null;
//        if (reader.ready()) {
//            StringBuilder bodyBuilder = new StringBuilder();
//            while ((line = reader.readLine()) != null) {
//                bodyBuilder.append(line).append("\r\n");
//            }
//            requestBody = bodyBuilder.toString();
//        }

        return requestBuilder.build();
    }

    static boolean isConnectionTLS(String data){
        if(data == null) return false;
        return data.contains("CONNECT");
    }


    public static void startTunneling(Socket clientSocket, Socket serverSocket) throws InterruptedException {


        Thread clientToServerThread = Thread.ofVirtual().start(()-> forwardData(clientSocket,serverSocket));
        Thread serverToClientThread = Thread.ofVirtual().start(() -> forwardData(serverSocket, clientSocket));

        clientToServerThread.join();
        serverToClientThread.join();
    }

    private static void forwardData(Socket sourceSocket, Socket destinationSocket) {
        try (InputStream in = sourceSocket.getInputStream();
             OutputStream out = destinationSocket.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ( (bytesRead = in.read(buffer)) != -1 && bytesRead != 0 ) {
                //System.out.println(bytesRead);
                out.write(buffer, 0, bytesRead);
                //out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
