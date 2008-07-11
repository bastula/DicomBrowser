/**
 * $Id: SwingProgressMonitor.java,v 1.2 2007/01/23 18:50:47 karchie Exp $
 * Copyright (c) 2006 Washington University
 */
package org.nrg.dcm.browse;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;

import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

import org.nrg.dcm.ProgressMonitorI;

/**
 * Swing ProgressMonitor implementing ProgressMonitorI interface
 * All methods are thread safe.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public final class SwingProgressMonitor implements
ProgressMonitorI {
  final ProgressMonitor pm;

  private static abstract class ReturnRunnable<T> implements Runnable {
    T r = null;
  };

  private SwingProgressMonitor(Component parent, Object message, String note,
      int min, int max) {
    assert SwingUtilities.isEventDispatchThread();
    pm = new ProgressMonitor(parent, message, note, min, max);
  }

  /**
   * Thread-safe factory method
   */
  public static SwingProgressMonitor getMonitor(final Component parent,
      final Object message, final String note, final int min, final int max) {
    if (SwingUtilities.isEventDispatchThread()) {
      return new SwingProgressMonitor(parent, message, note, min, max);
    } else {
      final ReturnRunnable<SwingProgressMonitor> get = new ReturnRunnable<SwingProgressMonitor>() {
	public void run() { r = new SwingProgressMonitor(parent, message, note, min, max); }
      };
      while (get.r == null) try {
	SwingUtilities.invokeAndWait(get);
	assert get.r != null;
      } catch (InterruptedException e) {
	// ignore exception and try again
      } catch (InvocationTargetException e) {
	throw new RuntimeException("SwingProgressMonitor constructor threw an exception " + e.getMessage());
      }
      return get.r;
    }
  }

  public void setMinimum(final int m) { 
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { pm.setMinimum(m); }
    });
  }

  public void setMaximum(final int m) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { pm.setMaximum(m); }
    });
  }

  public void setProgress(final int nv) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { pm.setProgress(nv); }
    });
  }

  public void setNote(final String note) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { pm.setNote(note); }
    });
  }

  public boolean isCanceled() {
    if (SwingUtilities.isEventDispatchThread()) {
      return pm.isCanceled();
    } else {
      final ReturnRunnable<Boolean> check = new ReturnRunnable<Boolean>() {
	public void run() { r = pm.isCanceled(); }
      };

      try {
	SwingUtilities.invokeAndWait(check);
      } catch (InterruptedException e) {
	return false;
      } catch (InvocationTargetException e) {
	return false;
      }
      return check.r;
    }
  }

  public void close() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { pm.close(); }
    });
  }
}
