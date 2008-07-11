/**
 * $Id: FileWriter.java,v 1.6 2008/04/02 22:24:39 karchie Exp $
 * Copyright (c) 2006 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import java.util.Collection;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomOutputStream;

import org.nrg.dcm.ProgressMonitorI;
import org.nrg.dcm.edit.Statement;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
abstract class FileWriter extends Exporter {
  private static final String AE_TITLE = "DicomBrowser";

  private final ProgressMonitorI pm;
  private int progress = 0;

  /**
   * @param s
   * @param files
   * @params pm
   */
  public FileWriter(final Collection<File> files, final Collection<Statement> s,
      ProgressMonitorI pm) {
    super(s, files);
    this.pm = pm;
    pm.setMinimum(0);
    pm.setMaximum(files.size());
  }
  
  public FileWriter(final Collection<File> files, final Statement s,
      ProgressMonitorI pm) {
    super(s, files);
    this.pm = pm;
    pm.setMinimum(0);
    pm.setMaximum(files.size());
  }

  abstract File getDestFile(File f) throws IOException;

  /* (non-Javadoc)
   * @see org.nrg.dcm.browse.Exporter#process(java.io.File, org.dcm4che2.data.DicomObject)
   */
  @Override
  final void process(final File f, final DicomObject o) throws IOException,CancelException {
    if (pm != null)
      pm.setNote(f.getPath());

    final DicomObject orig = read(f);
    final String cuid = orig.getString(Tag.MediaStorageSOPClassUID);
    final String iuid = orig.getString(Tag.MediaStorageSOPInstanceUID);
    final String tsuid = getTransferSyntaxUID(orig);

    BasicDicomObject fmi = new BasicDicomObject();
    fmi.initFileMetaInformation(cuid, iuid, tsuid);
    fmi.putString(Tag.SourceApplicationEntityTitle, VR.AE, AE_TITLE);

    DicomOutputStream dos = null;
    OutputStream os = null;
    FileOutputStream fos = null;

    try {
      fos = new FileOutputStream(getDestFile(f));
      os = (f.getName().endsWith(".gz")) ? new GZIPOutputStream(fos) : new BufferedOutputStream(fos);

      dos = new DicomOutputStream(os);
      dos.writeFileMetaInformation(fmi);
      dos.writeDataset(o, tsuid);
    } finally {
      // This is ugly, but any of the constructors could fail.
      if (dos != null)
	dos.close();
      else if (os != null)
	os.close();
      else if (fos != null)
	fos.close();
    }

    if (pm != null) {
      if (pm.isCanceled())
	throw new CancelException();
      pm.setProgress(++progress);
    }
  }

  @Override
  void close() {
    if (pm != null)
      pm.close();
  }
}
