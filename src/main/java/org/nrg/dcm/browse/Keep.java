/**
 * Copyright (c) 2012 Washington University
 */
package org.nrg.dcm.browse;

import java.util.Map;
import java.util.SortedSet;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.util.TagUtils;
import org.nrg.dcm.edit.AbstractOperation;
import org.nrg.dcm.edit.Action;
import org.nrg.dcm.edit.AttributeException;
import org.nrg.dcm.edit.Operation;

import com.google.common.collect.ImmutableSortedSet;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public final class Keep extends AbstractOperation implements Operation {
    private final int tag;
    private final SortedSet<Integer> tags;
    
    public Keep(final int tag) {
        super("Keep");
        this.tags = ImmutableSortedSet.of(this.tag = tag);
    }
    
    /*
     * (non-Javadoc)
     * @see org.nrg.dcm.edit.Operation#affects(int)
     */
    public boolean affects(final int tag) {
	return this.tag == tag;
    }
    
    public int getTopTag() {
	return tag;
    }
    
    /* (non-Javadoc)
     * @see org.nrg.dcm.edit.Operation#makeAction(org.dcm4che2.data.DicomObject)
     */
    public Action makeAction(final DicomObject o) throws AttributeException {
        return new Action() {
            public void apply() {}
        };
    }

    /* (non-Javadoc)
     * @see org.nrg.dcm.edit.Operation#apply(java.util.Map)
     */
    public String apply(Map<Integer,String> vals) { return vals.get(tag); }

    /* (non-Javadoc)
     * @see org.nrg.dcm.edit.Operation#getAffectedTags()
     */
    public SortedSet<Integer> getAffectedTags() { return tags; }

    /* (non-Javadoc)
     * @see org.nrg.dcm.edit.Operation#getRequiredTags()
     */
    public SortedSet<Integer> getRequiredTags() { return tags; }
    
    public String toString() {
        return "Keep " + TagUtils.toString(tag);
    }
}
