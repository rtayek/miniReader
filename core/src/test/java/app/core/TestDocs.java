package app.core;

import java.time.Instant;
import java.util.List;

/**
 * Tiny helpers to make test data setup concise.
 */
class TestDocs {
  static DocumentDto docWithParagraphs(String id, String url, String title, String... paragraphs) {
    return docWithBlocks(id, url, title, paragraphsToBlocks(paragraphs));
  }

  static DocumentDto docWithBlocks(String id, String url, String title, List<BlockDto> blocks) {
    return new DocumentDto(
        id,
        url,
        title,
        Instant.EPOCH,
        List.copyOf(blocks),
        List.of(),
        blocksToPlain(blocks)
    );
  }

  static List<BlockDto> paragraphsToBlocks(String... paragraphs) {
    return List.of(paragraphs).stream()
        .map(BlockDto.Paragraph::new)
        .map(b -> (BlockDto) b)
        .toList();
  }

  static ChunkDto chunk(String docId, int chunkId, String heading, String text) {
    return new ChunkDto(docId, chunkId, heading, text);
  }

  private static String blocksToPlain(List<BlockDto> blocks) {
    StringBuilder sb = new StringBuilder();
    for (BlockDto b : blocks) {
      if (b instanceof BlockDto.Paragraph p) {
        sb.append(p.text()).append("\n\n");
      }
    }
    return sb.toString().strip();
  }
}
