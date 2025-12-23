package app.core.internal;

import app.core.MiniReaderConfig;
import app.core.MiniReaderException;

import java.io.IOException;

/**
 * Test helper to create a CoreRuntime-like object with minimal setup.
 * Uses a temp base dir and the defaults for other settings.
 */
class CoreTestRuntimeFactory {
  static CoreRuntime runtime() {
    try {
      return new CoreRuntime(MiniReaderConfig.fromBaseDir(java.nio.file.Files.createTempDirectory("mr-test")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
