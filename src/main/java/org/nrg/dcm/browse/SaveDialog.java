/**
 * $Id: SaveDialog.java,v 1.2 2007/02/27 22:58:29 karchie Exp $
 * Copyright (c) 2006 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JComboBox;



/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
final class SaveDialog extends JDialog implements ActionListener {
  private static final long serialVersionUID = 1L;
  private static final String TITLE = "Save DICOM files";       // TODO: localize
  private static final String SAVE_BUTTON = "Save";             // TODO: localize
  private static final String CANCEL_BUTTON = "Cancel";         // TODO: localize
  private static final String BROWSE_BUTTON = "Browse...";      // TODO: localize
  
  private static final String DEFAULT_SUFFIX = "-anon";         // TODO: localize?  //  @jve:decl-index=0:
  
  private static final String ADJACENT_DIR_OPT = "Write files in adjacent directory with suffix:";
  private static final String ADJACENT_FILE_OPT = "Write files in same directory with suffix:";
  private static final String NEW_ROOT_OPT = "Write files under new root directory:";
  private static final String OVERWRITE_OPT = "Overwrite existing files";
  
  private static final String[] whereOpts = { ADJACENT_DIR_OPT, ADJACENT_FILE_OPT, NEW_ROOT_OPT, OVERWRITE_OPT };

  private static final String WRITE_ALL_FILES = "Write all loaded files";
  private static final String WRITE_SELECTED_FILES = "Write only selected files";
  
  private static final String[] whichOpts = { WRITE_ALL_FILES, WRITE_SELECTED_FILES };    // TODO: localize
  
  private static final int OUTER_INSET = 12;
  private static final int INNER_INSET = 6;
  
  private final FileSetTableModel model;
  private final JComboBox whereComboBox;
  private final JTextField argTextField;
  private final JComboBox whichComboBox;
  private final JButton browseButton;
  
  private String suffix = DEFAULT_SUFFIX;
  private enum TextContents { SUFFIX, ROOTPATH };
  private TextContents textFieldContents = TextContents.SUFFIX;
  private int lastWhereSelected = 0;
  
  /**
   * @param owner
   */
  public SaveDialog(DicomBrowser owner, FileSetTableModel model) {
    super(owner.getFrame());
    
    setTitle(TITLE);
    setLayout(new GridBagLayout());
    
    final GridBagConstraints gbc = new GridBagConstraints();
    
    // upper row
    whereComboBox = new JComboBox(whereOpts);
    gbc.gridx = gbc.gridy = 1;
    gbc.gridwidth = 2;
    gbc.insets = new Insets(OUTER_INSET,OUTER_INSET,INNER_INSET,INNER_INSET);
    whereComboBox.addActionListener(this);
    add(whereComboBox,gbc);
    
    browseButton = new JButton(BROWSE_BUTTON);
    browseButton.addActionListener(this);
    gbc.gridx = 3; gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(OUTER_INSET,INNER_INSET,INNER_INSET,OUTER_INSET);
    browseButton.addActionListener(this);
    add(browseButton,gbc);
    
    // middle row
    argTextField = new JTextField(DEFAULT_SUFFIX);
    gbc.gridx = 1; gbc.gridy = 2;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(INNER_INSET,OUTER_INSET,INNER_INSET,OUTER_INSET);
    add(argTextField,gbc);
    gbc.fill = GridBagConstraints.NONE;
    
    // lower row
    whichComboBox = new JComboBox(whichOpts);
    gbc.gridx = 1; gbc.gridy = 3;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(INNER_INSET,OUTER_INSET,OUTER_INSET,INNER_INSET);
    add(whichComboBox,gbc);
    
    final JButton cancelButton = new JButton(CANCEL_BUTTON);
    gbc.gridx = 2; gbc.gridy = 3;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(INNER_INSET,INNER_INSET,OUTER_INSET,INNER_INSET);
    cancelButton.addActionListener(this);
    add(cancelButton,gbc);
    
    final JButton saveButton = new JButton(SAVE_BUTTON);
    gbc.gridx = 3; gbc.gridy = 3;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(INNER_INSET,INNER_INSET,OUTER_INSET,OUTER_INSET);
    saveButton.addActionListener(this);
    add(saveButton,gbc);
    
    // initial state: where = adjacent dir, browse button disabled, text field enabled and editable
    whereComboBox.setSelectedIndex(0);
    browseButton.setEnabled(false);
    argTextField.setEnabled(true);
    argTextField.setEditable(true);
    
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);
    this.model = model;
  }


  private boolean setRootPath() {
    final JFileChooser chooser = new JFileChooser(DicomBrowser.prefs.get(DicomBrowser.OPEN_DIR_PREF, null));
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    final int r = chooser.showOpenDialog(this);
    if (r == JFileChooser.APPROVE_OPTION) {
      final File f = chooser.getSelectedFile();
      argTextField.setText(f.getPath());
      return true;
    } else {
      return false;
    }

  }
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == whereComboBox) {
      switch (whereComboBox.getSelectedIndex()) {
      case 0:   // adjacent directory
      case 1:   // adjacent file
        if (textFieldContents == TextContents.ROOTPATH)     // if text field had file path, put suffix back in.
          argTextField.setText(suffix);
        
        argTextField.setEditable(true);
        argTextField.setEnabled(true);
        browseButton.setEnabled(false);
        textFieldContents = TextContents.SUFFIX;
        lastWhereSelected = whereComboBox.getSelectedIndex();
        break;
        
      case 2:   // new root
        if (textFieldContents == TextContents.SUFFIX)
          suffix = argTextField.getText();
        
        argTextField.setEditable(false);
        argTextField.setEnabled(true);
        browseButton.setEnabled(true);
        if (setRootPath()) {
          textFieldContents = TextContents.ROOTPATH;
          lastWhereSelected = whereComboBox.getSelectedIndex();
        } else
          whereComboBox.setSelectedIndex(lastWhereSelected);
        
        break;
        
      case 3:   // overwrite
        argTextField.setEnabled(false);
        browseButton.setEnabled(false);
        lastWhereSelected = whereComboBox.getSelectedIndex();
        break;
        
      default:
        throw new RuntimeException("Unimplemented operation: " + whereComboBox.getSelectedItem());
      }
    } else {
      final String command = e.getActionCommand();
      if (command.equals(SAVE_BUTTON)) {
        setVisible(false);
        
        final boolean allFiles = whichComboBox.getSelectedIndex() == 0;
        final String arg = argTextField.getText();
        switch (whereComboBox.getSelectedIndex()) {
        case 0:         // adjacent directory
          model.saveInAdjacentDir("%s" + arg, allFiles);
          break;

        case 1:         // adjacent file
          model.saveInAdjacentFile("%s" + arg, allFiles);
          break;
          
        case 2:         // new root
          model.saveInNewRoot(arg, allFiles);
          break;

        case 3:         // overwrite
          model.overwrite(allFiles);
          break;
    
        default:
          throw new RuntimeException("Unimplemented operation: " + whereComboBox.getSelectedItem());
        }

      } else if (command.equals(CANCEL_BUTTON))
        setVisible(false);        // never mind
      else if (command.equals(BROWSE_BUTTON))
        setRootPath();
      else
        throw new RuntimeException("Unimplemented action: " + command);
    }
  }

  
}
