package http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wholeman on 2019-02-13
 * Github : http://github.com/wholemann
 * E-Mail : gowgow0606@naver.com
 */
public class HttpRequest {
    private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);

    String requestMethod;
    String requestUrl;
    Map<String, String> requestHttpHeader = new HashMap<>();
    Map<String, String> requestHttpBody = new HashMap<>();

    public HttpRequest(InputStream in) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            // 첫 줄 읽기
            String line = br.readLine();
            processRequestLine(line);

            while (!line.equals("")) {
                line = br.readLine();
                log.debug("header : {}", line);
                if (line.equals("")) {
                    break;
                }
                String[] fields = line.split(":", 2);
                requestHttpHeader.put(fields[0].trim(), fields[1].trim());
            }

            if (getMethod().equals("POST")) {
                String requestBody = IOUtils.readData(br, Integer.parseInt(getHeader("Content-Length")));
                requestHttpBody = HttpRequestUtils.parseQueryString(requestBody);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processRequestLine(String line) {
        log.debug("request line : {}", line);
        if (line == null) {
            return;
        }
        String[] tokens = line.split(" ");
        // tokens[0] : GET or POST
        // tokens[1] : url
        requestMethod = tokens[0];
        if (requestMethod.equals("POST")) {
            requestUrl = tokens[1];
        }

        if (requestMethod.equals("GET")) {
            // GET은 parameter가 url에 같이 온다.
            String[] paths = tokens[1].split("\\?");
            requestUrl = paths[0];
            requestHttpBody = HttpRequestUtils.parseQueryString(paths[1]);
        }
    }

    public String getMethod() {
        return requestMethod;
    }

    public String getPath() {
        return requestUrl;
    }

    public String getHeader(String field) {
        return requestHttpHeader.get(field);
    }

    public String getParameter(String key) {
        return requestHttpBody.get(key);
    }
}
