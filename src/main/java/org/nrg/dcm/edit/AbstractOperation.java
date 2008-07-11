/**
 * 
 */
package org.nrg.dcm.edit;


/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public abstract class AbstractOperation implements Operation {
  private final int tag;
  
  AbstractOperation(final int tag) { this.tag = tag; }
  
  final public int getTag() { return tag; }
}
