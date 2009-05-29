/**
 * Copyright (c) 2006,2008-2009 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JOptionPane;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;

import org.nrg.dcm.CStoreException;
import org.nrg.dcm.edit.Action;
import org.nrg.dcm.edit.AttributeException;
import org.nrg.dcm.edit.StatementList;
import org.nrg.dcm.io.DicomFileMapper;


abstract class Exporter implements Runnable {
  private static final String IO_SKIP_MSG = "Unable to read %1$s : %2$s; skipping";     // TODO: localize
  private static final String SOCKET_MSG = "Network error: %1$s";
  private static final String ERROR_TITLE = "Export failed";
  private static final String FAIL_MSG = "Export failed: %1$s";
  private static final Object[] FAIL_OPTIONS = {"Try next object", "Cancel export"};

  private final StatementList statements;
  private final Collection<File> files;

  Exporter(final StatementList statements, final Collection<File> files) {
    this.statements = statements;
    this.files = new ArrayList<File>(files);
  }

  final class CancelException extends Exception {
    private static final long serialVersionUID = 1L;
  }

  void prepare(File f) throws IOException,CancelException {}

  void open() throws IOException {}

  abstract void process(File f, DicomObject o) throws IOException,CancelException,CStoreException;

  void close() throws IOException {}

  final DicomObject read(final File f) throws IOException {
    return (DicomObject)DicomFileMapper.getInstance().map(f);
  }


  final private DicomObject apply(final File f) throws IOException {
      final DicomObject o = read(f);
      try {
	  for (final Object action : statements.getActions(f, o)) {
	      ((Action)action).apply();
	  }
      } catch (AttributeException e) {
	  throw new RuntimeException("Caught AttributeException when only file constraints are used");
      }

      return o;
  }

  final public void run() {
    final InterfaceCounter ic = InterfaceCounter.getInstance();
    ic.register(this);	// don't exit the program until we're done with this task.
    try {
      for (final Iterator<File> fi = files.iterator(); fi.hasNext();) {
        final File f = fi.next();
        try {
          prepare(f);
        } catch (SocketException e) {
          JOptionPane.showMessageDialog(null,
              String.format(SOCKET_MSG, e.getMessage()), ERROR_TITLE, 
              JOptionPane.ERROR_MESSAGE);
          return;
        } catch (IOException e) {
          JOptionPane.showMessageDialog(null,
              String.format(IO_SKIP_MSG, f.getPath(), e.getMessage()), ERROR_TITLE,
              JOptionPane.WARNING_MESSAGE);
          fi.remove();
        }
      }

      try {
        open();
      } catch (IOException e) {
        JOptionPane.showMessageDialog(null,
            String.format(FAIL_MSG, e.getMessage()), ERROR_TITLE,
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      for (final File f : files) {
        final DicomObject o;
        try {
          o = apply(f);
        } catch (SocketException e) {   // no point in continuing with a socket error
          JOptionPane.showMessageDialog(null,
              String.format(SOCKET_MSG, e.getMessage()), ERROR_TITLE,
              JOptionPane.ERROR_MESSAGE);
          return;
        } catch (IOException e) {
          final int n = JOptionPane.showOptionDialog(null,
              String.format(IO_SKIP_MSG, f.getPath(), e.getMessage()),
              ERROR_TITLE,
              JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE,
              null,
              FAIL_OPTIONS,
              FAIL_OPTIONS[0]);
          if (0 == n) continue; else return;
        }
        try {
          process(f, o);
        } catch (IOException e) {
          final int n = JOptionPane.showOptionDialog(null,
              String.format(FAIL_MSG, e.getMessage()),
              ERROR_TITLE,
              JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE,
              null,
              FAIL_OPTIONS,
              FAIL_OPTIONS[0]);
          if (0 == n) continue; else return;
        } catch (CStoreException e) {
          final int n = JOptionPane.showOptionDialog(null,
              String.format(FAIL_MSG, e.getMessage()),
              ERROR_TITLE,
              JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE,
              null,
              FAIL_OPTIONS,
              FAIL_OPTIONS[0]);
          if (0 == n) continue; else return;
        }
      }
    } catch (CancelException e) {     // exit quietly
    } finally {
      ic.unregister(this);
      try {
        close();
      } catch (IOException e) {}        // no use squawking now
    }
  }

  /**
   * Returns the Transfer Syntax UID for the given DicomObject
   * @param o DicomObject
   * @return Transfer Syntax UID
   */
  protected static final String getTransferSyntaxUID(final DicomObject o) {
    final String ts = o.getString(Tag.TransferSyntaxUID);
    // Default TS UID is Implicit VR LE (PS 3.5, Section 10)
    return ts == null ? UID.ImplicitVRLittleEndian : ts;
  }
}