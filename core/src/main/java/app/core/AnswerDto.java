package app.core;

import java.util.List;
import java.util.Objects;

public record AnswerDto(String summary, List<String> bullets, List<CitationDto> citations) {
  public AnswerDto {
    Objects.requireNonNull(summary, "summary");
    bullets = List.copyOf(bullets == null ? List.of() : bullets);
    citations = List.copyOf(citations == null ? List.of() : citations);
  }

  public record CitationDto(String title, String url, String headingPath, int chunkId, String snippet) {
    public CitationDto {
      Objects.requireNonNull(title, "title");
      Objects.requireNonNull(url, "url");
      Objects.requireNonNull(snippet, "snippet");
    }
  }
}
