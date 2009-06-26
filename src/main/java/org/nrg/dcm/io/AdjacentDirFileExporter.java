/**
 * Copyright (c) 2009 Washington University
 */
package org.nrg.dcm.io;

import java.io.File;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public final class AdjacentDirFileExporter extends AbstractFileExporter
implements DicomObjectExporter {
    private final String format;

    /**
     * @param aeTitle
     * @param format
     */
    public AdjacentDirFileExporter(final String aeTitle, final String format) {
	super(aeTitle);
	this.format = format;
    }

    /* (non-Javadoc)
     * @see org.nrg.dcm.io.AbstractFileExporter#map(java.io.File)
     */
    @Override
    protected File map(File source) {
	final File origparent = source.getParentFile();
	final String newparent = String.format(format, origparent.getName());
	final File destdir = new File(origparent.getParentFile(), newparent);
	destdir.mkdir();
	return new File(destdir,source.getName());
    }

}
