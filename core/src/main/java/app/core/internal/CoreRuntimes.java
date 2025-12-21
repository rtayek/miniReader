package app.core.internal;

import app.core.MiniReaderConfig;
import app.core.MiniReaderException;

import java.io.IOException;

public final class CoreRuntimes {
  private CoreRuntimes() {}

  public static CoreRuntime create(MiniReaderConfig config) throws MiniReaderException {
    try {
      return new CoreRuntime(config);
    } catch (IOException e) {
      throw new MiniReaderException("Failed to initialize core", e);
    }
  }
}
