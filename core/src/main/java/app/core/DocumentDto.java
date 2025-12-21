package app.core;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record DocumentDto(
    String id,
    String url,
    String title,
    Instant fetchedAt,
    List<BlockDto> blocks,
    List<LinkDto> links,
    String plainText
) {
  public DocumentDto {
    Objects.requireNonNull(blocks, "blocks");
    Objects.requireNonNull(links, "links");
    blocks = List.copyOf(blocks);
    links = List.copyOf(links);
  }
}
