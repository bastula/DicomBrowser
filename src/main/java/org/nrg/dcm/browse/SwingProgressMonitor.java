/**
 * Copyright (c) 2006-2009 Washington University
 */
package org.nrg.dcm.browse;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;

import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.nrg.dcm.ProgressMonitorI;
import org.nrg.util.EditProgressMonitor;


/**
 * Swing ProgressMonitor implementing ProgressMonitorI interface
 * All methods are thread safe.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public final class SwingProgressMonitor implements
ProgressMonitorI,EditProgressMonitor {
    private final Logger logger = Logger.getLogger(SwingProgressMonitor.class);
    private final ProgressMonitor pm;
    private final String message;

    private static abstract class ReturnRunnable<T> implements Runnable {
	T r = null;
    };

    private SwingProgressMonitor(Component parent, Object message, String note,
	    int min, int max) {
	assert SwingUtilities.isEventDispatchThread();
	pm = new ProgressMonitor(parent, message, note, min, max);
	this.message = null == message ? null : message.toString();
	logger.trace("Created " + this);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
	final StringBuilder sb = new StringBuilder(super.toString());
	sb.append(" ").append(message);
	sb.append(" [").append(pm.getMinimum()).append("-");
	sb.append(pm.getMaximum()).append("]");
	return sb.toString();
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
		if (e.getCause() instanceof RuntimeException) {
		    throw (RuntimeException)e.getCause();
		} else {
		    throw new RuntimeException(e.getCause());
		}
	    }
	    return get.r;
	}
    }

    /*
     * (non-Javadoc)
     * @see org.nrg.dcm.ProgressMonitorI#setMinimum(int)
     */
    public void setMinimum(final int m) { 
	logger.trace(this + " minimum <- " + m);
	SwingUtilities.invokeLater(new Runnable() {
	    public void run() { pm.setMinimum(m); }
	});
    }

    /*
     * (non-Javadoc)
     * @see org.nrg.dcm.ProgressMonitorI#setMaximum(int)
     */
    public void setMaximum(final int m) {
	logger.trace(this + " maximum <- " + m);
	SwingUtilities.invokeLater(new Runnable() {
	    public void run() { pm.setMaximum(m); }
	});
    }

    /*
     * (non-Javadoc)
     * @see org.nrg.dcm.ProgressMonitorI#setProgress(int)
     */
    public void setProgress(final int nv) {
	logger.trace(this + " progress <- " + nv);
	SwingUtilities.invokeLater(new Runnable() {
	    public void run() { pm.setProgress(nv); }
	});
    }

    /*
     * (non-Javadoc)
     * @see org.nrg.dcm.ProgressMonitorI#setNote(java.lang.String)
     */
    public void setNote(final String note) {
	logger.trace(this + " note <- " + note);
	SwingUtilities.invokeLater(new Runnable() {
	    public void run() { pm.setNote(note); }
	});
    }

    /*
     * (non-Javadoc)
     * @see org.nrg.dcm.ProgressMonitorI#isCanceled()
     */
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

    /*
     * (non-Javadoc)
     * @see org.nrg.dcm.ProgressMonitorI#close()
     */
    public void close() {
	logger.trace(this + " close");
	SwingUtilities.invokeLater(new Runnable() {
	    public void run() { pm.close(); }
	});
    }
}
