/**
 * Copyright (c) 2009 Washington University
 */
package org.nrg;

import java.util.Iterator;

/**
 * I've defined these utilities elsewhere.  I really need a common place for them.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public final class StringUtils {
  private StringUtils() {}
  
  public static StringBuilder join(final StringBuilder sb, final Iterable<?> values, final String separator) {
    final Iterator<?> i = values.iterator();
    if (i.hasNext()) sb.append(i.next());
    while (i.hasNext()) sb.append(separator).append(i.next());
    return sb;
  }
  
  public static String join(final Iterable<?> values, final String separator) {
    return join(new StringBuilder(), values, separator).toString();
  }
}
