/**
 * $Id: AttributeAddDialog.java,v 1.1 2007/08/22 18:19:10 karchie Exp $
 * Copyright (c) 2007 Washington University
 */
package org.nrg.dcm.browse;

import javax.swing.JPanel;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.util.TagUtils;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
final class AttributeAddDialog extends JDialog implements ActionListener,DocumentListener {
  private static final long serialVersionUID = 1L;
  private final static String TITLE = "Add new attribute";	// TODO: localize  //  @jve:decl-index=0:
  private final static String CANCEL_BUTTON = "Cancel";		// TODO: localize
  private final static String ADD_BUTTON = "Add";		// TODO: localize
  private final static String UNKNOWN = "Unknown attribute";	// TODO: localize
  
  private final static DicomObject dcmo = new BasicDicomObject();	// used for name translation  //  @jve:decl-index=0:

  private JPanel jContentPane = null;

  private JPanel tagPanel = null;

  private JLabel tagLabel = null;
  private JLabel tagOPLabel = null;
  private JTextField tagHighField = null;
  private JLabel tagCommaLabel = null;
  private JTextField tagLowField = null;
  private JLabel tagCPLabel = null;

  private JPanel valuePanel = null;

  private JLabel valueLabel = null;
  private JTextField valueField = null;

  private JPanel actionPanel = null;

  private JButton cancelButton = null;
  private JButton addButton = null;

  private final FileSetTableModel table;
  private JLabel attrDescLabel = null;
  
  
  public void changedUpdate(final DocumentEvent e) {
    attrDescLabel.setText("");
    try {
      final int tag = parseTag();
      String desc = TagUtils.toString(tag) + ": " + dcmo.nameOf(tag);
      if (null == desc || "?".equals(desc))
	desc = UNKNOWN;
      attrDescLabel.setText(desc);
      addButton.setEnabled(true);
    } catch (NumberFormatException ex) {
      addButton.setEnabled(false);
    }
  }

  public void insertUpdate(final DocumentEvent e) { changedUpdate(e); }
  public void removeUpdate(final DocumentEvent e) { changedUpdate(e); }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(final ActionEvent e) {
    final String cmd = e.getActionCommand();
    if (CANCEL_BUTTON.equals(cmd)) {
      // drop this operation and move on
    } else if (ADD_BUTTON.equals(cmd) || e.getSource().equals(valueField)) {
      try {
	table.doOperation(OperationFactory.getInstance(OperationFactory.ASSIGN, parseTag(), valueField.getText()));
      } catch (NumberFormatException ex) {
	JOptionPane.showMessageDialog(this,
	    String.format("(%s,$s) is not a valid DICOM tag",
		tagHighField.getText(), tagLowField.getText()));
	return;
      }
    } else {
      throw new RuntimeException("Unimplemented action: " + cmd);
    }
    setVisible(false);	// Either button means we're done with this dialog.
  }

  private final int parseTag() {
    return (Integer.parseInt(tagHighField.getText(),16) << 16) + Integer.parseInt(tagLowField.getText(), 16);
  }
  
  /**
   * @param owner
   */
  public AttributeAddDialog(final Frame owner, final FileSetTableModel table) {
    super(owner, TITLE, true);
    this.setLocationRelativeTo(owner);
    initialize();
    this.table = table;
  }

  /**
   * This method initializes this
   * 
   * @return void
   */
  private void initialize() {
    this.setSize(260, 168);
    this.setContentPane(getJContentPane());
  }

  /**
   * This method initializes jContentPane
   * 
   * @return javax.swing.JPanel
   */
  private JPanel getJContentPane() {
    if (jContentPane == null) {
      jContentPane = new JPanel(new GridBagLayout());
     
      final GridBagConstraints tagPC = new GridBagConstraints();
      tagPC.gridy = 0;
      jContentPane.add(getTagPanel(), tagPC);

      attrDescLabel = new JLabel("");
      attrDescLabel.setPreferredSize(new Dimension(240, 20));
      final GridBagConstraints descLC = new GridBagConstraints();
      descLC.gridy = 1;
      jContentPane.add(attrDescLabel, descLC);

      final GridBagConstraints valuePC = new GridBagConstraints();
      valuePC.gridy = 2;
      jContentPane.add(getValuePanel(), valuePC);
      
      final GridBagConstraints actionPC = new GridBagConstraints();
      actionPC.gridy = 3;
      actionPC.anchor = GridBagConstraints.LINE_END;
      jContentPane.add(getActionPanel(), actionPC);
    }
    return jContentPane;
  }

  /**
   * This method initializes tagPanel	
   * 	
   * @return javax.swing.JPanel	
   */
  private JPanel getTagPanel() {
    if (tagPanel == null) {
      tagPanel = new JPanel();

      tagLabel = new JLabel("New attribute tag:");
      final GridBagConstraints tagLC = new GridBagConstraints();
      tagLC.insets = new Insets(0, 0, 0, 5);
      tagLC.gridx = 0;
      tagPanel.add(tagLabel);
      
      tagOPLabel = new JLabel("(");
      final GridBagConstraints tagOPLC = new GridBagConstraints();
      tagOPLC.gridx = 1;
      tagPanel.add(tagOPLabel);
      
      final GridBagConstraints tagHFC = new GridBagConstraints();
      tagHFC.gridx = 2;
      tagPanel.add(getTagHighField());
      
      tagCommaLabel = new JLabel(",");
      final GridBagConstraints tagCommaC = new GridBagConstraints();
      tagCommaC.gridx = 3;
      tagPanel.add(tagCommaLabel);
           
      final GridBagConstraints tagLFC = new GridBagConstraints();
      tagLFC.gridx = 4;
      tagPanel.add(getTagLowField());

      tagCPLabel = new JLabel(")");
      final GridBagConstraints tagCPLC = new GridBagConstraints();
      tagCPLC.gridx = 5;
      tagPanel.add(tagCPLabel);
     }
    return tagPanel;
  }

  /**
   * This method initializes valuePanel	
   * 	
   * @return javax.swing.JPanel	
   */
  private JPanel getValuePanel() {
    if (valuePanel == null) {
      valueLabel = new JLabel();
      valueLabel.setText("Value:");
      valuePanel = new JPanel();
      valuePanel.add(valueLabel);
      valuePanel.add(getValueField());
    }
    return valuePanel;
  }

  /**
   * This method initializes tagHighField	
   * 	
   * @return javax.swing.JTextField	
   */
  private JTextField getTagHighField() {
    if (tagHighField == null) {
      tagHighField = new JTextField();
      tagHighField.setPreferredSize(new Dimension(36, 20));
      tagHighField.getDocument().addDocumentListener(this);
    }
    return tagHighField;
  }

  /**
   * This method initializes tagLowField	
   * 	
   * @return javax.swing.JTextField	
   */
  private JTextField getTagLowField() {
    if (tagLowField == null) {
      tagLowField = new JTextField();
      tagLowField.setPreferredSize(new Dimension(36, 20));
      tagLowField.getDocument().addDocumentListener(this);
    }
    return tagLowField;
  }

  /**
   * This method initializes actionPanel	
   * 	
   * @return javax.swing.JPanel	
   */
  private JPanel getActionPanel() {
    if (actionPanel == null) {
      actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      actionPanel.add(getCancelButton(), null);
      actionPanel.add(getAddButton(), null);
    }
    return actionPanel;
  }

  /**
   * This method initializes valueField	
   * 	
   * @return javax.swing.JTextField	
   */
  private JTextField getValueField() {
    if (valueField == null) {
      valueField = new JTextField();
      valueField.setPreferredSize(new Dimension(180, 20));
      valueField.addActionListener(this);	// VK_ENTER in this field treated same as clicking "Add"
    }
    return valueField;
  }

  /**
   * This method initializes cancelButton	
   * 	
   * @return javax.swing.JButton	
   */
  private JButton getCancelButton() {
    if (cancelButton == null) {
      cancelButton = new JButton();
      cancelButton.setText(CANCEL_BUTTON);
      cancelButton.setToolTipText("Don't add this attribute");
      cancelButton.addActionListener(this);
    }
    return cancelButton;
  }

  /**
   * This method initializes addButton	
   * 	
   * @return javax.swing.JButton	
   */
  private JButton getAddButton() {
    if (addButton == null) {
      addButton = new JButton();
      addButton.setEnabled(false);
      addButton.setText(ADD_BUTTON);
      addButton.addActionListener(this);
    }
    return addButton;
  }

}  //  @jve:decl-index=0:visual-constraint="20,45"
