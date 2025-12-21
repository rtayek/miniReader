package app.core;

import app.core.internal.Extractor;
import app.core.internal.FetchResult;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ExtractorTest {
  @Test
  void extract_shouldProduceBlocksAndPlainText() {
    String html =
        "<html>\n" +
        "  <head><title>t</title></head>\n" +
        "  <body>\n" +
        "    <h1>Header</h1>\n" +
        "    <p>First paragraph.</p>\n" +
        "    <ul><li>One</li><li>Two</li></ul>\n" +
        "    <pre>code line 1\ncode line 2</pre>\n" +
        "    <a href=\"https://example.com/x\">Link</a>\n" +
        "  </body>\n" +
        "</html>\n";

    FetchResult fetch = new FetchResult("https://example.com/page", 200, "text/html", html);
    Extractor extractor = new Extractor();

    DocumentDto doc = extractor.extract(fetch);

    assertEquals("t", doc.title());
    assertEquals("https://example.com/page", doc.url());
    assertNotNull(doc.id());
    assertFalse(doc.id().isBlank());

    assertFalse(doc.blocks().isEmpty());
    assertTrue(doc.plainText().contains("Header"));
    assertTrue(doc.plainText().contains("First paragraph."));
    assertTrue(doc.plainText().contains("• One"));

    assertTrue(doc.links().stream().anyMatch(l -> l.url().equals("https://example.com/x")));
  }

  @Test
  void extract_fixture_shouldPreferMeaningfulContentOverJunk() throws Exception {
    String html = readFixture("fixtures/article_with_junk.html");
    FetchResult fetch = new FetchResult("https://example.com/article", 200, "text/html", html);

    Extractor extractor = new Extractor();
    DocumentDto doc = extractor.extract(fetch);

    String t = doc.plainText();
    assertTrue(t.contains("The Meaningful Content"));
    assertTrue(t.contains("The extractor should keep this section"));
    assertTrue(t.contains("Keep lists"));

    assertFalse(t.toLowerCase().contains("advertisement"));
    assertFalse(t.toLowerCase().contains("cookie consent"));
    assertFalse(t.toLowerCase().contains("subscribe now"));
  }

  @Test
  void extract_fixture_jsShell_shouldStaySmall() throws Exception {
    String html = readFixture("fixtures/js_shell.html");
    FetchResult fetch = new FetchResult("https://example.com/app", 200, "text/html", html);

    Extractor extractor = new Extractor();
    DocumentDto doc = extractor.extract(fetch);

    assertTrue(doc.plainText().length() < 250);
  }

  String readFixture(String name) throws Exception {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
      assertNotNull(in, "missing test resource: " + name);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
