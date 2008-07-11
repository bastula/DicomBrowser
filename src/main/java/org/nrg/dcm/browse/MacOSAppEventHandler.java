/**
 * $Id: MacOSAppEventHandler.java,v 1.1 2007/01/04 04:06:42 karchie Exp $
 * Copyright (c) 2007 Washington University
 */
package org.nrg.dcm.browse;

import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;
import com.apple.eawt.Application;
import javax.swing.JFrame;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 * Handle MacOS ApplicationEvents to be iFruitier.
 */
final class MacOSAppEventHandler extends Application implements ApplicationListener {
	private final JFrame owner;
	private final String title;
	
	public MacOSAppEventHandler(final JFrame owner, final String title) {
		this.owner = owner;
		this.title = title;
		
		addApplicationListener(this);
	}

	public void handleAbout(final ApplicationEvent e) {
		new AboutDialog(owner, title).setVisible(true);
		e.setHandled(true);
	}
	
	public void handleQuit(final ApplicationEvent e) {
		System.exit(0);	// TODO: don't abandon operations in progress
		e.setHandled(true);
	}
	
	public void handleOpenApplication(final ApplicationEvent e) {}
	public void handleReOpenApplication(final ApplicationEvent e) {}
	public void handleOpenFile(final ApplicationEvent e) {}
	public void handlePrintFile(final ApplicationEvent e) {}
	public void handlePreferences(final ApplicationEvent e) {}
}
