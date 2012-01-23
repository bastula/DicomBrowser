/*
 * Copyright (c) 2012 Washington University
 */
package org.nrg.dcm.browse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;

import javax.swing.JFrame;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class CStoreDialogTest {
    private static final String SERVICE_1 = "AU#my.host:8104:AP";
    private static final String SERVICE_2 = "BU#my.other.host:104:BP";
    private static final String SERVICE_BROKEN = "bzzt!";
    private DicomBrowser browser;
    private FileSetTableModel model;
    private JFrame frame;
    private Preferences prefs;

    @Before
    public void setUp() {
        InterfaceCounter.getInstance().register(this);
        browser = mock(DicomBrowser.class);
        model = mock(FileSetTableModel.class);
        frame = new JFrame();
        prefs = mock(Preferences.class);
    }

    @After
    public void tearDown() {
        browser = null;
        model = null;
        frame = null;
        prefs = null;
        InterfaceCounter.getInstance().unregister(this, false);
    }

    @Test
    public void testCStoreDialogSimpleHistory() {
        when(browser.getFrame()).thenReturn(frame);
        when(browser.getPrefs()).thenReturn(prefs);
        when(browser.isShowing()).thenReturn(false);    // for setLocationRelativeTo()
        when(prefs.get(matches("AE-history"), anyString())).thenReturn(SERVICE_1);

        final CStoreDialog dialog = new CStoreDialog(browser, model);

        final ActionEvent ev = new ActionEvent(this, 0, "Send");

        dialog.actionPerformed(ev);

        verify(model).send(eq("my.host"), eq("8104"), eq("AP"), eq(false), eq("AU"), eq(true));
        verify(prefs).put(eq("AE-history"), eq(SERVICE_1));
    }

    @Test
    public void testCStoreDialogCompoundHistory() {
        when(browser.getFrame()).thenReturn(frame);
        when(browser.getPrefs()).thenReturn(prefs);
        when(browser.isShowing()).thenReturn(false);    // for setLocationRelativeTo()
        when(prefs.get(matches("AE-history"), anyString()))
        .thenReturn(SERVICE_1 + "," + SERVICE_2);

        final CStoreDialog dialog = new CStoreDialog(browser, model);

        final ActionEvent ev = new ActionEvent(this, 0, "Send");

        dialog.actionPerformed(ev);

        verify(model).send(eq("my.other.host"), eq("104"), eq("BP"), eq(false), eq("BU"), eq(true));
        verify(prefs).put(eq("AE-history"), eq(SERVICE_1 + "," + SERVICE_2));
    }

    @Test
    public void testCStoreDialogBrokenHistory() {
        when(browser.getFrame()).thenReturn(frame);
        when(browser.getPrefs()).thenReturn(prefs);
        when(browser.isShowing()).thenReturn(false);    // for setLocationRelativeTo()
        when(prefs.get(matches("AE-history"), anyString()))
        .thenReturn(SERVICE_1 + "," + SERVICE_BROKEN);

        final CStoreDialog dialog = new CStoreDialog(browser, model);

        final ActionEvent ev = new ActionEvent(this, 0, "Send");

        dialog.actionPerformed(ev);

        verify(model).send(eq("my.host"), eq("8104"), eq("AP"), eq(false), eq("AU"), eq(true));
        verify(prefs).put(eq("AE-history"), eq(SERVICE_1));
    }

}
