/**
 * $Id: UIDRequesterTest.java,v 1.1 2008/04/02 22:21:05 karchie Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.dcm.edit;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public class UIDRequesterTest {

  /**
   * Test method for {@link org.nrg.dcm.edit.UIDRequester#isUID(java.lang.String)}.
   */
  @Test
  public final void testIsUID() {
    assertTrue(UIDRequester.isUID("0"));
    assertTrue(UIDRequester.isUID("1"));
    assertFalse(UIDRequester.isUID("01"));
    assertTrue(UIDRequester.isUID("10"));
    assertFalse(UIDRequester.isUID("0."));
    assertTrue(UIDRequester.isUID("0.0"));
    assertFalse(UIDRequester.isUID("0.0."));
    assertTrue(UIDRequester.isUID("0.10"));
    assertFalse(UIDRequester.isUID("0.01"));
    assertTrue(UIDRequester.isUID("1.20.333.4140.54321"));
  }

}
