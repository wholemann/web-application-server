package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.

            /*
            1. 먼저 user의 http request를 서버쪽에서 읽어온다.
            2. 요청된 url을 추출한다.
            3. 해당 url에 맞는 파일을 webapp 디렉토리에서 읽어서 전달한다.
             */
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            // 첫 줄 읽기
            String line = br.readLine();
            log.debug("request line : {}", line);

            if (line == null) {
                return;
            }

            String[] tokens = line.split(" ");
            int contentLength = 0;

            while (!line.equals("")) {
                line = br.readLine();
                log.debug("header : {}", line);
                if (line.contains("Content-Length")) {

                    contentLength = getContentLength(line);
                }
            }

            String requestedUrl = tokens[1];


            if (requestedUrl.equals("/user/create")) {
//                int index = requestedUrl.indexOf("?");
//                String requestPath = requestedUrl.substring(0, index);
//                String params = requestedUrl.substring(index+1);

                String requestBody = IOUtils.readData(br, contentLength);

                Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);

                User user = new User(params.get("userId"),
                        params.get("password"),
                        params.get("name"),
                        params.get("email")
                );
                log.debug("User : {}", user);
                byte[] body = Files.readAllBytes(new File("./webapp" + "/index.html").toPath());
                DataOutputStream dos = new DataOutputStream(out);
                response302Header(dos, body.length);
                responseBody(dos, body);

            } else {
                byte[] body = Files.readAllBytes(new File("./webapp" + requestedUrl).toPath());
                DataOutputStream dos = new DataOutputStream(out);
                response200Header(dos, body.length);
                responseBody(dos, body);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private int getContentLength(String line) {
        String[] headerTokens = line.split(":");
        return Integer.parseInt(headerTokens[1].trim());
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 302 FOUND \r\n");
            dos.writeBytes("Location : /index.html \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
