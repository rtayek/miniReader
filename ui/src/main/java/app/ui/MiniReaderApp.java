package app.ui;

import javax.swing.SwingUtilities;

public class MiniReaderApp {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      try {
        new MiniReaderFrame().setVisible(true);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
