/**
 * Copyright (c) 2012 Washington University
 */
package org.nrg.dcm.io;


/**
 * Operations for saving sets of files to disk
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public interface MultifileExporter {
    void saveInAdjacentDir(String format, boolean saveAllFiles);

    void saveInAdjacentFile(String suffix, boolean saveAllFiles);
    
    void saveInNewRoot(String rootpath, boolean allFiles);

    void overwrite(final boolean allFiles);
}
