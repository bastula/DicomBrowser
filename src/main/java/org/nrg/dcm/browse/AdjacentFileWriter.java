/**
 * Copyright (c) 2006-2009 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.nrg.dcm.ProgressMonitorI;
import org.nrg.dcm.edit.StatementList;

/**
 * Writes each new file next to the original, with the given suffix.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public final class AdjacentFileWriter extends FileWriter {
  private final static String DCM_SUFFIX = ".dcm";
  
  final String suffix;
  
  public AdjacentFileWriter(final Collection<File> files, final StatementList s,
    final String suffix, final ProgressMonitorI pm) {
      super(files, s, pm);
      this.suffix = suffix;
    }

  @Override
  File getDestFile(final File f) throws IOException {
    final String orig = f.getName();
    final String name;
    if (orig.endsWith(DCM_SUFFIX))
      name = orig.substring(0, orig.lastIndexOf(DCM_SUFFIX)) + suffix + DCM_SUFFIX;
    else
      name = orig + suffix;
    
    return new File(f.getParentFile(), name);
  }

}
