/**
 * $Id: ConstraintMatch.java,v 1.2 2008/01/18 21:59:40 karchie Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.dcm.edit;

import org.dcm4che2.data.DicomObject;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
interface ConstraintMatch {
  boolean matches(DicomObject o);
}
