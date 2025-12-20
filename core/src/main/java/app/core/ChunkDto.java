package app.core;

public record ChunkDto(String docId, int chunkId, String headingPath, String text) {}
