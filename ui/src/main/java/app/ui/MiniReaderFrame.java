package app.ui;

import app.core.AnswerDto;
import app.core.CoreFacade;
import app.core.DocumentDto;
import app.core.IngestOutcome;
import app.core.MiniReaderException;
import app.core.SavedDocDto;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
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
    if (url.isBlank()) {
      statusLabel.setText("Error: URL is empty");
      showCopyableError(this, "Fetch failed", new IllegalArgumentException("URL is empty"));
      return;
    }

    fetchButton.setEnabled(false);
    statusLabel.setText("Fetching…");
    readerArea.setText("");
    readerArea.setCaretPosition(0);

    new SwingWorker<IngestOutcome, Void>() {
      @Override
      protected IngestOutcome doInBackground() throws Exception {
        return core.ingestUrl(url);
      }

      @Override
      protected void done() {
        try {
          IngestOutcome outcome = get();
          handleIngestOutcome(outcome);
        } catch (ExecutionException ex) {
          Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
          cause.printStackTrace();
          statusLabel.setText("Error: " + cause.getMessage());
          showCopyableError(MiniReaderFrame.this, "Fetch failed", cause);
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
    new SwingWorker<java.util.List<SavedDocDto>, Void>() {
      @Override
      protected java.util.List<SavedDocDto> doInBackground() throws Exception {
        return core.listSavedDocs();
      }

      @Override
      protected void done() {
        try {
          java.util.List<SavedDocDto> docs = get();
          docListModel.clear();
          for (SavedDocDto d : docs) docListModel.addElement(d);
          if (!docs.isEmpty() && docList.getSelectedIndex() < 0) docList.setSelectedIndex(docs.size() - 1);
        } catch (ExecutionException ex) {
          Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
          cause.printStackTrace();
          statusLabel.setText("Error listing docs: " + cause.getMessage());
          showCopyableError(MiniReaderFrame.this, "List docs failed", cause);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          statusLabel.setText("Interrupted.");
        }
      }
    }.execute();
  }

  void loadSelectedDoc() {
    SavedDocDto p = docList.getSelectedValue();
    if (p == null) return;

    new SwingWorker<DocumentDto, Void>() {
      @Override
      protected DocumentDto doInBackground() throws Exception {
        return core.loadSavedDoc(p.id());
      }

      @Override
      protected void done() {
        try {
          DocumentDto doc = get();
          readerArea.setText(doc.plainText());
          readerArea.setCaretPosition(0);
          statusLabel.setText("Loaded: " + doc.title());
        } catch (ExecutionException ex) {
          Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
          cause.printStackTrace();
          statusLabel.setText("Error loading doc: " + cause.getMessage());
          showCopyableError(MiniReaderFrame.this, "Load failed", cause);
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
          Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
          cause.printStackTrace();
          appendChat("MiniReader: Error: " + cause.getMessage() + "\n\n");
          showCopyableError(MiniReaderFrame.this, "Ask failed", cause);
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

  private void handleIngestOutcome(IngestOutcome outcome) {
    if (outcome instanceof IngestOutcome.SavedIndexed saved) {
      statusLabel.setText("Saved + indexed (" + saved.chunkCount() + " chunks).");
      if (saved.doc() != null) {
        readerArea.setText(saved.doc().plainText());
        readerArea.setCaretPosition(0);
      }
      refreshDocList();
    } else if (outcome instanceof IngestOutcome.JsShell shell) {
      statusLabel.setText(shell.reason());
      if (shell.doc() != null) {
        readerArea.setText(shell.doc().plainText());
        readerArea.setCaretPosition(0);
      }
      refreshDocList();
    } else if (outcome instanceof IngestOutcome.RejectedNonHtml rej) {
      statusLabel.setText("Unsupported content-type: " + rej.contentType());
      showCopyableError(this, "Fetch failed", new IllegalArgumentException("Unsupported content-type: " + rej.contentType()));
    } else if (outcome instanceof IngestOutcome.HttpError http) {
      statusLabel.setText("HTTP " + http.statusCode());
      showCopyableError(this, "Fetch failed", new IOException("HTTP " + http.statusCode() + "\n" + http.snippet()));
    } else if (outcome instanceof IngestOutcome.FetchError fe) {
      statusLabel.setText("Fetch error: " + fe.message());
      showCopyableError(this, "Fetch failed", new IOException(fe.message()));
    }
  }

  @Override
  public void dispose() {
    try {
      core.close();
    } catch (MiniReaderException ignored) {
    }
    super.dispose();
  }
  
  private static void showCopyableError(java.awt.Component parent, String title, Throwable ex) {
	    java.io.StringWriter sw = new java.io.StringWriter();
	    ex.printStackTrace(new java.io.PrintWriter(sw));

	    javax.swing.JTextArea area = new javax.swing.JTextArea(18, 90);
	    area.setText(sw.toString());
	    area.setCaretPosition(0);
	    area.setEditable(false);

	    javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(area);

	    javax.swing.JOptionPane.showMessageDialog(
	        parent,
	        scroll,
	        title,
	        javax.swing.JOptionPane.ERROR_MESSAGE
	    );
	}


  private final CoreFacade core;

  private JButton askButton;
  private JButton fetchButton;
  private DefaultListModel<SavedDocDto> docListModel;
  private JLabel statusLabel;
  private JList<SavedDocDto> docList;
  private JTextArea chatArea;
  private JTextArea readerArea;
  private JTextField chatInput;
  private JTextField urlField;
}
