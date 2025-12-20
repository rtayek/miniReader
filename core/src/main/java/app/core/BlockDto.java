package app.core;

public sealed interface BlockDto permits BlockDto.Heading, BlockDto.Paragraph, BlockDto.BulletedList, BlockDto.Code {
  record Heading(int level, String text) implements BlockDto {}
  record Paragraph(String text) implements BlockDto {}
  record BulletedList(java.util.List<String> items) implements BlockDto {}
  record Code(String text) implements BlockDto {}
}
