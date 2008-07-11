package org.nrg.dcm.edit;

public class ConstraintRegexpMatch extends SimpleConstraintMatch {
  ConstraintRegexpMatch(int tag, String pattern) {
    super(tag, pattern);
  }
  
  @Override
  public boolean matches(String value) {
    return value.matches(getPattern());
  }
}
