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

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    String contentType = response.headers().firstValue("Content-Type").orElse("");
    return new FetchResult(url, response.statusCode(), contentType, response.body());
  }

  private final MiniReaderConfig config;
}
