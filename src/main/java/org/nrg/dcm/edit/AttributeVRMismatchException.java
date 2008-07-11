package org.nrg.dcm.edit;

import org.dcm4che2.data.VR;
import org.dcm4che2.util.TagUtils;

public class AttributeVRMismatchException extends AttributeException {
  static final long serialVersionUID = 1;
  
  AttributeVRMismatchException(int tag, VR vr) {
    super("Cannot use DICOM attribute " + TagUtils.toString(tag)
         + " -- uninterpretable type " + vr);
  }
}
