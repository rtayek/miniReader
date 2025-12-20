package app.core;

import java.util.List;
import java.util.Objects;

public record AnswerDto(String summary, List<String> bullets, List<CitationDto> citations) {
  public AnswerDto {
    Objects.requireNonNull(summary, "summary");
    bullets = List.copyOf(bullets == null ? List.of() : bullets);
    citations = List.copyOf(citations == null ? List.of() : citations);
  }
}
