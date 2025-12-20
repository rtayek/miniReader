package app.core;

import java.nio.file.Path;

class TestPathsConfig extends PathsConfig {
  TestPathsConfig(Path baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  Path baseDir() {
    return baseDir;
  }

  @Override
  Path docsDir() {
    return baseDir.resolve("docs");
  }

  @Override
  Path indexDir() {
    return baseDir.resolve("index");
  }

  private final Path baseDir;
}
