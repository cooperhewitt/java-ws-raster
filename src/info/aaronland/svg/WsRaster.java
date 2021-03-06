package info.aaronland.svg;

/*

  $Id: WsRaster.java,v 1.10 2008/02/29 18:57:00 asc Exp $

  http://aaronland.info/java/ws-raster/
  Copyright (c) 2008 Aaron Straup Cope. All Rights Reserved.

  This code is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License version 2 only, as
  published by the Free Software Foundation.  Sun designates this
  particular file as subject to the "Classpath" exception as provided
  by Sun in the LICENSE file that accompanied this code.
 
  This code is distributed in the hope that it will be useful, but WITHOUT
  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  version 2 for more details (a copy is included in the LICENSE file that
  accompanied this code).
 
  You should have received a copy of the GNU General Public License version
  2 along with this work; if not, write to the Free Software Foundation,
  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.

  http://xmlgraphics.apache.org/batik/using/transcoder.html#genImagefromSVGDOM
  http://javablog.co.uk/2007/10/27/http-server-api-backport-to-java-5/
  http://java.sun.com/javase/6/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpExchange.html

  curl -v -H 'Content-Type: image/svg+xml' -H 'Expect:' --data-binary \
  	'@/Users/asc/Desktop/map.svg' http://127.0.0.1:9956/png \
        > /Users/asc/Desktop/map.png

*/

import com.sun.net.httpserver.*;

import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.util.*;

import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage; 

import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;

public class WsRaster {

    private static String serverAddress;
    private static String serverName = "ws-raster";
    private static int serverPort = 9956;
    private static double serverVersion = 0.1;

    private static int maxPostSize = 1024 * 1024;

    //

    public static void main (String args[]) throws Exception {

        if (args.length != 0) {
            serverPort = Integer.parseInt (args[0]);
        }

        Handler handler = new Handler();

        InetSocketAddress addr = new InetSocketAddress (serverPort);
        HttpServer server = HttpServer.create (addr, 0);
        HttpContext ctx = server.createContext ("/", handler);
        
        ExecutorService executor = Executors.newCachedThreadPool();
        server.setExecutor (executor);
        server.start ();
        
        int port = server.getAddress().getPort();
        serverAddress = "http://localhost:" + port;

        System.out.println (serverName + " server running on port : " + port);   
        System.out.println ("documentation and usage is available at " + serverAddress + "/\n");
    }

    //

    static class Handler implements HttpHandler {

        public void handle (HttpExchange t) throws IOException {

            String uri = t.getRequestURI().toString();
            String addr = t.getRemoteAddress().getHostName();

            System.out.println ("[" + addr + "] " + uri);
            
            if (uri.equals("/")){
                usage(t);
                return;
            }
            
            if (uri.startsWith("png", 1)){
                rasterize(t);
                return;
            }

            int status = 404;
            String rsp = "IN UR SVGEEZ HIDIN FROM U";
            send_response(t, status, rsp);
            return;                
        }
    }

    //

    private static void usage (HttpExchange t) throws IOException {
        String msg =
            serverName + "\n" +
            "------------------------------------------------------\n\n" +
            serverName + " is a bare-bones HTTP interface to the Batik\n" + 
            "SVG to PNG transcoder. That is, you send a (binary) POST\n" +
            "containing an SVG file and " + serverName + " sends back\n" + 
            "the content rendered as a PNG image. That's it.\n\n" + 
            "No, really\n\n" + 
            "------------------------------------------------------\n" +
            "EXAMPLE\n" +
            "------------------------------------------------------\n\n" +

            "$> curl -v -H 'X-Width: 2048' -H 'Content-Type: image/svg+xml' \\\n" +
            "     -H 'Expect:' --data-binary '@/Users/asc/Desktop/jones.svg' \\\n" +
            "     http://127.0.0.1:9956/png > ~/Desktop/jones.png \n\n" +
            "* About to connect() to 127.0.0.1 port 9956\n" +
            "* Trying 127.0.0.1...\n" +
            "* connected\n" +
            "* Connected to 127.0.0.1 (127.0.0.1) port 9956\n\n" +
            "> POST /png HTTP/1.1\n" +
            "User-Agent: curl/7.13.1 (powerpc-apple-darwin8.0) libcurl/7.13.1 OpenSSL/0.9.7l zlib/1.2.3\n" +
            "Host: 127.0.0.1:9956\n" +
            "Pragma: no-cache\n" +
            "Accept: */*\n" +
            "X-Width: 2048\n" +
            "Content-Type: image/svg+xml\n" +
            "Content-Length: 222692\n\n" +
            "[whir-click, whir-click]\n\n" +
            "< HTTP/1.1 200 OK\n" +
            "< Date: Fri, 29 Feb 2008 16:45:21 GMT\n" +
            "< Content-length: 192191\n" +
            "< X-height: 988\n" + 
            "< X-width: 2048\n\n" +
            "[whir-click, whir-click]\n\n" +
            "* Connection #0 to host 127.0.0.1 left intact\n" +
            "* Closing connection #0\n\n" +
            "$>ls -la ~/Desktop/jones.png\n" + 
            "-rw-r--r--   1 asc  asc  192191 Feb 29 08:45 /Users/asc/Desktop/jones.png\n\n" +
            "------------------------------------------------------\n" +
            "OPTIONS\n" +
            "------------------------------------------------------\n\n" +
            "Besides specifics defined in the SVG document itself you specify additional\n" + 
            "options by passing them as HTTP headers : \n\n" +
            "* X-Width : the width of the final image; if a height option is not defined\n" +
            "\tit will be adjusted relative to the image's final width.\n\n" +
            "* X-Height : the height of the final image; if a width option is not defined\n" +
            "\tit will be adjusted relative to the image's final height.\n\n" +
            "------------------------------------------------------\n" +
            "ERRORS\n" +
            "------------------------------------------------------\n\n" +
            "Errors are returned with the HTTP status code 500. Specific error codes\n" +
            "and messages are returned both in the message body as XML and in the\n" +
            "'X-ErrorCode' and 'X-ErrorMessage' headers.\n\n" +
            "------------------------------------------------------\n" +
            "NOTES\n" +
            "------------------------------------------------------\n\n" +
            "By default, " + serverName + " runs on port " + serverPort + ". You can override\n" +
            "this value by specifying your own port number as the first\n" +
            "(command-line) argument when you start the server or by\n" +
            "setting the 'PORT' variable in the start.sh script.\n\n" +
            "Relative includes or pointers (src, href, or otherwise) are\n" + 
            "no supported.\n\n" +
            "The maximum size of a file that you may POST to " + serverName + "\n" +
            "is 1024kb.\n\n" +
            "There is no logging to speak of.\n\n" +
            "------------------------------------------------------\n" +
            "SEE ALSO\n" +
            "------------------------------------------------------\n\n" +
            "http://xmlgraphics.apache.org/batik/using/transcoder.html#genImagefromSVGDOM\n" +
            "http://javablog.co.uk/2007/10/27/http-server-api-backport-to-java-5/\n" +
            "http://www.aaronland.info/weblog/2008/02/05/fox#ws-raster/\n" +
            "http://www.aaronland.info/papernet/\n" +
            "\n" + 
            "------------------------------------------------------\n" +
            "VERSION\n" +
            "------------------------------------------------------\n\n" +
            serverVersion + "\n\n" +
            "------------------------------------------------------\n" +
            "LICENSE\n" +
            "------------------------------------------------------\n\n" +
            "Copyright (c) 2008 Aaron Straup Cope. All Rights Reserved.\n\n" +
            "This is free software. You may redistribute it and/or modify it\n" +
            "under the same terms as the GPL license.\n\n" + 
            "\n";

        int status = 200;
        send_response(t, status, msg);
    }

    //

    private static void rasterize (HttpExchange t) throws IOException {

        if (! t.getRequestMethod().equals ("POST")) {
            int errcode = 100;
            String errmsg = "Method Not Allowed";
            send_error(t, errcode, errmsg);
            return;
        }

        // i has an image?

        Headers headers = t.getRequestHeaders();
        String ctype = headers.getFirst("Content-Type");

        String [] parts = ctype.split("/");

        if (! parts[0].equals("image")) {
            int errcode = 200;
            String errmsg = "Invalid content-type";
            send_error(t, errcode, errmsg);
            return;
        }

        String ext;
        String fmt;

        if (parts[1].equals("svg+xml")) {
            ext = ".svg";
            fmt = "svg";
        }

        else {
            int errcode = 210;
            String errmsg = "Invalid image format";
            send_error(t, errcode, errmsg);
            return;
        }

        InputStream is = t.getRequestBody();        
        int sz = is.available();   

        if (sz > maxPostSize){
            is.close();

            int errcode = 220;
            String errmsg = "File too large";
            send_error(t, errcode, errmsg);
            return;            
        }
        
        String width = headers.getFirst("X-Width");
        String height = headers.getFirst("X-Height");

        try {

            PNGTranscoder tr = new PNGTranscoder();

            if (width != null){
                tr.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(width));
            }

            if (height != null){
                tr.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(height));
            }

            TranscoderInput input = new TranscoderInput(is);

            File tmpfile = File.createTempFile("svg", ".png");
            // System.out.println("create tmp file " + tmpfile);

            OutputStream os = new FileOutputStream(tmpfile.toString());

            TranscoderOutput output = new TranscoderOutput(os);
            tr.transcode(input, output);

            os.flush();
            os.close();

            //

            InputStream im = new FileInputStream(tmpfile.toString());
            BufferedImage buf = ImageIO.read(im);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            //

            Headers h = t.getResponseHeaders();
            OutputStream rsp = t.getResponseBody();

            h.set("X-Height", String.valueOf(buf.getHeight()));
            h.set("X-Width", String.valueOf(buf.getWidth()));

            ImageIO.write(buf, "png", bos);
            byte[] imageInBytes = bos.toByteArray();
            int length = imageInBytes.length;

            im.close(); 
            tmpfile.delete();

            int status = 200;
            t.sendResponseHeaders (status, length);
            rsp.write(imageInBytes);

            rsp.close();
            t.close();
        }
        
        catch ( Exception e) {
            // e.printStackTrace();

            int errcode = 310;
            String errmsg = e.toString();
            send_error(t, errcode, errmsg);
        }

        return;
    }

    private static void send_error (HttpExchange t, int errcode,  String errmsg) throws IOException {

        String rsp = 
            "<?xml version=\"1.0\" ?>\n" + 
            "<error code=\"" + errcode + "\">" + errmsg + "</error>";

        Headers headers = t.getResponseHeaders();
        headers.set("X-ErrorCode", String.valueOf(errcode));
        headers.set("X-ErrorMessage", errmsg);
        headers.set("Content-Type", "text/xml");

        int status = 500;
        send_response(t, status, rsp);        
        return;
    }

    private static void send_response (HttpExchange t, int status, String msg) throws IOException {

        Headers headers = t.getResponseHeaders();
        headers.set("X-" + serverName + "-Version", String.valueOf(serverVersion));

        OutputStream os = t.getResponseBody();
        t.sendResponseHeaders (status, msg.length());
        os.write(msg.getBytes());
        t.close();
    }

}
