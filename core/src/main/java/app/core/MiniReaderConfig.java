package app.core;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for MiniReader core.
 *
 * Keep this small and only expose knobs that avoid code edits.
 */
public record MiniReaderConfig(
    Path baseDir,
    Duration connectTimeout,
    Duration requestTimeout,
    String userAgent,
    int chunkMaxChars,
    int chunkOverlapChars
) {
  public MiniReaderConfig {
    Objects.requireNonNull(baseDir, "baseDir");
    Objects.requireNonNull(connectTimeout, "connectTimeout");
    Objects.requireNonNull(requestTimeout, "requestTimeout");
    Objects.requireNonNull(userAgent, "userAgent");
    if (chunkMaxChars <= 0) throw new IllegalArgumentException("chunkMaxChars must be > 0");
    if (chunkOverlapChars < 0) throw new IllegalArgumentException("chunkOverlapChars must be >= 0");
    if (chunkOverlapChars >= chunkMaxChars) throw new IllegalArgumentException("chunkOverlapChars must be < chunkMaxChars");
  }

  public static MiniReaderConfig defaults() {
    return new MiniReaderConfig(
        Path.of(System.getProperty("user.home"), ".miniReader"),
        Duration.ofSeconds(12),
        Duration.ofSeconds(20),
        "MiniReader/0.1 (+local reader)",
        900,
        180
    );
  }

  public static MiniReaderConfig fromBaseDir(Path baseDir) {
    MiniReaderConfig d = defaults();
    return new MiniReaderConfig(
        baseDir,
        d.connectTimeout(),
        d.requestTimeout(),
        d.userAgent(),
        d.chunkMaxChars(),
        d.chunkOverlapChars()
    );
  }

  public Path docsDir() {
    return baseDir.resolve("docs");
  }

  public Path indexDir() {
    return baseDir.resolve("index");
  }
}
