package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.Database;
import model.User;
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
            
        	//convert inputstream to bufferedreader
        	BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        	String line = br.readLine();
        	if (line == null) {
        		return;
        	}
        	
        	String url = HttpRequestUtils.getUrl(line);
			Map<String, String> headers = new HashMap<String, String>();
        	while(!"".equals(line)) {
			log.debug("header: {}", line);
				
				line = br.readLine();
				String[] headerTokens = line.split(": ");
				if(headerTokens.length == 2)
					headers.put(headerTokens[0], headerTokens[1]);
			}
        	log.debug("Content-Length : {}", headers.get("Content-Length"));
        	
        	//log.debug("request path : {}", url);
        	if(url.startsWith("/create")) {
        		//int index = url.indexOf("?");
        		//String requestPath = url.substring(0, index);
        		
        		
        		//String queryString = url.substring(index + 1);
        		String requestBody = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
        		log.debug("Reeuest Body : {}", requestBody);
        		Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);
        		User user = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
        		
        		log.debug("User : {}", user);
        		Database.addUser(user);
        		
        		DataOutputStream dos = new DataOutputStream(out);
        		response302Header(dos);
        	} else if(url.equals("/login")) {
        		String requestBody = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
        		log.debug("Reeuest Body : {}", requestBody);
        		Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);
        		
        		log.debug("userId : {}, password : {}, ", params.get("userId"), params.get("password"));
        		User user = Database.getUser(params.get("userId"));
        		if (user == null) {
        			log.debug("User Not Found!");
        		} else if (user.getPassword().equals(params.get("password"))) {
        			log.debug("login success!!");
        		} else {
        			log.debug("Password Mismatch!!");
        		}
        		
        		DataOutputStream dos = new DataOutputStream(out);
        		response302Header(dos);
        		
        	}
        	else {
        		DataOutputStream dos = new DataOutputStream(out);
                byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                response200Header(dos, body.length);
                responseBody(dos, body);
        	}
        	
//        	while(!"".equals(line)) {
//        		log.debug("header: {}", line);
//        		line = br.readLine();
//        	}
        	
            
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302HeaderWithCookie(DataOutputStream dos, String cookie) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: /index.html\r\n");
            dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    
    
    private void response302Header(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: /index.html\r\n");
            dos.writeBytes("\r\n");
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
