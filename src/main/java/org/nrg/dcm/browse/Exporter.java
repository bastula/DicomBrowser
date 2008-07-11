/**
 * $Id: Exporter.java,v 1.7 2008/01/31 21:24:22 karchie Exp $
 * Copyright (c) 2006,2008 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.io.DicomInputStream;

import org.nrg.dcm.edit.Action;
import org.nrg.dcm.edit.AttributeException;
import org.nrg.dcm.edit.Statement;


abstract class Exporter implements Runnable {
  private static final String GZIP_SUFFIX = ".gz";
  private static final String IO_SKIP_MSG = "Unable to read %1$s : %2$s; skipping";     // TODO: localize
  private static final String SOCKET_MSG = "Network error: %1$s";
  private static final String ERROR_TITLE = "Export failed";
  private static final String FAIL_MSG = "Export failed: %1$s";
  private static final Object[] FAIL_OPTIONS = {"Try next object", "Cancel export"};

  private final Collection<Statement> ss;
  private final Collection<File> files;

  Exporter(final Collection<Statement> s, final Collection<File> files) {
    this.ss = new ArrayList<Statement>(s);
    this.files = new ArrayList<File>(files);
  }
  
  Exporter(final Statement s, final Collection<File> files) {
    this(new LinkedList<Statement>(), files);
    if (null != s) { this.ss.add(s); }
  }

  final class CancelException extends Exception {
    private static final long serialVersionUID = 1L;
  }

  @SuppressWarnings("unused")
  void prepare(File f) throws IOException,CancelException {}

  @SuppressWarnings("unused")
  void open() throws IOException {}

  abstract void process(File f, DicomObject o) throws IOException,CancelException;

  @SuppressWarnings("unused")
  void close() throws IOException {}

  final DicomObject read(final File f) throws IOException {
    BufferedInputStream bin = null;
    final DicomObject o;
    try {
      InputStream fin = new FileInputStream(f);
      if (f.getName().endsWith(GZIP_SUFFIX))
	fin = new GZIPInputStream(fin);
      bin = new BufferedInputStream(fin);
      final DicomInputStream in = new DicomInputStream(bin);
      o = in.readDicomObject();
    } finally {
      // DicomInputStream.close() is inherited from FilterInputStream and simply
      // does in.in.close(), so nothing leaks if we do in.in (aka fin).close() directly.
      if (bin != null)
	bin.close();
    }
    return o;
  }

  final private DicomObject apply(final File f) throws IOException {
    final DicomObject o = read(f);
    final Collection<Action> actions = new LinkedList<Action>();
    for (final Statement s : ss) {
      try {
	actions.addAll(s.getActions(f, o));
      } catch (AttributeException e) {
	assert false : "Caught AttributeException when only file constraints are used";
      return null;
      }
    }
    if (!actions.isEmpty())
      for (Action action : actions)
	action.apply();

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
	  if (n == 0)
	    continue;	// move on to next file
	  else
	    return;	// abort export
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
	  if (n == 0)
	    continue;	// move on to next file
	  else
	    return;	// abort export
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