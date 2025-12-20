package app.core;

import java.time.Instant;
import java.util.List;

public record DocumentDto(
    String id,
    String url,
    String title,
    Instant fetchedAt,
    List<BlockDto> blocks,
    List<LinkDto> links,
    String plainText
) {}
