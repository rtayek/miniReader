package app.core;

record FetchResult(String url, int statusCode, String contentType, String body) {}
