/**
 * Copyright (c) 2007,2012 Washington University
 */
package org.nrg.dcm.browse;

import java.util.Set;

import java.awt.Window;
import java.awt.event.WindowEvent;

import java.awt.event.WindowAdapter;

import com.google.common.collect.Sets;

import ij.IJ;
import ij.ImageJ;

/**
 * Singleton class to count active interfaces, either UI elements (like
 * Windows, or threads accessing external resources (like Exporters).
 *
 * When the last interface closes, looks for an active ImageJ.  If one
 * exists, it's now effectively standalone so tell it to exit the JVM
 * when it's done.  If there's no active ImageJ, call System.exit().
 *
 * @author Kevin A. Archie <karchie@wustl.edu>
 */
final class InterfaceCounter extends WindowAdapter {
    private static final InterfaceCounter wc = new InterfaceCounter();
    private final Set<Object> interfaces = Sets.newHashSet();

    private InterfaceCounter() {}

    public static InterfaceCounter getInstance() { return wc; }

    /**
     * Sets the given object as an active interface.
     * @param o
     */
    public void register(final Object o) {
        synchronized(interfaces) {
            interfaces.add(o);
        }
        if (o instanceof Window) {
            ((Window)o).addWindowListener(this);
        }
    }

    /**
     * Removes the given object from the active interfaces.
     * @param o
     * @param exitIfLast if true, and this object is the last one registered, shut down the JVM.
     */
    public void unregister(final Object o, boolean exitIfLast) {
        synchronized(interfaces) {
            interfaces.remove(o);
            if (interfaces.isEmpty() && exitIfLast) {
                final ImageJ ij = IJ.getInstance();
                if (null == ij || ij.quitting()) {
                    System.exit(0);
                } else {
                    ij.exitWhenQuitting(true);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see sun.awt.WindowClosingListener#windowClosingDelivered(java.awt.event.WindowEvent)
     */
    public void windowClosed(final WindowEvent e) {
        unregister(e.getWindow(), true);
    }
}
