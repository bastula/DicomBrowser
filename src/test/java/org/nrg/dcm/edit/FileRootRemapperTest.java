/**
 * $Id: FileRootRemapperTest.java,v 1.1 2008/01/31 23:21:09 karchie Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.dcm.edit;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.nrg.dcm.io.FileRootRemapper;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public class FileRootRemapperTest {
  private final File newRoot = new File("c:/Documents and Settings/karchie/start");
  private final File[] roots = {
      new File("c:/Documents and Settings/karchie/foo"),
      new File("c:/Documents and Settings/karchie/bar"),
      new File("c:/baz"),
  };
  private final File coveredRoot = new File("c:/Documents and Settings/karchie/foo/start");
  
  private final File[] files = {
      new File("c:/Documents and Settings/karchie/foo/bar/test1.baz"),
      new File("c:/Documents and Settings/karchie/foo/baz/test2.bar"),
      new File("c:/Documents and Settings/karchie/foo/bar/bar/test3.baz"),
  };
  
  /**
   * Test method for {@link org.nrg.dcm.io.FileRootRemapper#FileRootRemapper(java.io.File, java.util.Collection)}.
   */
  @Test
  public final void testFileRootRemapper() {
    FileRootRemapper remapper;
    try {
      remapper = new FileRootRemapper(newRoot, Arrays.asList(roots));
    } catch (IOException e) {
      fail(e.getMessage());
      return;
    }
    System.out.println(remapper);	// TODO: real test
    
    try {
      new FileRootRemapper(coveredRoot, Arrays.asList(roots));
      fail("missed expected IOException: cannot have new root under source root");
    } catch (IOException success) {}
  }

  /**
   * Test method for {@link org.nrg.dcm.io.FileRootRemapper#toString()}.
   */
  @Test
  public final void testToString() {
    fail("Not yet implemented"); // TODO
  }

  /**
   * Test method for {@link org.nrg.dcm.io.FileRootRemapper#remap(java.io.File)}.
   */
  @Test
  public final void testRemap() throws IOException {
    final FileRootRemapper remapper = new FileRootRemapper(newRoot, Arrays.asList(roots));
    for (final File file : files) {
      System.out.println(file + " -> " + remapper.remap(file));
    }
    fail("Not yet implemented"); // TODO
  }

}
