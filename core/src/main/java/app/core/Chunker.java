package app.core;

import java.util.ArrayList;
import java.util.List;

class Chunker {
  List<ChunkDto> chunk(DocumentDto doc) {
    List<ChunkDto> out = new ArrayList<>();
    BlockAccumulator acc = new BlockAccumulator(doc.id(), out);
    for (BlockDto b : doc.blocks()) {
      b.accept(acc);
    }
    acc.flush();
    return out;
  }

  private static final class BlockAccumulator implements BlockDto.BlockVisitor<Void> {
    BlockAccumulator(String docId, List<ChunkDto> out) {
      this.docId = docId;
      this.out = out;
    }

    @Override
    public Void heading(BlockDto.Heading h) {
      flush();
      buf.setLength(0);
      headingPath = h.text();
      return null;
    }

    @Override
    public Void paragraph(BlockDto.Paragraph p) {
      buf.append(p.text()).append("\n\n");
      return null;
    }

    @Override
    public Void code(BlockDto.Code c) {
      buf.append(c.text()).append("\n\n");
      return null;
    }

    @Override
    public Void bulletedList(BlockDto.BulletedList ul) {
      for (String item : ul.items()) buf.append("- ").append(item).append("\n");
      buf.append("\n");
      return null;
    }

    void flush() {
      String text = buf.toString().strip();
      if (text.isBlank()) return;

      int maxChars = 900;
      int overlap = 180;

      int i = 0;
      while (i < text.length()) {
        int end = Math.min(text.length(), i + maxChars);
        String slice = text.substring(i, end).strip();
        if (!slice.isBlank()) out.add(new ChunkDto(docId, chunkId++, headingPath, slice));
        if (end == text.length()) break;
        i = Math.max(0, end - overlap);
      }
    }

    private final StringBuilder buf = new StringBuilder();
    private final String docId;
    private final List<ChunkDto> out;
    private String headingPath = "";
    private int chunkId = 0;
  }
}
