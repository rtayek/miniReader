package app.core.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShellHeuristicsTest {
  @Test
  void detectsTypicalShellStrings() {
    assertTrue(ShellHeuristics.looksLikeJsShell("Enable JavaScript to continue"));
    assertTrue(ShellHeuristics.looksLikeJsShell("loading"));
    assertTrue(ShellHeuristics.looksLikeJsShell("   "));
  }

  @Test
  void ignoresNormalContent() {
    assertFalse(ShellHeuristics.looksLikeJsShell("Short page with real content like Hello World"));
    assertFalse(ShellHeuristics.looksLikeJsShell("This is a normal page with enough text to pass the length check."));
  }
}
