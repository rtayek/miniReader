package app.core.internal;

public record FetchResult(String url, int statusCode, String contentType, String body) {}
