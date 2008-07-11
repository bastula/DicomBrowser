/**
 * $Id: StreamProgressMonitor.java,v 1.2 2008/04/02 22:25:26 karchie Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.dcm.edit;

import java.io.PrintStream;

import org.nrg.dcm.ProgressMonitorI;

/**
 * Sends progress messages to a PrintStream.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
final class StreamProgressMonitor implements ProgressMonitorI {
  private final static String LINE_SEPARATOR = System.getProperty("line.separator");
  private final PrintStream os;
  private final String desc, format, startNote;
  private String note;
  private int min = 0, max = 0, lastReport = 0;
  private int reportCutoff = 200, reportIntervalCoarse = 100, reportIntervalFine = 20;
  
  public StreamProgressMonitor(final PrintStream os, final String desc, final String startNote) {
    this.os = os;
    this.desc = desc;
    this.format = desc + " %s (%d/%d)" + LINE_SEPARATOR;
    this.note = this.startNote = startNote;
   }
  
  public void setReportIntervals(final int cutoff, final int coarse, final int fine) {
    this.reportCutoff = cutoff;
    this.reportIntervalCoarse = coarse;
    this.reportIntervalFine = fine;
  }
  
  public void setMinimum(final int min) {
    this.min = min;
  }
  
  public void setMaximum(final int max) {
    this.max = max;
  }
  
  public void setProgress(final int current) {
    final int range = max - min;
    final int reportInterval = (range > reportCutoff) ? reportIntervalCoarse : reportIntervalFine;
    if (current - lastReport >= reportInterval) {
      os.format(format, note, current - min, range);
      lastReport = current;
    }
  }
  
  public void setNote(final String note) {
    this.note = note;
  }
  
  public void close() {
    os.format("%s %s (done)%s", desc, startNote, LINE_SEPARATOR);
  };
  
  public boolean isCanceled() { return false; };
}