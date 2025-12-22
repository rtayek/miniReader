package app.core.internal;

import app.core.CoreFacade;
import app.core.DocumentDto;
import app.core.MiniReaderConfig;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class CoreFacadeShellDetectionTest {
  @Test
  void looksLikeJsShell_shouldDetectFixture() throws Exception {
    String html = readFixture("fixtures/js_shell.html");
    FetchResult fetch = new FetchResult("https://example.com/app", 200, "text/html", html);
    Extractor extractor = new Extractor();
    DocumentDto doc = extractor.extract(fetch);

    assertTrue(ShellHeuristics.looksLikeJsShell(doc.plainText()));
  }

  String readFixture(String name) throws Exception {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
      assertNotNull(in, "missing test resource: " + name);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
