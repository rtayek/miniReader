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
    MiniReaderConfig config = MiniReaderConfig.fromBaseDir(tempDir);

    try (LuceneIndex index = new LuceneIndex(config)) {
      DocumentDto doc = TestDocs.docWithParagraphs(
          "doc1",
          "https://example.com/doc1",
          "Doc 1",
          "JavaFX is a UI toolkit for the JVM."
      );

      List<ChunkDto> chunks = List.of(
          TestDocs.chunk(doc.id(), 0, "Intro", "JavaFX is a UI toolkit for the JVM."),
          TestDocs.chunk(doc.id(), 1, "Other", "Unrelated text about bananas.")
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
    MiniReaderConfig config = MiniReaderConfig.fromBaseDir(tempDir);

    try (LuceneIndex index = new LuceneIndex(config)) {
      DocumentDto doc = TestDocs.docWithBlocks("doc2", "u", "t", List.of());

      index.index(doc, List.of(TestDocs.chunk("doc2", 0, "", "cats dogs")));
      assertFalse(index.search("cats", 5).isEmpty());

      index.index(doc, List.of(TestDocs.chunk("doc2", 0, "", "lemons limes")));
      assertTrue(index.search("cats", 5).isEmpty());
      assertFalse(index.search("lemons", 5).isEmpty());
    }
  }
}
