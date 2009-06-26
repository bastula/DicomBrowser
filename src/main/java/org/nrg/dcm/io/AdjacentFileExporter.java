/**
 * Copyright (c) 2009 Washington University
 */
package org.nrg.dcm.io;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public final class AdjacentFileExporter extends AbstractFileExporter
implements DicomObjectExporter {
    private final String suffix;
    
    /**
     * @param aeTitle
     */
    public AdjacentFileExporter(final String aeTitle, final String suffix) {
	super(aeTitle);
	this.suffix = suffix;
    }

    private final Pattern DCM_SUFFIX_PATTERN = Pattern.compile("(.*)(\\.[dD][cC][mM](?:\\.gz))");
    
    /* (non-Javadoc)
     * @see org.nrg.dcm.io.AbstractFileExporter#map(java.net.URI)
     */
    protected File map(final File source) {
	final Matcher matcher = DCM_SUFFIX_PATTERN.matcher(source.getName());
	final StringBuilder name;
	if (matcher.matches()) {
	    name = new StringBuilder(matcher.group(1));
	    name.append(suffix);
	    name.append(matcher.group(2));
	} else {
	    name = new StringBuilder(matcher.group(0));
	    name.append(suffix);
	}
	return new File(source.getParentFile(), name.toString());
    }
}
