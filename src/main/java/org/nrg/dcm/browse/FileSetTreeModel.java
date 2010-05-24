/**
 * Copyright (c) 2006,2007,2010 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.util.Collection;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;

import java.awt.Component;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.nrg.dcm.FileSet;
import org.nrg.dcm.DirectoryRecord;
import org.nrg.dcm.ProgressMonitorI;


/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 */
public final class FileSetTreeModel implements TreeModel {
  private static final String READING_FILES = "Reading files...";
  private static final String UNABLE_TO_LOAD_FILE = "Unable to load file: %1$s";
  private static final String IMPORT_FAILED = "Import failed";

  private static final ResourceBundle rsrcb = new ListResourceBundle() {
    @Override
    public Object[][] getContents() { return contents; }
    private final Object[][] contents = {       // TODO: LOCALIZE THIS
	{READING_FILES, READING_FILES},
	{UNABLE_TO_LOAD_FILE, UNABLE_TO_LOAD_FILE},
	{IMPORT_FAILED, IMPORT_FAILED},
    };
  };

  private final Component window;
  private final FileSet fs;
  private List<DirectoryRecord> patients;
  private final Collection<TreeModelListener> listeners = new HashSet<TreeModelListener>();

  FileSetTreeModel(final Component window, final FileSet fs) {
    this.window = window;
    this.fs = fs;
    handleAdd();
  }


  private void handleAdd() {
    patients = new LinkedList<DirectoryRecord>(fs.getPatientDirRecords());
    final TreeModelEvent e = new TreeModelEvent(this, new Object[]{fs});	// TODO: permit treeNodesInserted
    for (TreeModelListener l : listeners)
      l.treeStructureChanged(e);
  }


  /**
   * Add the listed files to the FileSet.  Must be invoked from outside the Swing event handler.
   * @param files
   */
  public void add(final Collection<File> files) {
    assert !SwingUtilities.isEventDispatchThread();

    final ProgressMonitorI pn = SwingProgressMonitor.getMonitor(window, rsrcb.getString(READING_FILES),
	"", 0, files.size());

    try {
      fs.add(files.toArray(new File[0]), pn);
      handleAdd();
    } catch (final Exception e) {
      SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  JOptionPane.showMessageDialog(window,
	      String.format(rsrcb.getString(UNABLE_TO_LOAD_FILE), e.getMessage()),
	      rsrcb.getString(IMPORT_FAILED),
	      JOptionPane.ERROR_MESSAGE);
	}
      });
    } finally {
      pn.close();
    }
  }

  /**
   * Remove the selection from the FileSet.
   * @param records Paths to be removed
   */
  public void remove(final Collection<TreePath> tps) {
    final Collection<DirectoryRecord> drs = new LinkedList<DirectoryRecord>();
    for (final TreePath tp : tps) {
      drs.add((DirectoryRecord)tp.getLastPathComponent());
    }
    fs.remove(drs);

    final Collection<TreeModelEvent> es = new LinkedList<TreeModelEvent>();
    for (final TreePath tp : tps) {
      es.add(new TreeModelEvent(this, tp.getParentPath()));	// TODO: permit treeNodesRemoved
    }

    // Separate extracting the TreePaths from notifying listeners; one of the
    // listeners may be the caller, and may modify tps as a result of the
    // notification.
    for (TreeModelEvent e : es)
      for (TreeModelListener l : listeners)
	l.treeStructureChanged(e);
  }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.TreeModelListener)
   */
  public void addTreeModelListener(final TreeModelListener l) { listeners.add(l); }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
   */
  public Object getChild(final Object parent, final int index) {
    if (parent == fs)
      return patients.get(index);
    assert parent instanceof DirectoryRecord;
    assert 0 <= index && index <= ((DirectoryRecord)parent).getLower().size();
    return ((DirectoryRecord)parent).getLower().get(index);
  }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
   */
  public int getChildCount(final Object parent) {
    return (parent == fs) ? patients.size() : ((DirectoryRecord)parent).getLower().size();
  }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
   */
  public int getIndexOfChild(final Object parent, final Object child) {
    if (parent == fs)
      return patients.indexOf(child);
    else
      return ((DirectoryRecord)parent).getLower().indexOf(child);
  }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeModel#getRoot()
   */
  public Object getRoot() { return fs; }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
   */
  public boolean isLeaf(final Object node) {
    if (!(node instanceof DirectoryRecord)) return false;
    return ((DirectoryRecord)node).getType() == DirectoryRecord.Type.INSTANCE;
  }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.TreeModelListener)
   */
  public void removeTreeModelListener(final TreeModelListener l) { listeners.remove(l); }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
   */
  public void valueForPathChanged(final TreePath arg0, final Object arg1) {
    assert false : "don't change contents of this tree!";
  }

  public File[] getSelectedFiles() {
    return null;
  }
}
