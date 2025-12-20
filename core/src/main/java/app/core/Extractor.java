package app.core;

import net.dankito.readability4j.Readability4J;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

class Extractor {
  DocumentDto extract(FetchResult fetch) {
    String canonicalUrl = canonicalizeUrl(fetch.url());

    Document jsoupDoc = Jsoup.parse(fetch.body(), canonicalUrl);
    jsoupDoc.select("script,style,noscript").remove();

    String title = jsoupDoc.title();
    List<LinkDto> links = extractLinks(jsoupDoc);

    String mainHtml = tryReadability(canonicalUrl, fetch.body());
    if (mainHtml == null || mainHtml.isBlank()) {
      mainHtml = jsoupDoc.body() == null ? "" : jsoupDoc.body().html();
    }

    Document mainDoc = Jsoup.parse(mainHtml, canonicalUrl);
    mainDoc.select("script,style,noscript").remove();

    List<BlockDto> blocks = new ArrayList<>();
    extractBlocks(mainDoc, blocks);

    String plainText = blocksToPlainText(blocks);
    String id = stableId(canonicalUrl);
    return new DocumentDto(id, canonicalUrl, title == null ? "" : title, Instant.now(), List.copyOf(blocks), List.copyOf(links), plainText);
  }

  String tryReadability(String url, String html) {
    try {
      Readability4J readability = new Readability4J(url, html);
      var article = readability.parse();
      if (article == null) return null;
      String content = article.getContent();
      if (content == null) return null;

      String text = Jsoup.parse(content).text();
      if (text.strip().length() < 400) return null;

      return content;
    } catch (Exception e) {
      return null;
    }
  }

  List<LinkDto> extractLinks(Document doc) {
    List<LinkDto> out = new ArrayList<>();
    for (Element a : doc.select("a[href]")) {
      String href = a.absUrl("href");
      if (href == null || href.isBlank()) continue;
      String text = a.text();
      if (text == null) text = "";
      out.add(new LinkDto(text.strip(), href));
    }
    return out;
  }

  void extractBlocks(Document doc, List<BlockDto> out) {
    Element body = doc.body();
    if (body == null) return;

    for (Element el : body.select("h1,h2,h3,h4,h5,h6,p,ul,pre,code")) {
      switch (el.tagName()) {
        case "h1" -> out.add(new BlockDto.Heading(1, clean(el.text())));
        case "h2" -> out.add(new BlockDto.Heading(2, clean(el.text())));
        case "h3" -> out.add(new BlockDto.Heading(3, clean(el.text())));
        case "h4" -> out.add(new BlockDto.Heading(4, clean(el.text())));
        case "h5" -> out.add(new BlockDto.Heading(5, clean(el.text())));
        case "h6" -> out.add(new BlockDto.Heading(6, clean(el.text())));
        case "p" -> {
          String t = clean(el.text());
          if (!t.isBlank()) out.add(new BlockDto.Paragraph(t));
        }
        case "ul" -> {
          List<String> items = el.select("li").eachText().stream().map(this::clean).filter(s -> !s.isBlank()).toList();
          if (!items.isEmpty()) out.add(new BlockDto.BulletedList(items));
        }
        case "pre" -> {
          String t = el.text();
          if (t != null && !t.isBlank()) out.add(new BlockDto.Code(t.strip()));
        }
        case "code" -> {
          if (!"pre".equals(el.parent() != null ? el.parent().tagName() : "")) {
            String t = el.text();
            if (t != null && t.strip().length() > 40) out.add(new BlockDto.Code(t.strip()));
          }
        }
        default -> {}
      }
    }

    if (out.isEmpty()) {
      String t = clean(body.text());
      if (!t.isBlank()) out.add(new BlockDto.Paragraph(t));
    }
  }

  String blocksToPlainText(List<BlockDto> blocks) {
    StringBuilder sb = new StringBuilder();
    for (BlockDto b : blocks) {
      b.accept(new BlockDto.BlockVisitor<Void>() {
        @Override
        public Void heading(BlockDto.Heading h) {
          sb.append("\n")
              .append("#".repeat(Math.max(1, Math.min(6, h.level()))))
              .append(" ")
              .append(h.text())
              .append("\n");
          return null;
        }

        @Override
        public Void paragraph(BlockDto.Paragraph p) {
          sb.append(p.text()).append("\n\n");
          return null;
        }

        @Override
        public Void code(BlockDto.Code c) {
          sb.append("```\n").append(c.text()).append("\n```\n\n");
          return null;
        }

        @Override
        public Void bulletedList(BlockDto.BulletedList ul) {
          for (String item : ul.items()) sb.append("• ").append(item).append("\n");
          sb.append("\n");
          return null;
        }
      });
    }
    return sb.toString().strip();
  }

  String clean(String s) {
    if (s == null) return "";
    return s.replace('\u00A0', ' ').replaceAll("\\s+", " ").strip();
  }

  private String canonicalizeUrl(String url) {
    if (url == null) return "";
    String trimmed = url.trim();
    try {
      URI u = URI.create(trimmed);
      String path = (u.getPath() == null || u.getPath().isBlank()) ? "/" : u.getPath();
      URI normalized = new URI(
          u.getScheme(),
          u.getAuthority(),
          path,
          u.getQuery(),
          null
      ).normalize();
      return normalized.toString();
    } catch (Exception e) {
      return trimmed;
    }
  }

  private String stableId(String url) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(url.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception e) {
      return url.isBlank() ? "unknown" : url;
    }
  }
}
