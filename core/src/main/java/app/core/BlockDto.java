package app.core;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = BlockDto.Heading.class, name = "heading"),
    @JsonSubTypes.Type(value = BlockDto.Paragraph.class, name = "paragraph"),
    @JsonSubTypes.Type(value = BlockDto.Code.class, name = "code"),
    @JsonSubTypes.Type(value = BlockDto.BulletedList.class, name = "bulletedList")
})
sealed interface BlockDto
    permits BlockDto.Heading,
            BlockDto.Paragraph,
            BlockDto.Code,
            BlockDto.BulletedList {

    /* ========= BLOCK TYPES ========= */

    /**
     * A document heading.
     *
     * Arguments:
     *   level — heading level (1 = h1, 2 = h2, etc.)
     *   text  — heading text
     */
    record Heading(int level, String text) implements BlockDto {}

    /**
     * A normal paragraph.
     *
     * Arguments:
     *   text — paragraph content
     */
    record Paragraph(String text) implements BlockDto {}

    /**
     * A code block.
     *
     * Arguments:
     *   text — code content (already formatted)
     */
    record Code(String text) implements BlockDto {}

    /**
     * A bulleted list.
     *
     * Arguments:
     *   items — list of bullet strings
     */
    record BulletedList(List<String> items) implements BlockDto {}
}
