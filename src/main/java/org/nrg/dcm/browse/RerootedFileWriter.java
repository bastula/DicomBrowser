/**
 * $Id: RerootedFileWriter.java,v 1.4 2008/04/02 18:30:16 karchie Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.nrg.dcm.ProgressMonitorI;
import org.nrg.dcm.edit.FileRootRemapper;
import org.nrg.dcm.edit.Statement;

/**
 * Writes modified files to a new hierarchy under a new root
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public class RerootedFileWriter extends FileWriter {
  private final Logger log = Logger.getLogger(RerootedFileWriter.class);
  private final FileRootRemapper remapper;
  
  /**
   * @param outDir Root of the new directory hierarchy
   * @param roots Roots under which original files can be found
   * @param files Original files to be processed
   * @param s Collection of Statements to be performed
   * @param pm
   * @throws IOException If a root directory (new or existing) cannot be converted to canonical form
   */
  public RerootedFileWriter(final File outDir,
      final Collection<File> roots, final Collection<File> files, final Collection<Statement> s,
      final ProgressMonitorI pm)
  throws IOException {
    super(files, s, pm);
    remapper = new FileRootRemapper(outDir, roots);
  }

  /* (non-Javadoc)
   * @see org.nrg.dcm.browse.FileWriter#getDestFile(java.io.File)
   */
  @Override
  File getDestFile(final File f) throws IOException {
    final File r = remapper.remap(f);
    r.getParentFile().mkdirs();
    log.debug("Rewriting " + f + " as " + r);
    return r;
  }

}
