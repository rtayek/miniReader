package app.core;

import app.core.internal.ChunkDto;
import app.core.internal.Chunker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkerTest {
  @Test
  void chunk_shouldSplitLongTextWithOverlap() {
    String big = "0123456789".repeat(500); // 5000 chars
    DocumentDto doc = TestDocs.docWithBlocks(
        "doc1",
        "https://example.com",
        "t",
        List.of(new BlockDto.Heading(1, "H"), new BlockDto.Paragraph(big))
    );

    Chunker chunker = new Chunker(MiniReaderConfig.defaults());
    List<ChunkDto> chunks = chunker.chunk(doc);

    assertTrue(chunks.size() >= 4);
    assertEquals("doc1", chunks.getFirst().docId());
    assertTrue(chunks.stream().allMatch(c -> c.text().length() <= 900));

    String first = chunks.get(0).text();
    String second = chunks.get(1).text();

    String tail = first.substring(Math.max(0, first.length() - 120));
    assertTrue(second.contains(tail.substring(0, 40)));
  }

  @Test
  void chunk_shouldKeepHeadingPath() {
    DocumentDto doc = TestDocs.docWithBlocks(
        "doc2",
        "u",
        "t",
        List.of(
            new BlockDto.Heading(2, "Section A"),
            new BlockDto.Paragraph("Some text here."),
            new BlockDto.Heading(2, "Section B"),
            new BlockDto.Paragraph("Other text here.")
        )
    );

    Chunker chunker = new Chunker(MiniReaderConfig.defaults());
    List<ChunkDto> chunks = chunker.chunk(doc);

    assertTrue(chunks.stream().anyMatch(c -> "Section A".equals(c.headingPath())));
    assertTrue(chunks.stream().anyMatch(c -> "Section B".equals(c.headingPath())));
  }
}
