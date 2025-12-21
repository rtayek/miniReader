package app.core.internal;

import app.core.MiniReaderConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class Fetcher {
  Fetcher(MiniReaderConfig config) {
    this.config = config;
  }

  FetchResult fetch(String url) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(config.connectTimeout())
        .build();

    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(config.requestTimeout())
        .header("User-Agent", config.userAgent())
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .GET()
        .build();
    System.out.println(request);
    System.out.flush();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    int code = response.statusCode();
    String body = response.body();
    if (code < 200 || code >= 300) {
        String snippet = body == null ? "" : body.substring(0, Math.min(body.length(), 400));
        throw new IOException("HTTP " + code + " fetching " + url + "\n" + snippet);
    }
    System.out.println("code: "+code);
    System.out.flush();
    String contentType = response.headers().firstValue("Content-Type").orElse("");
    System.out.println("content type: "+contentType);
    System.out.flush();
    if (!contentType.toLowerCase().contains("text/html") &&
        !contentType.toLowerCase().contains("application/xhtml+xml")) {
        throw new IOException("Unsupported Content-Type: " + contentType + " for " + url);
    }
    
    return new FetchResult(url, response.statusCode(), contentType, response.body());
  }

  private final MiniReaderConfig config;
}
