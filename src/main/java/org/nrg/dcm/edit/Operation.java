package org.nrg.dcm.edit;

import java.util.Map;

import org.dcm4che2.data.DicomObject;

public interface Operation {
  Action makeAction(DicomObject o) throws AttributeException;
  String apply(Map<Integer,String> vals);
  int getTag();
  
  public String getName();
}
