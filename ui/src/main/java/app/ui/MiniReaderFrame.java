package app.ui;

import javax.swing.*;
import java.awt.*;

public class MiniReaderFrame extends JFrame {
  public MiniReaderFrame() {
    super("MiniReader (Scaffold)");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setSize(1000, 700);
    add(new JLabel("MiniReader scaffold loaded. Paste full implementation as needed."),
        BorderLayout.CENTER);
  }
}
