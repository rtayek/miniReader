package app.core.internal;

import app.core.MiniReaderConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class FetcherTest {
  @Test
  void fetch_shouldReturnStatusContentTypeAndBody() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        String body = "<html><body>Hello</body></html>";
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(body.getBytes());
        }
      }
    });
    server.start();

    try {
      int port = server.getAddress().getPort();
      String url = "http://localhost:" + port + "/";

      Fetcher fetcher = new Fetcher(MiniReaderConfig.defaults());
      FetchResult result = fetcher.fetch(url);

      assertEquals(200, result.statusCode());
      assertTrue(result.contentType().toLowerCase().contains("text/html"));
      assertTrue(result.body().contains("Hello"));
    } finally {
      server.stop(0);
    }
  }
}
