/**
 * $Id: Constraint.java,v 1.1 2006/12/22 21:10:46 karchie Exp $
 * Copyright (c) 2006 Washington University
 */
package org.nrg.dcm.edit;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

import org.dcm4che2.data.DicomObject;

/**
 * Represents a constraint on an Operation.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public class Constraint {
  private final ConstraintMatch m;
  private final Set<File> files;
  
  public Constraint(final ConstraintMatch m, final File...fs) {
    this(m, Arrays.asList(fs));
  }

  public Constraint(final ConstraintMatch m, final Collection<File> fs) {
    this.m = m;
    files = new HashSet<File>(fs);
    if (m == null && files.isEmpty())
      throw new IllegalArgumentException("Either value constraint or file list must be provided");
  }

  boolean matches(final File f, final DicomObject o) {
    assert m != null || !files.isEmpty();
    if (m != null && !m.matches(o)) return false;
    return files.isEmpty() || files.contains(f);
  }
  
  @Override
  public String toString() { return m.toString(); }
}
