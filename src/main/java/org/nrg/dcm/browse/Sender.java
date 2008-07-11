/**
 * $Id: Sender.java,v 1.2 2007/04/03 04:32:23 karchie Exp $
 * Copyright (c) 2006 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.swing.ProgressMonitor;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.net.TransferCapability;

import org.nrg.dcm.DicomSender;
import org.nrg.dcm.edit.Statement;

final class Sender extends Exporter {
  private static final String MAKE_CONN_MSG = "Making network connection";
  private static final String BAD_CONN_MSG = "Unable to make network connection: %1$s";

  private final String host;
  private final String port;
  private final String aeTitle;
  private int progress = 0;
  private final ProgressMonitor pm;
  private DicomSender sender = null;
  private final TransferCapability[] tcs;
  

  Sender(final String host, final String port, final String aeTitle,
      final TransferCapability[] tcs, final Collection<File> files,
      final Statement statements, final ProgressMonitor pm) {
    super(statements, files);
    this.host = host;
    this.port = port;
    this.aeTitle = aeTitle;
    this.tcs = tcs;
    this.pm = pm;
  }
  
  @Override
  void open() throws IOException {
    if (pm != null)
      pm.setNote(MAKE_CONN_MSG);
    
    // Open a connection to the remote AE.
    try {
      sender = new DicomSender("DicomBrowser", tcs, host, port, aeTitle);
    } catch (ConfigurationException e) {
      throw new IOException(String.format(BAD_CONN_MSG, e.getMessage()));
    }
  }
     
  @Override
  protected void process(File f, DicomObject o) throws IOException,CancelException {
    if (pm != null)
      pm.setNote(f.getName());

    sender.send(o, getTransferSyntaxUID(o));

    if (pm != null) {
      if (pm.isCanceled()) {
        throw new CancelException();
      }
      pm.setProgress(++progress);
    }
  }
  
  @Override
  void close() {
    if (sender != null)
      sender.close();
    if (pm != null)
      pm.close();
  }
 }