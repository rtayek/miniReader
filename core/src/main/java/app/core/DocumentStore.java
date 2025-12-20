package app.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class DocumentStore {
	DocumentStore(PathsConfig paths) {
		  this.paths = paths;
		  mapper = new ObjectMapper()
		      .registerModule(new JavaTimeModule())
		      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		}


  void save(DocumentDto doc) throws IOException {
    Files.createDirectories(paths.docsDir());
    Path p = paths.docsDir().resolve(doc.id() + ".json");
    mapper.writeValue(p.toFile(), doc);
  }

  DocumentDto load(Path file) throws IOException {
    return mapper.readValue(file.toFile(), DocumentDto.class);
  }

  java.util.List<Path> list() throws IOException {
    Files.createDirectories(paths.docsDir());
    try (var s = Files.list(paths.docsDir())) {
      return s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
    }
  }

  private final ObjectMapper mapper;
  private final PathsConfig paths;
}
