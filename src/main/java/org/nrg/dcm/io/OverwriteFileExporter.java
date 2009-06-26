/**
 * Copyright (c) 2009 Washington University
 */
package org.nrg.dcm.io;

import java.io.File;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public final class OverwriteFileExporter
extends AbstractFileExporter implements DicomObjectExporter {

    /**
     * @param aeTitle
     */
    public OverwriteFileExporter(String aeTitle) {
	super(aeTitle);
    }

    /* (non-Javadoc)
     * @see org.nrg.dcm.io.AbstractFileExporter#map(java.io.File)
     */
    @Override
    protected File map(final File source) {
	return source;
    }
}
