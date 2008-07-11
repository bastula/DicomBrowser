/**
 * $Id: AttrTableCellEditor.java,v 1.1 2006/12/22 21:10:46 karchie Exp $
 * Copyright (c) 2006 Washington University
 */
package org.nrg.dcm.browse;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.table.TableCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public final class AttrTableCellEditor extends AbstractCellEditor
  implements TableCellEditor,TreeSelectionListener,WindowListener {
  private final static long serialVersionUID = 1;
  private final Frame frame;
  private final JTable table;
  private final JTextField textField;
  private Object value;
  private AttributeEditDialog editDialog = null;
  private int row = -1, column = -1;
  
  AttrTableCellEditor(Frame frame, JTable table) {
    this.frame = frame;
    this.table = table;
    textField = new JTextField();
  }
  
  public Component getTableCellEditorComponent(final JTable table,
      final Object value, final boolean isSelected,
      final int row, final int column) {
    assert value instanceof MultiValueAttribute;
    assert table.equals(this.table);
    this.row = row;
    this.column = column;
    
    final MultiValueAttribute a = (MultiValueAttribute)value;
    if (a.size() == 1) { // we can edit a single value with a JTextField
      textField.setText(a.toString());
      return textField;
    } else {              // zero (deleted) or multiple values require a dialog
      editDialog = new AttributeEditDialog(frame, this, a);
      editDialog.addWindowListener(this);
      editDialog.setVisible(true);
      return null;      // need to do this by hand
    }
  }
  
  void setValue(final Object o) { value = o; }

  public Object getCellEditorValue() { return editDialog == null ? textField.getText() : value; }
  
  // Assign a value and finish the editing operation at the same time
  boolean stopCellEditing(final Object o) {
    value = o;
    return stopCellEditing();
  }
  
  private void endEditingState() {
    row = -1;
    column = -1;
    value = null;
    if (editDialog != null) {
      editDialog.removeWindowListener(this);
      editDialog.setVisible(false);
    }
    editDialog = null;
  }
  
  @Override
  public boolean stopCellEditing() {
    if (row >= 0 && column >= 0)
      table.setValueAt(value, row, column);
    endEditingState();
    return super.stopCellEditing();
  }
  
  @Override
  public void cancelCellEditing() {
    endEditingState();
    super.cancelCellEditing();
  }
  
  public void valueChanged(TreeSelectionEvent e) {
    // TODO: put up a dialog to confirm the selection change
    cancelCellEditing();
  }
  
  // We're a WindowListener so that if the edit dialog gets canceled,
  // we can properly abort editing that cell.
  public void windowActivated(WindowEvent e) {}
  public void windowClosed(WindowEvent e) {}
  public void windowClosing(WindowEvent e) { this.cancelCellEditing(); }
  public void windowDeactivated(WindowEvent e) {}
  public void windowDeiconified(WindowEvent e) {}
  public void windowIconified(WindowEvent e) {}
  public void windowOpened(WindowEvent e) {}
}
