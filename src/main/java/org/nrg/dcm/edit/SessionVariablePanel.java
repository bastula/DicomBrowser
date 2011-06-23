/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.dcm.edit;

import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.google.common.collect.Multimap;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class SessionVariablePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final int count;

    public SessionVariablePanel(final List<?> variables, final Multimap<Integer,String> values) {
        super(new GridBagLayout());
        count = new VariableAssignmentManager(this, variables, values).getVisibleRows();
    }

    public int count() { return count; }

    public static boolean withAssignedVariables(final List<?> variables,
            final Multimap<Integer,String> values) {
        final SessionVariablePanel panel = new SessionVariablePanel(variables, values);

        if (0 == panel.count()) {
            return true;    // no variables need to be set, just move on.
        } else {
            final JFrame frame = new JFrame("Set script variables");
            final int n = JOptionPane.showOptionDialog(frame,
                    panel,
                    "Set script variables",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    new Object[]{"Apply", "Cancel"},
                    "Apply");
            return 0 == n;
        }
    }
}
