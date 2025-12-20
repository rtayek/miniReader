package app.core;

import java.util.Objects;

public record CitationDto(String title, String url, String headingPath, int chunkId, String snippet) {
  public CitationDto {
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(url, "url");
    Objects.requireNonNull(snippet, "snippet");
  }
}
