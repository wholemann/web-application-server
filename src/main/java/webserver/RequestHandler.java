package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

import javax.xml.crypto.Data;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;
    private boolean includeCSS = false;

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
            String requestHeaderCookie = null;

            while (!line.equals("")) {
                line = br.readLine();
                log.debug("header : {}", line);
                if (line.contains("Content-Length")) {
                    contentLength = getContentLength(line);
                }

                if (line.contains("Cookie")) {
                    requestHeaderCookie = getCookie(line);
                }

                if (line.contains("text/css")) {
                    log.debug("css문서");
                    includeCSS = true;
                }
            }

            String requestedUrl = tokens[1];

            if (requestedUrl.equals("/user/create")) {
                String requestBody = IOUtils.readData(br, contentLength);
                Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);

                User user = new User(params.get("userId"),
                        params.get("password"),
                        params.get("name"),
                        params.get("email")
                );

                DataBase.addUser(user);
                log.debug("User : {}", user);
                byte[] body = Files.readAllBytes(new File("./webapp" + "/index.html").toPath());
                DataOutputStream dos = new DataOutputStream(out);
                response302Header(dos, "/index.html");
                responseBody(dos, body);

            } else if (requestedUrl.equals("/user/login")) {
                log.debug("로그인");
                String requestBody = IOUtils.readData(br, contentLength);
                Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);
                log.debug(requestBody);

                User user = DataBase.findUserById(params.get("userId"));
                // 해당 유저가 가입된 경우
                if (user != null) {
                    log.debug("가입된 유저 : " + user.getUserId());
                    //비밀 번호가 일치하는 경우
                    if (user.getPassword().equals(params.get("password"))) {
                        log.debug("로그인 성공");
                        byte[] body = Files.readAllBytes(new File("./webapp" + "/index.html").toPath());
                        DataOutputStream dos = new DataOutputStream(out);
                        responseLogin(dos, body.length, true);
                        responseBody(dos, body);
                        // 비밀 번호가 일치하지 않는 경우
                    } else {
                        log.debug("로그인 실패");
                        byte[] body = Files.readAllBytes(new File("./webapp" + "/user/login_failed.html").toPath());
                        DataOutputStream dos = new DataOutputStream(out);
                        responseLogin(dos, body.length, false);
                        responseBody(dos, body);
                    }
                    // 해당 유저가 DB에 없는 경우
                } else {
                    log.debug("가입되지 않은 유저");
                    byte[] body = Files.readAllBytes(new File("./webapp" + "/user/login_failed.html").toPath());
                    DataOutputStream dos = new DataOutputStream(out);
                    responseLogin(dos, body.length, false);
                    responseBody(dos, body);
                }

            } else if (requestedUrl.equals("/user/list.html")) {
                log.debug("사용자 목록 보여주기");
                log.debug(requestHeaderCookie);

                if (requestHeaderCookie != null) {
                    Map<String, String> cookieParams = HttpRequestUtils.parseCookies(requestHeaderCookie);
                    String logined = cookieParams.get("logined");
                    if (Boolean.parseBoolean(logined)) {
                        Collection<User> userList = DataBase.findAll();
                        StringBuilder sb = new StringBuilder();
                        sb.append("<table border='1'>");
                        for (User user : userList) {
                            sb.append("<tr>");
                            sb.append("<td>" + user.getUserId() + "</td>");
                            sb.append("<td>" + user.getName() + "</td>");
                            sb.append("<td>" + user.getEmail() + "</td>");
                            sb.append("</tr>");
                        }
                        sb.append("</table>");
                        byte[] body = sb.toString().getBytes();
                        DataOutputStream dos = new DataOutputStream(out);
                        response200Header(dos, body.length);
                        responseBody(dos, body);
                    } else {
                        log.debug("로그인 되지 않은 사용자");
                        byte[] body = Files.readAllBytes(new File("./webapp" + "/user/login.html").toPath());
                        DataOutputStream dos = new DataOutputStream(out);
                        response302Header(dos, "/user/login.html");
                        responseBody(dos, body);
                    }

                } else {
                    byte[] body = Files.readAllBytes(new File("./webapp" + "/user/login.html").toPath());
                    DataOutputStream dos = new DataOutputStream(out);
                    response302Header(dos, "/user/login.html");
                    responseBody(dos, body);
                }

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

    private String getCookie(String line) {
        String[] headerTokens = line.split(":");
        return headerTokens[1].trim();
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            if (includeCSS) {
                dos.writeBytes("Content-Type: text/css\r\n");
            } else {
                dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            }
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error("response200Header: "+ e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String url) {
        try {
            dos.writeBytes("HTTP/1.1 302 FOUND \r\n");
            dos.writeBytes("Location: " + url + "\r\n");
            if (includeCSS) {
                dos.writeBytes("Content-Type: text/css\r\n");
            } else {
                dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            }//            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error("response302Header: " + e.getMessage());
        }
    }

    private void responseLogin(DataOutputStream dos, int lengthOfBodyContent, boolean loginSuccess) {
        try {
            dos.writeBytes("HTTP/1.1 302 FOUND \r\n");
            if (includeCSS) {
                dos.writeBytes("Content-Type: text/css\r\n");
            } else {
                dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            }
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            if (loginSuccess) {
                dos.writeBytes("Location: /index.html\n");
                dos.writeBytes("Set-Cookie: logined=" + String.valueOf(loginSuccess) + "\r\n");
            } else {
                dos.writeBytes("Location: /user/login_failed.html\n");
                dos.writeBytes("Set-Cookie: logined=" + String.valueOf(loginSuccess) + "\r\n");
            }
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error("responseSetCookieHeader: " + e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error("responseBody: " + e.getMessage());
        }
    }
}

