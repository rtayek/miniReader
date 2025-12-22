package app.core.internal;

import app.core.DocumentDto;
import app.core.MiniReaderConfig;
import app.core.SavedDocDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

class DocumentStore {
  DocumentStore(MiniReaderConfig config) {
    this.config = config;
    mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  void save(DocumentDto doc) throws IOException {
    Files.createDirectories(config.docsDir());
    Path p = config.docsDir().resolve(doc.id() + ".json");
    mapper.writeValue(p.toFile(), doc);
  }

  DocumentDto load(Path file) throws IOException {
    return mapper.readValue(file.toFile(), DocumentDto.class);
  }

  DocumentDto load(String id) throws IOException {
    return load(pathForId(id));
  }

  List<SavedDocDto> list() throws IOException {
    Files.createDirectories(config.docsDir());
    try (var s = Files.list(config.docsDir())) {
      return s
          .filter(p -> p.getFileName().toString().endsWith(".json"))
          .sorted()
          .map(this::toSavedDoc)
          .sorted(Comparator.comparing(SavedDocDto::fetchedAt).reversed())
          .toList();
    }
  }

  Path pathForId(String id) {
    return config.docsDir().resolve(id + ".json");
  }

  private SavedDocDto toSavedDoc(Path path) {
    try {
      DocumentDto dto = load(path);
      Instant fetched = dto.fetchedAt() == null ? Instant.EPOCH : dto.fetchedAt();
      return new SavedDocDto(dto.id(), dto.title(), dto.url(), fetched);
    } catch (IOException e) {
      return new SavedDocDto(stripExt(path.getFileName().toString()), path.toString(), "", Instant.EPOCH);
    }
  }

  private static String stripExt(String name) {
    int i = name.lastIndexOf('.');
    return i > 0 ? name.substring(0, i) : name;
  }

  private final ObjectMapper mapper;
  private final MiniReaderConfig config;
}
