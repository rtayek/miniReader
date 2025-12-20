package app.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//import app.core.BlockDto.*;

class DocumentStoreTest {
  @TempDir
  Path tempDir;

  @Test
  void saveLoad_roundTrip() throws Exception {
    PathsConfig paths = new TestPathsConfig(tempDir);
    DocumentStore store = new DocumentStore(paths);

    DocumentDto doc = new DocumentDto(
        "docX",
        "https://example.com/x",
        "Title X",
        Instant.now(),
        List.of(new BlockDto.Heading(1, "H"), new BlockDto.Paragraph("P")),
        List.of(new LinkDto("L", "https://example.com/l")),
        "plain"
    );

    store.save(doc);

    List<Path> files = store.list();
    assertEquals(1, files.size());

    DocumentDto loaded = store.load(files.getFirst());
    assertEquals(doc.id(), loaded.id());
    assertEquals(doc.url(), loaded.url());
    assertEquals(doc.title(), loaded.title());
    assertEquals(doc.plainText(), loaded.plainText());
    assertEquals(1, loaded.links().size());
  }
}
