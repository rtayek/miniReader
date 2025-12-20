package app.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LuceneIndexTest {
  @TempDir
  Path tempDir;

  @Test
  void indexAndSearch_shouldFindRelevantChunk() throws Exception {
    PathsConfig paths = new TestPathsConfig(tempDir);

    try (LuceneIndex index = new LuceneIndex(paths)) {
      DocumentDto doc = new DocumentDto(
          "doc1",
          "https://example.com/doc1",
          "Doc 1",
          Instant.now(),
          List.of(new BlockDto.Paragraph("JavaFX is a UI toolkit for the JVM.")),
          List.of(),
          "JavaFX is a UI toolkit for the JVM."
      );

      List<ChunkDto> chunks = List.of(
          new ChunkDto(doc.id(), 0, "Intro", "JavaFX is a UI toolkit for the JVM."),
          new ChunkDto(doc.id(), 1, "Other", "Unrelated text about bananas.")
      );

      index.index(doc, chunks);

      List<LuceneIndex.SearchHit> hits = index.search("JavaFX toolkit", 5);
      assertFalse(hits.isEmpty());

      LuceneIndex.SearchHit best = hits.getFirst();
      assertEquals("doc1", best.docId());
      assertTrue(best.text().toLowerCase().contains("javafx"));
    }
  }

  @Test
  void reindex_shouldReplaceOldChunks() throws Exception {
    PathsConfig paths = new TestPathsConfig(tempDir);

    try (LuceneIndex index = new LuceneIndex(paths)) {
      DocumentDto doc = new DocumentDto("doc2", "u", "t", Instant.now(), List.of(), List.of(), "");

      index.index(doc, List.of(new ChunkDto("doc2", 0, "", "cats dogs")));
      assertFalse(index.search("cats", 5).isEmpty());

      index.index(doc, List.of(new ChunkDto("doc2", 0, "", "lemons limes")));
      assertTrue(index.search("cats", 5).isEmpty());
      assertFalse(index.search("lemons", 5).isEmpty());
    }
  }
}
