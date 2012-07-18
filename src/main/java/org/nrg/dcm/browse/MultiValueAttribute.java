/**
 * Copyright (c) 2012 Washington University
 */
package org.nrg.dcm.browse;

import java.util.Collections;
import java.util.Set;

import org.nrg.dcm.FileSet;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public interface MultiValueAttribute extends Iterable<String> {
    int getTag();

    int size();

    String[] getValues();

    String getTagString();

    String getNameString();

    boolean isModified();

    boolean isModifiable();

    public class Factory {
        public final static MultiValueAttribute
        create(final FileSet fs, final int tag, final boolean isModified, final Set<String> values) {
            if (fs.isSequenceTag(tag)) {
                return new SQValueAttribute(tag);
            } else {
                return new SimpleMultiValueAttribute(tag, isModified, values);
            }
        }

        public final static MultiValueAttribute
        create(final FileSet fs, final int tag, final boolean isModified, final String value) {
            return create(fs, tag, isModified, Collections.singleton(value));
        }

        public final static MultiValueAttribute
        create(final FileSet fs, final int tag, final boolean isModified) {
            return create(fs, tag, isModified, Collections.<String>emptySet());
        }
    }
}
