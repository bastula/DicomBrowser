/**
 * Copyright (c) 2012 Washington University
 */
package org.nrg.dcm.browse;

import java.awt.Component;
import java.awt.Container;
import java.awt.HeadlessException;
import java.io.File;

import javax.swing.JFileChooser;

/**
 * JFileChooser specialized for selecting an output directory, possibly creating
 * directories along the way.
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public final class OutputFolderChooser extends JFileChooser {
    private static final long serialVersionUID = -1209369407121160230L;

    public OutputFolderChooser() {
        super();
        setFileSelectionMode(DIRECTORIES_ONLY);
    }

    public OutputFolderChooser(final File currentDirectory) {
        super(currentDirectory);
        setFileSelectionMode(DIRECTORIES_ONLY);
    }
    
    public OutputFolderChooser(final String currentDirectoryPath) {
        super(currentDirectoryPath);
        setFileSelectionMode(DIRECTORIES_ONLY);
    }
    
    @Override
    public int showOpenDialog(final Component parent) throws HeadlessException {
        throw new UnsupportedOperationException("OutputFolderChooser is type SAVE_DIALOG");
    }
    
    @Override
    public int showDialog(final Component parent, final String approveButtonText) {
        final Container contents  = (Container)this.getComponent(1);
        final Component newFileNamePanel = contents.getComponent(0);
        if (null != approveButtonText) {
            setApproveButtonText(approveButtonText);
        }
        setDialogType(SAVE_DIALOG);
        newFileNamePanel.setVisible(false);
        return super.showDialog(parent, null);
    }
}
