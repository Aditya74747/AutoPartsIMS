package com.autoparts;

import com.autoparts.gui.LoginFrame;
import javax.swing.*;

/// Main class to start the application
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}
