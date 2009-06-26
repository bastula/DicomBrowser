/**
 * Copyright (c) 2006-2009 Washington University
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
import org.nrg.dcm.edit.StatementList;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
@Deprecated
abstract class FileWriter extends Exporter {
  private static final String AE_TITLE = "DicomBrowser";

  private final ProgressMonitorI pm;
  private int progress = 0;

  /**
   * @param s
   * @param files
   * @params pm
   */
  public FileWriter(final Collection<File> files, final StatementList s,
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
    if (pm != null) {
      pm.setNote(f.getPath());
    }

    final DicomObject orig = read(f);
    final String cuid = orig.getString(Tag.MediaStorageSOPClassUID);
    final String iuid = orig.getString(Tag.MediaStorageSOPInstanceUID);
    final String tsuid = getTransferSyntaxUID(orig);

    BasicDicomObject fmi = new BasicDicomObject();
    fmi.initFileMetaInformation(cuid, iuid, tsuid);
    fmi.putString(Tag.SourceApplicationEntityTitle, VR.AE, AE_TITLE);

    final FileOutputStream fos = new FileOutputStream(getDestFile(f));
    try {
	final OutputStream os;
	if (f.getName().endsWith(".gz")) {
	    os = new GZIPOutputStream(fos);
	} else {
	    os = new BufferedOutputStream(fos);
	}
	try {
	    final DicomOutputStream dos = new DicomOutputStream(os);
	    try {
		dos.writeFileMetaInformation(fmi);
		dos.writeDataset(o, tsuid);
	    } finally {
		dos.close();
	    }
	} finally {
	    os.close();
	}
    } finally {
	fos.close();
    }

    if (pm != null) {
      if (pm.isCanceled()) {
	throw new CancelException();
      }
      pm.setProgress(++progress);
    }
  }

  @Override
  void close() {
    if (null != pm) {
      pm.close();
    }
  }
}
