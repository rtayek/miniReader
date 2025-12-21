package app.core.internal;

import app.core.DocumentDto;
import app.core.MiniReaderConfig;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class LuceneIndex implements AutoCloseable {
  private static final String FIELD_DOC_ID = "docId";
  private static final String FIELD_CHUNK_ID = "chunkId";
  private static final String FIELD_HEADING = "headingPath";
  private static final String FIELD_URL = "url";
  private static final String FIELD_TITLE_STORED = "title";
  private static final String FIELD_TEXT = "text";
  private static final String FIELD_TITLE_SEARCH = "title";
  private static final String FIELD_HEADING_SEARCH = "heading";

  public LuceneIndex(MiniReaderConfig config) throws IOException {
    this.analyzer = new StandardAnalyzer();
    Files.createDirectories(config.indexDir());
    this.directory = FSDirectory.open(config.indexDir());

    IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
    cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    this.writer = new IndexWriter(directory, cfg);
  }

  public void index(DocumentDto doc, List<ChunkDto> chunks) throws IOException {
    deleteDoc(doc.id());

    for (ChunkDto c : chunks) {
      Document d = new Document();
      d.add(new StringField(FIELD_DOC_ID, c.docId(), Field.Store.YES));
      d.add(new StoredField(FIELD_CHUNK_ID, c.chunkId()));
      d.add(new StoredField(FIELD_HEADING, c.headingPath() == null ? "" : c.headingPath()));
      d.add(new StoredField(FIELD_URL, doc.url()));
      d.add(new StoredField(FIELD_TITLE_STORED, doc.title()));
      d.add(new TextField(FIELD_TEXT, c.text(), Field.Store.YES));
      d.add(new TextField(FIELD_TITLE_SEARCH, doc.title() == null ? "" : doc.title(), Field.Store.NO));
      d.add(new TextField(FIELD_HEADING_SEARCH, c.headingPath() == null ? "" : c.headingPath(), Field.Store.NO));
      writer.addDocument(d);
    }
    writer.commit();
  }

  public void deleteDoc(String docId) throws IOException {
    writer.deleteDocuments(new Term(FIELD_DOC_ID, docId));
    writer.commit();
  }

  public List<SearchHit> search(String query, int limit) throws IOException, ParseException {
    writer.commit();
    try (DirectoryReader reader = DirectoryReader.open(writer)) {
      IndexSearcher searcher = new IndexSearcher(reader);
      QueryParser parser = buildQueryParser();
      Query q = parser.parse(QueryParser.escape(query));

      TopDocs docs = searcher.search(q, limit);
      List<SearchHit> hits = new ArrayList<>();
      for (ScoreDoc sd : docs.scoreDocs) {
        Document d = searcher.doc(sd.doc);
        hits.add(new SearchHit(
            d.get(FIELD_DOC_ID),
            Integer.parseInt(d.get(FIELD_CHUNK_ID)),
            d.get(FIELD_TITLE_STORED),
            d.get(FIELD_URL),
            d.get(FIELD_HEADING),
            d.get(FIELD_TEXT),
            sd.score
        ));
      }
      return hits;
    }
  }

  private QueryParser buildQueryParser() {
    return new MultiFieldQueryParser(new String[] { FIELD_TEXT, FIELD_TITLE_SEARCH, FIELD_HEADING_SEARCH }, analyzer);
  }

  @Override
  public void close() throws IOException {
    writer.close();
    directory.close();
    analyzer.close();
  }

  public record SearchHit(String docId, int chunkId, String title, String url, String headingPath, String text, float score) {}

  private final Analyzer analyzer;
  private final FSDirectory directory;
  private final IndexWriter writer;
}
