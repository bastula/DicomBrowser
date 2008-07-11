package org.nrg.dcm.edit;

/**
 * Represents an action to be performed on a specific DICOM attribute
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public interface Action {
  /**
   * @return the tag of the affected attribute
   */
  public int getTag();
  
  /**
   * Performs the associated action.
   */
  public void apply();
}
