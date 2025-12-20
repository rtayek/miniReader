package app.core;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

class LuceneIndex implements AutoCloseable {
  LuceneIndex(MiniReaderConfig config) throws IOException {
    this.analyzer = new StandardAnalyzer();
    Files.createDirectories(config.indexDir());
    this.directory = FSDirectory.open(config.indexDir());

    IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
    cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    this.writer = new IndexWriter(directory, cfg);
  }

  void index(DocumentDto doc, List<ChunkDto> chunks) throws IOException {
    deleteDoc(doc.id());

    for (ChunkDto c : chunks) {
      Document d = new Document();
      d.add(new StringField("docId", c.docId(), Field.Store.YES));
      d.add(new StoredField("chunkId", c.chunkId()));
      d.add(new StoredField("headingPath", c.headingPath() == null ? "" : c.headingPath()));
      d.add(new StoredField("url", doc.url()));
      d.add(new StoredField("title", doc.title()));
      d.add(new TextField("text", c.text(), Field.Store.YES));
      d.add(new TextField("title", doc.title() == null ? "" : doc.title(), Field.Store.NO));
      d.add(new TextField("heading", c.headingPath() == null ? "" : c.headingPath(), Field.Store.NO));
      writer.addDocument(d);
    }
    writer.commit();
  }

  void deleteDoc(String docId) throws IOException {
    writer.deleteDocuments(new Term("docId", docId));
    writer.commit();
  }

  List<SearchHit> search(String query, int limit) throws IOException, ParseException {
    writer.commit();
    try (DirectoryReader reader = DirectoryReader.open(writer)) {
      IndexSearcher searcher = new IndexSearcher(reader);
      QueryParser parser = new MultiFieldQueryParser(new String[] { "text", "title", "heading" }, analyzer);
      Query q = parser.parse(QueryParser.escape(query));

      TopDocs docs = searcher.search(q, limit);
      List<SearchHit> hits = new ArrayList<>();
      for (ScoreDoc sd : docs.scoreDocs) {
        Document d = searcher.doc(sd.doc);
        hits.add(new SearchHit(
            d.get("docId"),
            Integer.parseInt(d.get("chunkId")),
            d.get("title"),
            d.get("url"),
            d.get("headingPath"),
            d.get("text"),
            sd.score
        ));
      }
      return hits;
    }
  }

  @Override
  public void close() throws IOException {
    writer.close();
    directory.close();
    analyzer.close();
  }

  record SearchHit(String docId, int chunkId, String title, String url, String headingPath, String text, float score) {}

  private final Analyzer analyzer;
  private final FSDirectory directory;
  private final IndexWriter writer;
}
