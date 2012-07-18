/**
 * Copyright (c) 2006,2012 Washington University
 */
package org.nrg.dcm.browse;

import javax.swing.JPanel;
import java.awt.Frame;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JComboBox;
import javax.swing.JButton;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
final class AttributeEditDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;
    private final static String TITLE_FORMAT = "Edit %1$s %2$s";  // TODO: localize
    private final static String SET_LABEL = "New value:";         // TODO: localize
    private final static String SET_BUTTON = "Set";               // TODO: localize
    private final static String REVERT_BUTTON = "Revert";         // TODO: localize
    private final static String CANCEL_BUTTON = "Cancel";         // TODO: localize
    private final static String DELETE_BUTTON = "Delete";         // TODO: localize
    private final static String CLEAR_BUTTON = "Clear";           // TODO: localize

    private JPanel jContentPane = null;

    private JLabel setLabel = null;

    private final JComboBox setComboBox;

    private JButton deleteButton = null;

    private JButton clearButton = null;

    private JButton revertButton = null;

    private JButton cancelButton = null;

    private JButton setButton = null;

    private final int tag;
    private final AttrTableCellEditor editor;

    /**
     * @param owner
     */
    public AttributeEditDialog(final Frame owner, final AttrTableCellEditor editor,
            final MultiValueAttribute attr) {
        super(owner,
                String.format(TITLE_FORMAT, attr.getNameString(), attr.getTagString()),
                true);
        tag = attr.getTag();
        this.editor = editor;
        editor.setValue(attr);

        setComboBox = new JComboBox(attr.getValues());
        setComboBox.setEditable(true);

        initialize();

        revertButton.setEnabled(attr.isModified());

        this.setLocationRelativeTo(owner);

        // Window close should act as a cancel.
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                editor.stopCellEditing();
            }
        });
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setSize(460, 124);
        this.setContentPane(getJContentPane());
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            gridBagConstraints6.insets = new Insets(6, 18, 17, 12);
            gridBagConstraints6.gridy = 1;
            gridBagConstraints6.ipadx = 8;
            gridBagConstraints6.ipady = -1;
            gridBagConstraints6.anchor = GridBagConstraints.EAST;
            gridBagConstraints6.gridx = 4;
            GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            gridBagConstraints5.insets = new Insets(6, 6, 17, 17);
            gridBagConstraints5.gridy = 1;
            gridBagConstraints5.ipady = -1;
            gridBagConstraints5.gridx = 3;
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.insets = new Insets(6, 6, 17, 5);
            gridBagConstraints4.gridy = 1;
            gridBagConstraints4.ipadx = 2;
            gridBagConstraints4.ipady = -1;
            gridBagConstraints4.gridx = 2;
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.insets = new Insets(6, 6, 17, 5);
            gridBagConstraints3.gridy = 1;
            gridBagConstraints3.ipadx = 9;
            gridBagConstraints3.ipady = -1;
            gridBagConstraints3.gridx = 1;
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.insets = new Insets(6, 12, 17, 5);
            gridBagConstraints2.gridy = 1;
            gridBagConstraints2.ipadx = 3;
            gridBagConstraints2.ipady = -1;
            gridBagConstraints2.gridx = 0;
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.fill = GridBagConstraints.VERTICAL;
            gridBagConstraints1.gridwidth = 4;
            gridBagConstraints1.gridx = 1;
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.ipadx = 306;
            gridBagConstraints1.weightx = 1.0;
            gridBagConstraints1.insets = new Insets(12, 6, 5, 19);
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.insets = new Insets(16, 12, 10, 9);
            gridBagConstraints.gridy = 0;
            gridBagConstraints.ipadx = 8;
            gridBagConstraints.gridx = 0;
            setLabel = new JLabel();
            setLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            setLabel.setText(SET_LABEL);
            jContentPane = new JPanel();
            jContentPane.setLayout(new GridBagLayout());
            jContentPane.add(setLabel, gridBagConstraints);
            jContentPane.add(setComboBox, gridBagConstraints1);
            jContentPane.add(getDeleteButton(), gridBagConstraints2);
            jContentPane.add(getClearButton(), gridBagConstraints3);
            jContentPane.add(getRevertButton(), gridBagConstraints4);
            jContentPane.add(getCancelButton(), gridBagConstraints5);
            jContentPane.add(getSetButton(), gridBagConstraints6);
        }
        return jContentPane;
    }

    /**
     * This method initializes deleteButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getDeleteButton() {
        if (deleteButton == null) {
            deleteButton = new JButton();
            deleteButton.addActionListener(this);
            deleteButton.setText(DELETE_BUTTON);
        }
        return deleteButton;
    }

    /**
     * This method initializes clearButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getClearButton() {
        if (clearButton == null) {
            clearButton = new JButton();
            clearButton.addActionListener(this);
            clearButton.setText(CLEAR_BUTTON);
        }
        return clearButton;
    }

    /**
     * This method initializes revertButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getRevertButton() {
        if (revertButton == null) {
            revertButton = new JButton();
            revertButton.addActionListener(this);
            revertButton.setText(REVERT_BUTTON);
        }
        return revertButton;
    }

    /**
     * This method initializes cancelButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton();
            cancelButton.addActionListener(this);
            cancelButton.setText(CANCEL_BUTTON);
        }
        return cancelButton;
    }

    /**
     * This method initializes setButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getSetButton() {
        if (setButton == null) {
            setButton = new JButton();
            setButton.addActionListener(this);
            setButton.setText(SET_BUTTON);
        }
        return setButton;
    }

    public void actionPerformed(ActionEvent e) {
        final String cmd = e.getActionCommand();
        if (SET_BUTTON.equals(cmd)) {
            editor.stopCellEditing(setComboBox.getSelectedItem());
        } else if (DELETE_BUTTON.equals(cmd)) {
            editor.stopCellEditing(OperationFactory.getInstance(OperationFactory.DELETE, tag));
        } else if (REVERT_BUTTON.equals(cmd)) {
            editor.stopCellEditing(OperationFactory.getInstance(OperationFactory.KEEP, tag));
        } else if (CLEAR_BUTTON.equals(cmd)) {
            editor.stopCellEditing(OperationFactory.getInstance(OperationFactory.CLEAR, tag));
        } else if (CANCEL_BUTTON.equals(cmd)) {
            editor.cancelCellEditing();
        } else {
            throw new RuntimeException("Unimplemented action: " + cmd);
        }
        setVisible(false);  // in any case, we're done with this dialog.
    }
}  //  @jve:decl-index=0:visual-constraint="10,10"
