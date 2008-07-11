/**
 * $Id: SimpleConstraintMatch.java,v 1.1 2008/01/18 21:59:40 karchie Exp $
 * Copyright (c) 2006, 2008 Washington University
 */
package org.nrg.dcm.edit;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.VR;
import org.dcm4che2.util.StringUtils;
import org.dcm4che2.util.TagUtils;


class SimpleConstraintMatch implements ConstraintMatch {
  private final int tag;
  private final String pattern;
  
  SimpleConstraintMatch(final int tag, final String pattern) {
    this.tag = tag; this.pattern = pattern;
  }
  
  final String getPattern() { return pattern; }
  
  boolean matches(final String value) {
    return value.equals(pattern);
  }
  
  public boolean matches(final DicomObject o) {
    if (o.contains(tag)) {
      VR vr = o.vrOf(tag);
      String value;
      if (vr == VR.SQ) 
        throw new RuntimeException("can't use SQ type attribute for constraint");
      if (vr == VR.UN)
        value = o.getString(tag);
      else
        value = StringUtils.join(o.getStrings(tag), '\\');
      return matches(value);
    } else
      return false;
  }

  @Override
  public String toString() {
    return "Constraint: " + TagUtils.toString(tag) + " matches " + pattern;
  }
}
