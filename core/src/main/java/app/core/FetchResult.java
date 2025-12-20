package app.core;

public record FetchResult(String url, int statusCode, String contentType, String body) {}
