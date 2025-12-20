package app.core.internal;

public record ChunkDto(String docId, int chunkId, String headingPath, String text) {}
