/**
 * $Id: NewAssignment.java,v 1.1 2008/03/18 06:12:11 karchie Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.dcm.edit;

import java.util.HashMap;
import java.util.Map;

import org.dcm4che2.data.DicomObject;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public final class NewAssignment extends AbstractOperation {
  public interface Generator {
    public String valueFor(final String old);
  }
  
  public final static class NoGeneratorDefined extends RuntimeException {
    private final static long serialVersionUID = 1L;
    public NoGeneratorDefined(final Label label) {
      super("no generator defined for label " + label);
    }
  }
  
  public final static class Scope {
    private final Map<Label,Generator> generators = new HashMap<Label,Generator>();
    private final Map<Label,Map<Integer,Map<String,String>>> maps = new HashMap<Label,Map<Integer,Map<String,String>>>();
        
    public void setGenerator(final Label label, final Generator generator) {
      if (generators.containsKey(label))
        throw new IllegalArgumentException("redefining generator for label " + label);
      generators.put(label, generator);
      maps.put(label, new HashMap<Integer,Map<String,String>>());
    }
    
    public String getValue(final Label label, final int tag, final String old) {
      if (!generators.containsKey(label))
        throw new NoGeneratorDefined(label);
      assert maps.containsKey(label);
      final Map<Integer,Map<String,String>> attrMap = maps.get(label);
      if (!attrMap.containsKey(tag))
        attrMap.put(tag, new HashMap<String,String>());
      final Map<String,String> valueMap = attrMap.get(tag);
      final String v = valueMap.get(old);
      if (null == v) {
        final String newv = generators.get(label).valueFor(old);
        valueMap.put(old, newv);
        return newv;
      } else {
        return v;
      }
    }
  }
  
  private final Scope scope;
  private final Label label;
  
  public NewAssignment(final Scope scope, final int tag, final Label label) {
    super(tag);
    this.scope = scope;
    this.label = label;
  }
  
  /* (non-Javadoc)
   * @see org.nrg.dcm.edit.Operation#apply(java.util.Map)
   */
  public String apply(Map<Integer,String> vals) {
    final int tag = getTag();
    return scope.getValue(label, tag, vals.get(tag));
  }

  /* (non-Javadoc)
   * @see org.nrg.dcm.edit.Operation#getName()
   */
  public String getName() { return "Assign-NEW"; }

  /* (non-Javadoc)
   * @see org.nrg.dcm.edit.Operation#makeAction(org.dcm4che2.data.DicomObject)
   */
  public Action makeAction(final DicomObject o) throws AttributeException {
    final int tag = getTag();
    return new AbstractAction(tag) {
      public void apply() {
        final String v = o.getString(tag);  // TODO: arguably should be smarter about various VRs
        if (null != v) {
          o.putString(tag, o.vrOf(tag), scope.getValue(label, tag, v));
        }
      }
    };
  }
}
