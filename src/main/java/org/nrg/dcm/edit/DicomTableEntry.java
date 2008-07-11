/**
 * $Id: DicomTableEntry.java,v 1.4 2008/01/30 16:42:59 karchie Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.dcm.edit;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.util.TagUtils;

/**
 * @author Kevin A. Archie <karchie@alumni.caltech.edu>
 *
 */
public final class DicomTableEntry {
  private final String header;
  private final String level;
  private final int tag;
  private final boolean isSubstitution;

  public DicomTableEntry(final int tag, final String level, final boolean isSubstitution, final String header) {
    this.tag = tag;
    this.level = level;
    this.isSubstitution = isSubstitution;
    this.header = null == header ? new BasicDicomObject().nameOf(tag) : header;
  }

  public DicomTableEntry(final int tag, final String level) {
    this(tag, level, false, null);
  }

  public int getTag() { return tag; }
  public String getLevel() { return level; }
  public String getHeader() { return header; }
  public boolean isSubstitution() { return isSubstitution; }

  public boolean equals(Object o) {
    if (!(o instanceof DicomTableEntry)) return false;
    final DicomTableEntry other = (DicomTableEntry)o;
    return tag == other.tag && isSubstitution == other.isSubstitution
    	&& header.equals(other.header) && level.equals(other.level);
  }

  public int hashCode() {
    assert header != null;
    int result = 17;
    result = 37*result + header.hashCode();
    result = 37*result + level.hashCode();
    result = 37*result + tag;
    result = 37*result + (isSubstitution ? 1 : 0);
    return result;
  }

  public String toString() {
    final StringBuilder sb = new StringBuilder(TagUtils.toString(tag));
    sb.append(" (");
    sb.append(header);
    sb.append(") ");
    sb.append(":");
    sb.append(level);
    if (isSubstitution) {
      sb.append(" - substitution");
    }
    return sb.toString();
  }
}
