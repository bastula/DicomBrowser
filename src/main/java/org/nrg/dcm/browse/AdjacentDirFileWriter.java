package org.nrg.dcm.browse;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.nrg.dcm.ProgressMonitorI;
import org.nrg.dcm.edit.Statement;

/**
 * Places new files in a directory adjacent to the original.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
final class AdjacentDirFileWriter extends FileWriter {
  private final String format;
  
  public AdjacentDirFileWriter(final Collection<File> files, final Statement s,
      final String format, final ProgressMonitorI pm) {
    super(files, s, pm);
    this.format = format;
  }

  @Override
  File getDestFile(File f) throws IOException {
    final File origparent = f.getParentFile();
    final String newparent = String.format(format, origparent.getName());
    final File destdir = new File(origparent.getParentFile(), newparent);
    destdir.mkdir();
    return new File(destdir,f.getName());
  }

}
