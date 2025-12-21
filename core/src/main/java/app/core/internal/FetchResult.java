package app.core.internal;

record FetchResult(String url, int statusCode, String contentType, String body) {}
