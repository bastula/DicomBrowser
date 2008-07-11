/**
 * $Id: ConstraintConjunction.java,v 1.3 2008/01/31 21:46:07 karchie Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.dcm.edit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.dcm4che2.data.DicomObject;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public class ConstraintConjunction implements ConstraintMatch {
  private final Collection<ConstraintMatch> predicates;
  
  public ConstraintConjunction(final ConstraintMatch...predicates) {
    this(Arrays.asList(predicates));
  }
  
  public ConstraintConjunction(final Collection<ConstraintMatch> predicates) {
    this.predicates = new ArrayList<ConstraintMatch>(predicates);
  }
  
  /* (non-Javadoc)
   * @see org.nrg.dcm.edit.ConstraintMatch#matches(org.dcm4che2.data.DicomObject)
   */
  public boolean matches(final DicomObject o) {
    for (final ConstraintMatch predicate : predicates) {
      if (!predicate.matches(o))
	return false;
    }
    return true;
  }
  
  public String toString() {
    final StringBuilder sb = new StringBuilder("ConstraintConjunction: ");
    final Iterator<ConstraintMatch> i = predicates.iterator();
    if (i.hasNext()) {
      sb.append(i.next());
    } else {
      sb.append("[TRUE]");
    }
    while (i.hasNext()) {
      sb.append(" AND ");
      sb.append(i.next());
    }
    return sb.toString();
  }

}
