package http;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import static org.junit.Assert.*;


/**
 * Created by wholeman on 2019-02-13
 * Github : http://github.com/wholemann
 * E-Mail : gowgow0606@naver.com
 */
public class HttpRequestTest {
    private String testDirectory = "./src/test/resources/";

    @Test
    public void request_GET() throws Exception {
        InputStream in = new FileInputStream(new File(testDirectory + "Http_GET.txt"));
        HttpRequest request = new HttpRequest(in);

        assertEquals("GET", request.getMethod());
        assertEquals("/user/create", request.getPath());
        assertEquals("keep-alive", request.getHeader("Connection"));
        assertEquals("javajigi", request.getParameter("userId"));
    }

    @Test
    public void request_POST() throws Exception {
        InputStream in = new FileInputStream(new File(testDirectory + "Http_POST.txt"));
        HttpRequest request = new HttpRequest(in);

        assertEquals("POST", request.getMethod());
        assertEquals("/user/create", request.getPath());
        assertEquals("keep-alive", request.getHeader("Connection"));
        assertEquals("javajigi", request.getParameter("userId"));

    }
}