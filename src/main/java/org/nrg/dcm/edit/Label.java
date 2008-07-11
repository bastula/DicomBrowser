/**
 * $Id: Label.java,v 1.1 2008/03/18 06:12:11 karchie Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.dcm.edit;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public final class Label {
  public static class AlreadyDefinedException extends RuntimeException {
    private final static long serialVersionUID = 1L;
    AlreadyDefinedException(final String s) {
      super("Label already defined: " + s);
    }
  }
  
  public static class Table {
    private final Map<String,Label> defined = new HashMap<String,Label>();
    
    final Label add(final String s) throws AlreadyDefinedException {
      final String canonical = s.toLowerCase();
      if (defined.containsKey(canonical)) {
        throw new AlreadyDefinedException(canonical);
      }
      return defined.put(canonical, new Label(canonical));
    }
    
    final Label get(final String s) {
      return defined.get(s.toLowerCase());
    }
  }
  
  private final String label;
  
  private Label(final String label) {
    this.label = label;
  }
  
  public String toString() { return label; }
}
