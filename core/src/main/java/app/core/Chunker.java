package app.core;

import java.util.ArrayList;
import java.util.List;

class Chunker {
  List<ChunkDto> chunk(DocumentDto doc) {
    List<ChunkDto> out = new ArrayList<>();
    String headingPath = "";
    int chunkId = 0;

    StringBuilder buf = new StringBuilder();
    for (BlockDto b : doc.blocks()) {
      if (b instanceof BlockDto.Heading h) {
        chunkId = flush(doc.id(), headingPath, buf, out, chunkId);
        buf.setLength(0);
        headingPath = h.text();
      } else if (b instanceof BlockDto.Paragraph p) {
        buf.append(p.text()).append("\n\n");
      } else if (b instanceof BlockDto.BulletedList ul) {
        for (String item : ul.items()) buf.append("- ").append(item).append("\n");
        buf.append("\n");
      } else if (b instanceof BlockDto.Code c) {
        buf.append(c.text()).append("\n\n");
      }
    }
    flush(doc.id(), headingPath, buf, out, chunkId);
    return out;
  }

  int flush(String docId, String headingPath, StringBuilder buf, List<ChunkDto> out, int chunkIdStart) {
    String text = buf.toString().strip();
    if (text.isBlank()) return chunkIdStart;

    int maxChars = 900;
    int overlap = 180;

    int i = 0;
    int chunkId = chunkIdStart;
    while (i < text.length()) {
      int end = Math.min(text.length(), i + maxChars);
      String slice = text.substring(i, end).strip();
      if (!slice.isBlank()) out.add(new ChunkDto(docId, chunkId++, headingPath, slice));
      if (end == text.length()) break;
      i = Math.max(0, end - overlap);
    }
    return chunkId;
  }
}
