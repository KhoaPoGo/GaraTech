package main;

import gui.LoginFrame;
import gui.component.Theme;


import javax.swing.SwingUtilities;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        Theme.install();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
