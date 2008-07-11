package org.nrg.dcm.edit;

import java.util.Map;

import org.dcm4che2.data.DicomObject;

import org.nrg.dcm.edit.AbstractAction;

/**
 * Null operation
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public class NoOp extends AbstractOperation {
  private final static String id = "NoOp";
  public NoOp(final int tag) { super(tag); }

  public Action makeAction(DicomObject o) throws AttributeException {
    return new AbstractAction(getTag()) {
      public void apply() {}
      @Override
      public String toString() { return id; }
    };
  }

  public String apply(Map<Integer,String> vals) {
    return vals.get(getTag());
  }
  
  @Override     // all NoOp objects are equivalent
  public boolean equals(Object o) { return o instanceof NoOp; }
  
  @Override
  public int hashCode() { return 41; }
  
  @Override
  public String toString() { return id; }
  
  public String getName() { return id; }
}
