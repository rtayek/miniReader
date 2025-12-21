package app.core.internal;

import app.core.BlockDto;
import app.core.DocumentDto;
import app.core.MiniReaderConfig;

import java.util.ArrayList;
import java.util.List;

class Chunker {
  Chunker(MiniReaderConfig config) {
    this.config = config;
  }

  List<ChunkDto> chunk(DocumentDto doc) {
    List<ChunkDto> out = new ArrayList<>();
    BlockAccumulator acc = new BlockAccumulator(doc.id(), out, config.chunkMaxChars(), config.chunkOverlapChars());
    for (BlockDto b : doc.blocks()) {
      b.accept(acc);
    }
    acc.flush();
    return out;
  }

  private static final class BlockAccumulator implements BlockDto.BlockVisitor<Void> {
    BlockAccumulator(String docId, List<ChunkDto> out, int maxChars, int overlapChars) {
      this.docId = docId;
      this.out = out;
      this.maxChars = maxChars;
      this.overlapChars = overlapChars;
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

      int i = 0;
      while (i < text.length()) {
        int end = Math.min(text.length(), i + maxChars);
        String slice = text.substring(i, end).strip();
        if (!slice.isBlank()) out.add(new ChunkDto(docId, chunkId++, headingPath, slice));
        if (end == text.length()) break;
        i = Math.max(0, end - overlapChars);
      }
    }

    private final StringBuilder buf = new StringBuilder();
    private final String docId;
    private final List<ChunkDto> out;
    private final int maxChars;
    private final int overlapChars;
    private String headingPath = "";
    private int chunkId = 0;
  }

  private final MiniReaderConfig config;
}
