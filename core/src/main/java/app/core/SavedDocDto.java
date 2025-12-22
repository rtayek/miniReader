package app.core;

import java.time.Instant;
import java.util.Objects;

public record SavedDocDto(String id, String title, String url, Instant fetchedAt) {
  public SavedDocDto {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(url, "url");
    Objects.requireNonNull(fetchedAt, "fetchedAt");
  }

  @Override
  public String toString() {
    return title + " — " + url;
  }
}
