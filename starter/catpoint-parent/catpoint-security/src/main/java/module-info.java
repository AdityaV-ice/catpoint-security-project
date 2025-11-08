module com.udacity.catpoint.security {
    requires java.desktop;
    requires java.prefs;
    requires com.google.common;                     // for Swing UI
    requires com.google.gson;                  // for serialization
    requires com.udacity.catpoint.image;       // depends on image module
    requires miglayout.swing;
    exports com.udacity.catpoint.security.application;
    exports com.udacity.catpoint.security.service;

    opens com.udacity.catpoint.data to com.google.gson; // Gson reflection
}
