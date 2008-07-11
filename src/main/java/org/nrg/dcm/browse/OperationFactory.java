/**
 * 
 */
package org.nrg.dcm.browse;

import org.nrg.dcm.edit.Operation;
import org.nrg.dcm.edit.NoOp;
import org.nrg.dcm.edit.Assignment;
import org.nrg.dcm.edit.Deletion;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
final class OperationFactory {
  public static final String KEEP = "Keep";
  public static final String ASSIGN = "Assign";
  public static final String CLEAR = "Clear";
  public static final String DELETE = "Delete";
  private static final String MULTIPLE = "(Mixed)";
  
  private static final String CLEAR_VALUE = "";

  private static final String[] all = { KEEP, ASSIGN, CLEAR, DELETE };  // TODO: LOCALIZE

  private OperationFactory() { }        // prevent instantiation
  
  final static String getDefaultName() { return KEEP; } // TODO: LOCALIZE
  
  final static String getMultipleName() { return MULTIPLE; } // TODO: LOCALIZE
  
  final static String[] getOperationNames() { return all; }   // TODO: LOCALIZE
  
  
  static final Operation getInstance(final String name, final int tag) {
    return getInstance(name, tag, null);
  }
  
  static final Operation getInstance(final String name, final int tag, final String value) {
    if (KEEP.equals(name)) {
      assert value == null;
      return new NoOp(tag) { @Override public String getName() { return KEEP; }} ;
    } else if (ASSIGN.equals(name)) {
      return new Assignment(tag, value) { @Override public String getName() { return ASSIGN; }};
    } else if (CLEAR.equals(name)) {
      assert value == null;
      return new Assignment(tag, CLEAR_VALUE) { @Override public String getName() { return CLEAR; }};
    } else if (DELETE.equals(name)) {
      assert value == null;
      return new Deletion(tag) { @Override public String getName() { return DELETE; }};
    } else
      throw new IllegalArgumentException("unrecognized command " + name);
  }
}
