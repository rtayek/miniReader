package app.core;

import java.nio.file.Path;

class PathsConfig {
  Path baseDir() {
    return Path.of(System.getProperty("user.home"), ".miniReader");
  }

  Path docsDir() {
    return baseDir().resolve("docs");
  }

  Path indexDir() {
    return baseDir().resolve("index");
  }
}
