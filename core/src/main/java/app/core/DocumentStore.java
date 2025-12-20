package app.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

  java.util.List<Path> list() throws IOException {
    Files.createDirectories(config.docsDir());
    try (var s = Files.list(config.docsDir())) {
      return s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
    }
  }

  private final ObjectMapper mapper;
  private final MiniReaderConfig config;
}
