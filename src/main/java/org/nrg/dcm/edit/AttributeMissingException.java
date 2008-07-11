package org.nrg.dcm.edit;

import org.dcm4che2.util.TagUtils;

public class AttributeMissingException extends AttributeException {
  static final long serialVersionUID = 1;
  
  public AttributeMissingException(int tag) {
    super("Cannot use DICOM attribute " + TagUtils.toString(tag)
        + ": attribute is missing");
  }
}
