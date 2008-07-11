/**
 * $Id: Echo.java,v 1.1 2006/12/22 21:10:46 karchie Exp $
 * Copyright (c) 2006 Washington University
 */
package org.nrg.dcm.edit;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.util.TagUtils;

import org.nrg.dcm.DataSetAttrs;
import org.nrg.attr.ConversionFailureException;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public final class Echo implements Operation {
  private final static int MAX_VALUE_LEN = 64;
  private final static String LINE_SEPARATOR = System.getProperty("line.separator");
  private final String format;
  private final Integer[] tags;

  public Echo(final String format, List<Integer> tags) {
    this.format = format;
    this.tags = (tags == null) ? null : tags.toArray(new Integer[0]);
  }

  /* (non-Javadoc)
   * @see org.nrg.dcm.edit.Operation#makeAction(org.dcm4che2.data.DicomObject)
   */
  @SuppressWarnings("unchecked")        // for DicomObject.datasetIterator()
  public Action makeAction(final DicomObject o) {
    final String message;
    if (format == null) {
      final List<Integer> tagSet = new LinkedList<Integer>();
      if (tags == null) {
        for (Iterator<DicomElement> ei = o.datasetIterator(); ei.hasNext(); tagSet.add(ei.next().tag()))
          ;     // empty statement
      } else {
        tagSet.addAll(Arrays.asList(tags));
      }

      final StringBuilder sb = new StringBuilder();
      for (final int tag : tagSet) {
        sb.append(TagUtils.toString(tag));
        sb.append(" ");
        try {
          String val = DataSetAttrs.convert(o, tag);
          if (val != null && val.length() > MAX_VALUE_LEN) {
            sb.append(val.substring(0, MAX_VALUE_LEN));
            sb.append("...");
          } else
            sb.append(val);
         } catch (ConversionFailureException e) {
          sb.append("[ ");
          sb.append(TagUtils.toString(tag));
          sb.append(" can not be displayed : ");
          sb.append(e.getMessage());
          sb.append(" ]");
        }
        sb.append(LINE_SEPARATOR);
      }
      message = sb.toString();
    } else {
      final int nargs = tags == null ? 0 : tags.length;
      final Object[] args = new String[nargs];
      for (int i = 0; i < nargs; i++) try {
        args[i] = DataSetAttrs.convert(o, tags[i]);
      } catch (ConversionFailureException e) {
        args[i] = String.format("[ %1$s can not be displayed: %2$s]",
            TagUtils.toString(tags[i]), e.getMessage());
      }
      message = String.format(format, args);
    }

    return new AbstractAction((tags != null && tags.length > 0) ? tags[0] : -1) {
      public void apply() { System.out.print(message); }
      @Override
      public String toString() { return getName() + ": " + message; }
    };
  }

  public String apply(Map<Integer,String> vals) { return null; }

  public int getTag() { return -1; }
  
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getName());
    if (format != null) {
      sb.append("\"");
      sb.append(format);
      sb.append("\"");
    }
    if (tags != null) {
      sb.append(" using");
      for (final int tag : tags) {
        sb.append(" ");
        sb.append(TagUtils.toString(tag));
      }
    }
    return sb.toString();
  }
  
  public String getName() { return "Echo"; }
}
