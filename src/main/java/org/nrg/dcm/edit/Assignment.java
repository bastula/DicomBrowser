/**
 * $Id: Assignment.java,v 1.2 2007/08/22 18:20:10 karchie Exp $
 * Copyright (c) 2006 Washington University
 */
package org.nrg.dcm.edit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.lang.StringBuilder;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.util.TagUtils;

import org.nrg.attr.ConversionFailureException;
import org.nrg.dcm.DataSetAttrs;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public class Assignment extends AbstractOperation {
  private final String format;
  private final Integer[] tags;

  public Assignment(int tag, String value) { this(tag, value, null); }

  public Assignment(int tag, String format, List<Integer> tags) {
    super(tag);
    this.format = format;
    this.tags = (tags == null) ? null : tags.toArray(new Integer[1]);
  }

  public Assignment(int ltag, int rtag) {
    super(ltag);
    this.format = "%1$s";
    this.tags = new Integer[]{rtag};
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Assignment) {
      Assignment oa = (Assignment) o;
      if (format == null && oa.format != null) return false;
      if (getTag() != oa.getTag()) return false;
      return (format == oa.format) || format.equals(oa.format);
    } else return false;
  }

  @Override
  public int hashCode() {
    int accum = 17;
    if (format != null) accum += 37*format.hashCode();
    if (tags != null) accum += 37*Arrays.hashCode(tags);
    return accum;
  }

  public Action makeAction(final DicomObject o) throws AttributeException {
    final String value;
    if (tags == null || tags.length == 0) {
      value = format;
    } else {
      final Object[] args = new String[tags.length];
      for (int i = 0; i < tags.length; i++) try {
        args[i] = DataSetAttrs.convert(o, tags[i]);
      } catch (ConversionFailureException e) {
        throw new AttributeVRMismatchException(tags[i], o.vrOf(getTag()));
      }
      value = String.format(format, args);
    }

    return new AbstractAction(getTag()) {
      public void apply() {
        o.putString(new int[]{getTag()}, o.vrOf(getTag()), value);
      }
      @Override
      public String toString() {
        return String.format("%1$s: %2$s := %3$s",
            getName(), TagUtils.toString(getTag()), value);
      }
    };
  }

  public String apply(Map<Integer,String> vals) {
    if (tags == null || tags.length == 0)
      return format;
    final Object[] args = new String[tags.length];
    for (int i = 0; i < tags.length; i++)
      args[i] = vals.get(tags[i]);
    return String.format(format, args);
  }


  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getName());
    sb.append(" ");
    sb.append(TagUtils.toString(getTag()));
    sb.append(" := \"");
    sb.append(format);
    sb.append("\"");
    if (tags != null) {
      sb.append(" using");
      for (final int tag : tags) {
        sb.append(" ");
        sb.append(TagUtils.toString(tag));
      }
    }
    return sb.toString();
  }

  public String getName() { return "Assign"; }
}
