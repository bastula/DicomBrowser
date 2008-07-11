/**
 * $Id: UIDGenerator.java,v 1.8 2008/04/07 20:48:26 karchie Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.dcm.edit;

import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences;

import org.nrg.dcm.browse.DicomBrowser;
import org.nrg.dcm.edit.NewAssignment.Generator;

/**
 * Generates fresh UIDs based on a specified root.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public final class UIDGenerator implements Generator {
  private final static String LAST_FRAG_PREF = "last.frag";
  
  private final Preferences uidFragPrefs;
  private final String root;
  private int lastFrag;
  private final StringBuilder messages = new StringBuilder();
  
  public UIDGenerator(final Preferences prefs, final String uidRootPref)
  throws InvalidUIDRootException {
    this(prefs, uidRootPref, null, null);
   }
  
  /**
   * @param prefs User preferences object
   * @param uidRootPref Name of user preference containing UID root
   * @param uidRootProperty UID root, or null if preference should be used
   * @param newRootServer URL of web service that provides a UID root
   * @throws InvalidUIDRootException
   */
  public UIDGenerator(final Preferences prefs, final String uidRootPref, final String specUIDRoot, final URL newRootServer)
  throws InvalidUIDRootException {
    final String defaultRoot = prefs.get(uidRootPref, null);
    if (null == specUIDRoot) {
      if (null == defaultRoot && null != newRootServer) {
	String reqRoot;
	try {
	  reqRoot = new UIDRequester(newRootServer).getUID();
	  if (null != reqRoot)
	    prefs.put(uidRootPref, reqRoot);
	} catch (IOException e) {
	  messages.append(e.getMessage());
	  reqRoot = null;
	}
	root = reqRoot + ".";
      } else {
	root = defaultRoot + ".";
      }
    } else {
      root = specUIDRoot + ".";
    }

    if (null == root || !UIDRequester.isUID(root + "0")) {
      throw new InvalidUIDRootException(root);
    }
    
    uidFragPrefs = prefs.node(root);
    lastFrag = uidFragPrefs.getInt(LAST_FRAG_PREF, -1);
  }
  
  private final int getNextFrag() {
    uidFragPrefs.putInt(LAST_FRAG_PREF, ++lastFrag);
    return lastFrag;
  }
  
  /* (non-Javadoc)
   * @see org.nrg.dcm.edit.NewAssignment.Generator#valueFor(java.lang.String)
   */
  public String valueFor(final String old) {
    final String uid = root + getNextFrag();
    assert UIDRequester.isUID(uid);
    return uid;
  }

  public final class InvalidUIDRootException extends Exception {
    private final static long serialVersionUID = 1L;
    
    InvalidUIDRootException(final String root) {
      super("Invalid UID root: " + root);
    }
  }
  
  /**
   * @param args
   */
  public static void main(final String[] args) throws Exception {
    System.out.println(new UIDGenerator(DicomBrowser.prefs, DicomBrowser.UID_ROOT_PREF).valueFor("dummy"));
  }
}
