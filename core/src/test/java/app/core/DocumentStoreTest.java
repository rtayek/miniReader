package app.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    MiniReaderConfig config = MiniReaderConfig.fromBaseDir(tempDir);
    DocumentStore store = new DocumentStore(config);

    DocumentDto doc = new DocumentDto(
        "docX",
        "https://example.com/x",
        "Title X",
        Instant.EPOCH,
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

  @Test
  void save_matchesGoldenFormat() throws Exception {
    MiniReaderConfig config = MiniReaderConfig.fromBaseDir(tempDir);
    DocumentStore store = new DocumentStore(config);

    DocumentDto doc = new DocumentDto(
        "docX",
        "https://example.com/x",
        "Title X",
        Instant.parse("2023-01-02T03:04:05Z"),
        List.of(
            new BlockDto.Heading(1, "Heading 1"),
            new BlockDto.Paragraph("Paragraph text."),
            new BlockDto.Code("code block"),
            new BlockDto.BulletedList(List.of("One", "Two"))
        ),
        List.of(new LinkDto("Example", "https://example.com/link")),
        "plain text"
    );

    store.save(doc);

    Path saved = config.docsDir().resolve("docX.json");
    String actual = Files.readString(saved).trim();
    String expected = readResource("document_dto.json").trim();

    assertEquals(expected, actual);
  }

  private String readResource(String name) throws Exception {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
      assertNotNull(in, "missing test resource: " + name);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
