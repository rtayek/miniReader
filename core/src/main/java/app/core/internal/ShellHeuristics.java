package app.core.internal;

final class ShellHeuristics {
  private ShellHeuristics() {}

  static boolean looksLikeJsShell(String plainText) {
    String t = plainText == null ? "" : plainText.strip();
    if (t.length() >= JS_SHELL_MAX_LENGTH) return false;
    String lower = t.toLowerCase();
    return lower.contains("enable javascript")
        || lower.contains("loading")
        || lower.isBlank();
  }

  private static final int JS_SHELL_MAX_LENGTH = 250;
}
