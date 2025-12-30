package com.simplerender.ui;

import java.awt.GraphicsEnvironment;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModelSelectionDialog {
    private static final Logger logger = LoggerFactory.getLogger(ModelSelectionDialog.class);

    private ModelSelectionDialog() {
    }

    public static String chooseModelPath() {
        if (GraphicsEnvironment.isHeadless()) {
            logger.warn("Headless environment detected; skipping model selection dialog");
            return null;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a model to load");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Wavefront OBJ (*.obj)", "obj"));
        int result = chooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            logger.info("No model selected; using default chunks");
            return null;
        }
        File selected = chooser.getSelectedFile();
        if (selected == null) {
            logger.info("No model selected; using default chunks");
            return null;
        }
        return selected.getAbsolutePath();
    }
}
