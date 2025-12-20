package app.ui;

import app.core.AnswerDto;
import app.core.CoreFacade;
import app.core.DocumentDto;
import app.core.MiniReaderException;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

class MiniReaderFrame extends JFrame {
  MiniReaderFrame() throws Exception {
    super("MiniReader");
    this.core = new CoreFacade();

    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setSize(1200, 800);

    JPanel top = new JPanel(new BorderLayout(8, 8));
    urlField = new JTextField();
    fetchButton = new JButton("Fetch");
    statusLabel = new JLabel("Ready.");

    top.add(urlField, BorderLayout.CENTER);
    top.add(fetchButton, BorderLayout.EAST);
    top.add(statusLabel, BorderLayout.SOUTH);

    docListModel = new DefaultListModel<>();
    docList = new JList<>(docListModel);
    docList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    readerArea = new JTextArea();
    readerArea.setEditable(false);
    readerArea.setLineWrap(true);
    readerArea.setWrapStyleWord(true);

    chatArea = new JTextArea();
    chatArea.setEditable(false);
    chatArea.setLineWrap(true);
    chatArea.setWrapStyleWord(true);

    chatInput = new JTextField();
    askButton = new JButton("Ask");

    JPanel chatBottom = new JPanel(new BorderLayout(8, 8));
    chatBottom.add(chatInput, BorderLayout.CENTER);
    chatBottom.add(askButton, BorderLayout.EAST);

    JPanel chatPanel = new JPanel(new BorderLayout(8, 8));
    chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
    chatPanel.add(chatBottom, BorderLayout.SOUTH);

    JSplitPane leftCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(docList), new JScrollPane(readerArea));
    leftCenter.setResizeWeight(0.2);

    JSplitPane centerRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCenter, chatPanel);
    centerRight.setResizeWeight(0.65);

    getContentPane().setLayout(new BorderLayout(8, 8));
    getContentPane().add(top, BorderLayout.NORTH);
    getContentPane().add(centerRight, BorderLayout.CENTER);

    wireActions();
    refreshDocList();
  }

  void wireActions() {
    fetchButton.addActionListener(e -> ingestUrl());
    urlField.addActionListener(e -> ingestUrl());

    docList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) loadSelectedDoc();
    });

    askButton.addActionListener(e -> ask());
    chatInput.addActionListener(e -> ask());
  }

  void ingestUrl() {
    String url = urlField.getText().strip();
    if (url.isBlank()) return;

    fetchButton.setEnabled(false);
    statusLabel.setText("Fetching…");

    new SwingWorker<CoreFacade.IngestResult, Void>() {
      @Override
      protected CoreFacade.IngestResult doInBackground() throws Exception {
        return core.ingestUrl(url);
      }

      @Override
      protected void done() {
        try {
          CoreFacade.IngestResult r = get();
          statusLabel.setText(r.message());
          if (r.doc() != null) {
            readerArea.setText(r.doc().plainText());
            readerArea.setCaretPosition(0);
          }
          refreshDocList();
        } catch (ExecutionException ex) {
          Throwable cause = ex.getCause();
          String msg = cause instanceof MiniReaderException ? cause.getMessage() : ex.getMessage();
          statusLabel.setText("Error: " + msg);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          statusLabel.setText("Interrupted.");
        } finally {
          fetchButton.setEnabled(true);
        }
      }
    }.execute();
  }

  void refreshDocList() {
    new SwingWorker<java.util.List<Path>, Void>() {
      @Override
      protected java.util.List<Path> doInBackground() throws Exception {
        return core.listSavedDocs();
      }

      @Override
      protected void done() {
        try {
          java.util.List<Path> docs = get();
          docListModel.clear();
          for (Path p : docs) docListModel.addElement(p);
          if (!docs.isEmpty() && docList.getSelectedIndex() < 0) docList.setSelectedIndex(docs.size() - 1);
        } catch (ExecutionException ex) {
          Throwable cause = ex.getCause();
          String msg = cause instanceof MiniReaderException ? cause.getMessage() : ex.getMessage();
          statusLabel.setText("Error listing docs: " + msg);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          statusLabel.setText("Interrupted.");
        }
      }
    }.execute();
  }

  void loadSelectedDoc() {
    Path p = docList.getSelectedValue();
    if (p == null) return;

    new SwingWorker<DocumentDto, Void>() {
      @Override
      protected DocumentDto doInBackground() throws Exception {
        return core.loadSavedDoc(p);
      }

      @Override
      protected void done() {
        try {
          DocumentDto doc = get();
          readerArea.setText(doc.plainText());
          readerArea.setCaretPosition(0);
          statusLabel.setText("Loaded: " + doc.title());
        } catch (ExecutionException ex) {
          Throwable cause = ex.getCause();
          String msg = cause instanceof MiniReaderException ? cause.getMessage() : ex.getMessage();
          statusLabel.setText("Error loading doc: " + msg);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          statusLabel.setText("Interrupted.");
        }
      }
    }.execute();
  }

  void ask() {
    String q = chatInput.getText().strip();
    if (q.isBlank()) return;

    chatInput.setText("");
    appendChat("\nYou: " + q + "\n");

    askButton.setEnabled(false);

    new SwingWorker<AnswerDto, Void>() {
      @Override
      protected AnswerDto doInBackground() throws Exception {
        return core.ask(q);
      }

      @Override
      protected void done() {
        try {
          AnswerDto a = get();
          appendChat("MiniReader:\n" + a.summary() + "\n");
          if (!a.citations().isEmpty()) {
            appendChat("\nCitations:\n");
            for (var c : a.citations()) {
              String hp = c.headingPath() == null || c.headingPath().isBlank() ? "" : (" — " + c.headingPath());
              appendChat("- " + c.title() + hp + " (chunk " + c.chunkId() + ")\n  " + c.url() + "\n  “" + c.snippet() + "”\n");
            }
          }
          appendChat("\n");
        } catch (ExecutionException ex) {
          Throwable cause = ex.getCause();
          String msg = cause instanceof MiniReaderException ? cause.getMessage() : ex.getMessage();
          appendChat("MiniReader: Error: " + msg + "\n\n");
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          appendChat("MiniReader: Interrupted.\n\n");
        } finally {
          askButton.setEnabled(true);
        }
      }
    }.execute();
  }

  void appendChat(String s) {
    chatArea.append(s);
    chatArea.setCaretPosition(chatArea.getDocument().getLength());
  }

  @Override
  public void dispose() {
    try {
      core.close();
    } catch (MiniReaderException ignored) {
    }
    super.dispose();
  }

  private final CoreFacade core;

  private JButton askButton;
  private JButton fetchButton;
  private DefaultListModel<Path> docListModel;
  private JLabel statusLabel;
  private JList<Path> docList;
  private JTextArea chatArea;
  private JTextArea readerArea;
  private JTextField chatInput;
  private JTextField urlField;
}
