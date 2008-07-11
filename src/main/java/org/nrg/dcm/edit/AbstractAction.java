package org.nrg.dcm.edit;

/**
 * Skeleton class for an action on a specific DICOM attribute
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
abstract class AbstractAction implements Action {
  private final int tag;
  
  /**
   * @param tag specifies the attribute to be affected
   */
  AbstractAction(int tag) { this.tag = tag; }
  public final int getTag() { return tag; }
}
