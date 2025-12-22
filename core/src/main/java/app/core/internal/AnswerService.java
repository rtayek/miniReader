package app.core.internal;

import app.core.AnswerDto;
import app.core.CitationDto;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class AnswerService {
  AnswerService(LuceneIndex index) {
    this.index = index;
  }

  AnswerDto answer(String question) throws IOException, ParseException {
    List<LuceneIndex.SearchHit> hits = index.search(question, 8);
    if (hits.isEmpty()) {
      return new AnswerDto("No matches found in your local library.", List.of(), List.of());
    }

    List<LuceneIndex.SearchHit> top = hits.stream()
        .sorted(Comparator.comparing(LuceneIndex.SearchHit::score).reversed())
        .toList();

    List<String> bullets = top.stream()
        .limit(MAX_BULLETS)
        .map(h -> bestSentence(h.text()))
        .filter(s -> !s.isBlank())
        .distinct()
        .limit(MAX_BULLETS + 2)
        .toList();

    List<CitationDto> cites = top.stream()
        .limit(MAX_CITATIONS)
        .map(h -> new CitationDto(h.title(), h.url(), h.headingPath(), h.chunkId(), snippet(h.text())))
        .toList();

    String summary = bullets.isEmpty()
        ? "Top matching passages found. See citations."
        : bullets.stream().map(b -> "• " + b).collect(Collectors.joining("\n"));

    return new AnswerDto(summary, bullets, cites);
  }

  private String bestSentence(String text) {
    String t = text == null ? "" : text.strip();
    if (t.isBlank()) return "";

    String[] parts = t.split("(?<=[.!?])\\s+");
    for (String p : parts) {
      String s = p.strip();
      if (s.length() >= BULLET_MIN && s.length() <= BULLET_MAX) return s;
    }
    return t.length() <= BULLET_MAX ? t : t.substring(0, BULLET_MAX).strip() + "…";
  }

  private String snippet(String text) {
    String t = text == null ? "" : text.strip();
    if (t.length() <= SNIPPET_MAX) return t;
    return t.substring(0, SNIPPET_MAX).strip() + "…";
  }

  private final LuceneIndex index;

  private static final int BULLET_MIN = 60;
  private static final int BULLET_MAX = 220;
  private static final int SNIPPET_MAX = 280;
  private static final int MAX_BULLETS = 4;
  private static final int MAX_CITATIONS = 6;
}
