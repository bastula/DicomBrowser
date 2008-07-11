/**
 * $Id: StatusBar.java,v 1.2 2007/01/24 17:46:12 karchie Exp $
 * Copyright (c) 2006,2007 Washington University
 */
package org.nrg.dcm.browse;

import java.util.LinkedList;
import java.util.Queue;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import java.awt.BorderLayout;

import org.nrg.dcm.ProgressMonitorI;

/**
 * Status bar for the bottom of a DicomBrowser window.
 * Provides space for a progress bar and text status messages.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public class StatusBar extends JComponent {
  private static final long serialVersionUID = 1L;
  
  private static final int DEFAULT_MIN_PROGRESS = 0;
  private static final int DEFAULT_MAX_PROGRESS = 100;
  
  private static final Border margin = BorderFactory.createEmptyBorder(4,4,4,4);
  private final JProgressBar progressBar;
  private final JLabel text;
  
  private final Queue<TaskMonitor> tasks;
  
  /**
   * This constructor must be called from the Swing event handler thread.
   */
  public StatusBar() {
    assert SwingUtilities.isEventDispatchThread();
    
    this.setLayout(new BorderLayout());
    
    text = new JLabel();
    this.add(text, BorderLayout.WEST);
    
    progressBar = new JProgressBar(DEFAULT_MIN_PROGRESS, DEFAULT_MAX_PROGRESS);
    progressBar.setStringPainted(false);
    this.add(progressBar, BorderLayout.EAST);
    
    this.setBorder(margin);
    
    tasks = new LinkedList<TaskMonitor>();
  }
  
  public class TaskMonitor implements ProgressMonitorI {
    private boolean canceledp;
    private String note;
    private final String done;
    private int min, max, progress;
    
    private TaskMonitor(final int min, final int max, final String done) {
      this.min = min;
      this.max = max;
      this.progress = min;
      canceledp = false;
      this.done = done;
    }
    
    /**
     * This method is thread safe.
     */
    public void makeActive() {
      SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  progressBar.setMinimum(min);
	  progressBar.setMaximum(max);
	  progressBar.setValue(progress);
	  text.setText(note);
	}
      });
    }
    
    /**
     * This method is thread safe.
     */
    public void close() {
      synchronized(tasks) {
	if (tasks.peek() == this) {
	  SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
	      progressBar.setValue(progressBar.getMinimum());
	      progressBar.setStringPainted(false);
	      text.setText(done);
	    }
	  });

	  tasks.poll();

	  final TaskMonitor next = tasks.peek();
	  if (next != null)
	    next.makeActive();
	} else
	  tasks.remove(this);
      }
    }
    
    /**
     * This method is thread safe.
     */
    public void cancel() {
      canceledp = true;
      this.close();
    }
      
    /**
     * This method is thread safe.
     */
    public boolean isCanceled() { return canceledp; }
    
    /**
     * This method is thread safe.
     */
    public void setMinimum(final int m) {
      min = m;
      if (tasks.peek() == this) {
	SwingUtilities.invokeLater(new Runnable() {
	  public void run() { progressBar.setMinimum(min); }
	});
      }
    }
    
    /**
     * This method is thread safe.
     */
    public void setMaximum(final int m) {
      max = m;
      SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  if (tasks.peek() == TaskMonitor.this)
	    progressBar.setMaximum(max);
	  if (progressBar.getValue() >= max)
	    TaskMonitor.this.close();
	}
      });
    }
    
    /**
     * This method is thread safe.
     */
    public void setProgress(final int p) {
      progress = p;
      SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	      if (tasks.peek() == TaskMonitor.this)
	        progressBar.setValue(progress);
	      if (progress >= progressBar.getMaximum())
	        TaskMonitor.this.close();
	}
      });
    }
    
    /**
     * This method is thread safe.
     */
    public void setNote(final String s) {
      note = s;
      if (tasks.peek() == this)
	SwingUtilities.invokeLater(new Runnable() {
	  public void run() {
	    text.setText(note);
	  }
	});
    }
  }
  
  /**
   * This method is thread safe.
   */
  public TaskMonitor getTaskMonitor(final int min, final int max, final String start, final String done) {
    final TaskMonitor m = new TaskMonitor(min, max, done);

    synchronized(tasks) { tasks.add(m); }
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	m.setNote(start);
      }
    });
    tasks.peek().makeActive();
    return m;
  }
  
}
