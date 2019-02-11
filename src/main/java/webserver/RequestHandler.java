package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

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

            while (!line.equals("")) {
                line = br.readLine();
                log.debug("header : {}", line);
            }
            String requestedUrl = tokens[1];

            if (requestedUrl.startsWith("/user/create")) {
                int index = requestedUrl.indexOf("?");
                String requestPath = requestedUrl.substring(0, index);
                String params = requestedUrl.substring(index+1);
                User user = new User(
                        HttpRequestUtils.parseQueryString(params).get("userId"),
                        HttpRequestUtils.parseQueryString(params).get("password"),
                        HttpRequestUtils.parseQueryString(params).get("name"),
                        HttpRequestUtils.parseQueryString(params).get("email")
                );
                log.debug("User : {}", user);

            } else {
                byte[] body = body = Files.readAllBytes(new File("./webapp" + requestedUrl).toPath());
                DataOutputStream dos = new DataOutputStream(out);
                response200Header(dos, body.length);
                responseBody(dos, body);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
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

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
