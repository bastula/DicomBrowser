package org.nrg.dcm.edit;

import java.util.Map;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.util.TagUtils;

public class Deletion extends AbstractOperation {
  public Deletion(final int tag) { super(tag); }
  
  public Action makeAction(final DicomObject o) throws AttributeException {
   return new AbstractAction(getTag()) {
      public void apply() { o.remove(getTag()); }
      @Override
      public String toString() {
        return getName() + " " + TagUtils.toString(getTag());
      }
    };
  }
  
  public String apply(Map<Integer,String> vals) { return null; }
  
  @Override
  public String toString() {
    return getName() + TagUtils.toString(getTag());
  }
  
  public String getName() { return "Delete"; }
}
