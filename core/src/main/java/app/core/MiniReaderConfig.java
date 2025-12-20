package app.core;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Configuration for MiniReader core.
 *
 * Only exposes public inputs consumers may want to tweak (base directory).
 * Directories derived from the base are used for saved docs and the Lucene index.
 */
public record MiniReaderConfig(Path baseDir) {
  public MiniReaderConfig {
    Objects.requireNonNull(baseDir, "baseDir");
  }

  public static MiniReaderConfig defaults() {
    return new MiniReaderConfig(Path.of(System.getProperty("user.home"), ".miniReader"));
  }

  public Path docsDir() {
    return baseDir.resolve("docs");
  }

  public Path indexDir() {
    return baseDir.resolve("index");
  }
}
