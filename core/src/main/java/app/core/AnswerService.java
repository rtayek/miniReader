package app.core;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AnswerService {
  public AnswerService(LuceneIndex index) {
    this.index = index;
  }

  public Answer answer(String question) throws Exception {
    List<LuceneIndex.SearchHit> hits = index.search(question, 8);
    if (hits.isEmpty()) {
      return new Answer("No matches found in your local library.", List.of(), List.of());
    }

    List<LuceneIndex.SearchHit> top = hits.stream()
        .sorted(Comparator.comparing(LuceneIndex.SearchHit::score).reversed())
        .toList();

    List<String> bullets = top.stream()
        .limit(4)
        .map(h -> bestSentence(h.text()))
        .filter(s -> !s.isBlank())
        .distinct()
        .limit(6)
        .toList();

    List<Citation> cites = top.stream()
        .limit(6)
        .map(h -> new Citation(h.title(), h.url(), h.headingPath(), h.chunkId(), snippet(h.text())))
        .toList();

    String summary = bullets.isEmpty()
        ? "Top matching passages found. See citations."
        : bullets.stream().map(b -> "• " + b).collect(Collectors.joining("\n"));

    return new Answer(summary, bullets, cites);
  }

  String bestSentence(String text) {
    String t = text == null ? "" : text.strip();
    if (t.isBlank()) return "";

    String[] parts = t.split("(?<=[.!?])\\s+");
    for (String p : parts) {
      String s = p.strip();
      if (s.length() >= 60 && s.length() <= 220) return s;
    }
    return t.length() <= 220 ? t : t.substring(0, 220).strip() + "…";
  }

  String snippet(String text) {
    String t = text == null ? "" : text.strip();
    if (t.length() <= 280) return t;
    return t.substring(0, 280).strip() + "…";
  }

  public record Answer(String summary, List<String> bullets, List<Citation> citations) {}
  public record Citation(String title, String url, String headingPath, int chunkId, String snippet) {}

  private final LuceneIndex index;
}
