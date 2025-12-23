package app.core.internal;

import app.core.IngestOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FetchValidationTest {
  private final CoreRuntime runtime = CoreTestRuntimeFactory.runtime();

  @Test
  void validateFetch_shouldAcceptHtmlAndXhtml() {
    FetchResult html = new FetchResult("u", 200, "text/html", "<html></html>");
    FetchResult xhtml = new FetchResult("u", 200, "application/xhtml+xml", "<html></html>");

    assertNull(runtime.validateFetch(html));
    assertNull(runtime.validateFetch(xhtml));
  }

  @Test
  void validateFetch_shouldRejectNonHtml() {
    FetchResult json = new FetchResult("u", 200, "application/json", "{}");
    IngestOutcome outcome = runtime.validateFetch(json);
    assertTrue(outcome instanceof IngestOutcome.RejectedNonHtml);
  }
}
