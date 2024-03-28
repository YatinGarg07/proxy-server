package org.example;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

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

    static boolean isConnectionTLS(String data){
        return data.contains("CONNECT");
    }

}
