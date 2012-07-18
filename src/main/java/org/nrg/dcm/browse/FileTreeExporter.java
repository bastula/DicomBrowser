/**
 * Copyright (c) 2012 Washington University
 */
package org.nrg.dcm.browse;

import static org.nrg.dcm.browse.FileSetTableModel.AE_TITLE;
import static org.nrg.dcm.browse.FileSetTableModel.COPYING_FILES;
import static org.nrg.dcm.browse.FileSetTableModel.rsrcb;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.nrg.dcm.edit.ScriptApplicator;
import org.nrg.dcm.edit.ScriptEvaluationException;
import org.nrg.dcm.edit.SessionVariablePanel;
import org.nrg.dcm.edit.Statement;
import org.nrg.dcm.io.AdjacentDirFileExporter;
import org.nrg.dcm.io.AdjacentFileExporter;
import org.nrg.dcm.io.BatchExporter;
import org.nrg.dcm.io.DicomObjectExporter;
import org.nrg.dcm.io.MultifileExporter;
import org.nrg.dcm.io.NewRootFileExporter;
import org.nrg.dcm.io.OverwriteFileExporter;
import org.nrg.io.FileWalkIterator;
import org.nrg.util.EditProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class FileTreeExporter implements MultifileExporter {
    private final Logger logger = LoggerFactory.getLogger(FileTreeExporter.class);
    private final Component parent;
    private final ExecutorService executor;
    private final File source;
    private final List<Statement> statements = Lists.newArrayList();
    
    public FileTreeExporter(final Component parent, final Preferences prefs, final ExecutorService executor, final File root) {
        this.parent = parent;
        this.executor = executor;
        this.source = root;
        
        final JFileChooser scriptChooser = new JFileChooser(prefs.get(DicomBrowser.SCRIPT_DIR_PREF, null));
        scriptChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        scriptChooser.setDialogTitle("Select DicomEdit script to apply");
        final int useScript = scriptChooser.showOpenDialog(parent);
        if (useScript == JFileChooser.APPROVE_OPTION) {
            final File script = scriptChooser.getSelectedFile();
            try {
                IOException ioexception = null;
                final InputStream in = new FileInputStream(script);
                try {
                    final ScriptApplicator applicator = new ScriptApplicator(in);
                    statements.addAll(applicator.getStatements());
                    SessionVariablePanel.withAssignedVariables(applicator.getSortedVariables(),
                            ArrayListMultimap.<Integer,String>create()); // TODO: initial values?
                } catch (ScriptEvaluationException e) {
                    // TODO: warning dialog
                    logger.error("unable to parse script " + script, e);
                } catch (IOException e) {
                    throw ioexception = e;
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        if (null == ioexception) {
                            throw ioexception = e;
                        } else {
                            logger.warn("unable to close script file " + script, e);
                            throw ioexception;
                        }
                    }
                }
            } catch (IOException e) {
                // TODO: warning dialog
                logger.error("unable to read script " + script, e);
            }
        }
    }
    
    private void export(final DicomObjectExporter exporter) {
        final EditProgressMonitor pm = SwingProgressMonitor.getMonitor(parent,
                rsrcb.getString(COPYING_FILES), "", 0, 0);

        final BatchExporter batch = new BatchExporter(exporter, statements, new FileWalkIterator(source, pm));
        batch.setProgressMonitor(pm, 0);
        executor.execute(new ExportFailureHandler(batch, parent));
    }

    public void saveInAdjacentDir(final String format, final boolean allFiles) {
        export(new AdjacentDirFileExporter(AE_TITLE, format));
    }

    public void saveInAdjacentFile(final String suffix, final boolean allFiles) {
        export(new AdjacentFileExporter(AE_TITLE, suffix));
    }

    public void saveInNewRoot(final String rootpath, final boolean allFiles) {
        final DicomObjectExporter exporter;
        try {
            exporter = new NewRootFileExporter(AE_TITLE, new File(rootpath), Arrays.asList(source));
        } catch (Throwable t) {
            logger.error("unable to resolve data roots", t);
            JOptionPane.showMessageDialog(parent,      // TODO: localize
                    "Error in organizing output files: " + t.getMessage(),
                    "Export failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        export(exporter);
    }
    
    public void overwrite(final boolean allFiles) {
        export(new OverwriteFileExporter(AE_TITLE));
    }
}
