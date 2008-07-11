/**
 * 
 */
package org.nrg.dcm.browse;

import java.lang.Iterable;
import java.util.Arrays;
import java.util.Iterator;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.util.TagUtils;

/**
 * Represents a single DICOM attribute over multiple files, potentially
 * with multiple values.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
final class MultiValueAttribute implements Iterable<String> {
  private static final DicomObject dcmo = new BasicDicomObject();       // used for attribute translation
  private static final String EMPTY_ATTRIBUTE_VALUE = "";
  private final int tag;
  private final String[] values;
  private final boolean modified;

  MultiValueAttribute(int tag, boolean isModified, String...values) {
    this.tag = tag;
    this.modified = isModified;
    this.values = values;
  }

  int getTag() { return tag; }

  int size() { return values.length; }

  public Iterator<String> iterator() { return Arrays.asList(values).iterator(); }

  String[] getValues() { 
    final String[] copy = new String[values.length];
    System.arraycopy(values, 0, copy, 0, values.length);
    return copy;
  }

  String getTagString() { return TagUtils.toString(tag); }

  String getNameString() { return dcmo.nameOf(tag); }

  boolean isModified() { return modified; }
  
  @Override
  public String toString() {
    final int n = values.length;
    switch (n) {
    case 0: return EMPTY_ATTRIBUTE_VALUE;
    case 1: return values[0];
    default:
      StringBuilder sb = new StringBuilder();
      sb.append(n);
      sb.append(" values: "); // TODO: LOCALIZE THIS
      int i = 0;
      while (i < n-1 && i < FileSetTableModel.MAX_VALUES_SHOWN - 1) {
        sb.append(values[i++]);
        sb.append(", ");
      }
      sb.append(values[i++]);
      if (i < n) sb.append(FileSetTableModel.MORE_VALUES);
      return sb.toString();
    }
  }
}