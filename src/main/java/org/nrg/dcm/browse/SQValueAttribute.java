/**
 * Copyright (c) 2012 Washington University
 */
package org.nrg.dcm.browse;

import java.util.Collections;
import java.util.Iterator;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.util.TagUtils;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class SQValueAttribute implements MultiValueAttribute {
    private static final DicomObject dcmo = new BasicDicomObject();
    private static final String[] VALUES = {};
    private static final String DISPLAY = "{sequence}";
    private static final String DELETED_DISPLAY = "{deleted sequence}";
    private final int tag;
    private boolean deleted = false;
    
    public SQValueAttribute(final int tag) {
        this.tag = tag;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<String> iterator() { return Collections.<String>emptyList().iterator(); }

    /* (non-Javadoc)
     * @see org.nrg.dcm.browse.MultiValueAttribute#getTag()
     */
    public int getTag() { return tag; }

    /* (non-Javadoc)
     * @see org.nrg.dcm.browse.MultiValueAttribute#size()
     */
    public int size() { return 0; }

    /* (non-Javadoc)
     * @see org.nrg.dcm.browse.MultiValueAttribute#getValues()
     */
    public String[] getValues() { return VALUES; }

    /* (non-Javadoc)
     * @see org.nrg.dcm.browse.MultiValueAttribute#getTagString()
     */
    public String getTagString() { return TagUtils.toString(tag); }

    /* (non-Javadoc)
     * @see org.nrg.dcm.browse.MultiValueAttribute#getNameString()
     */
    public String getNameString() { return dcmo.nameOf(tag); }

    /* (non-Javadoc)
     * @see org.nrg.dcm.browse.MultiValueAttribute#isModified()
     */
    public boolean isModified() { return deleted; }
    
    /*
     * (non-Javadoc)
     * @see org.nrg.dcm.browse.MultiValueAttribute#isModifiable()
     */
    public boolean isModifiable() { return false; }

    /*
     * (non-Javadoc)
     * @see org.nrg.dcm.browse.MultiValueAttribute#set(java.lang.String)
     */
    public void modify(String v) {
        throw new UnsupportedOperationException();
    }
    
    /*
     * (non-Javadoc)
     * @see org.nrg.dcm.browse.MultiValueAttribute#delete()
     */
    public void delete() {
        deleted = true;
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return deleted ? DELETED_DISPLAY : DISPLAY;
    }
}
