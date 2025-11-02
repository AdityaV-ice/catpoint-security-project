package com.udacity.catpoint.application;

import javax.swing.SwingUtilities;

public class CatpointApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CatpointGui().setVisible(true));
    }
}
