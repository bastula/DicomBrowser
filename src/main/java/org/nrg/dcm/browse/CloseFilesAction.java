/**
 * $Id: CloseFilesAction.java,v 1.2 2007/08/22 17:50:35 karchie Exp $
 * Copyright (c) 2007 Washington University
 */
package org.nrg.dcm.browse;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedHashSet;

import javax.swing.AbstractAction;
import javax.swing.tree.TreePath;


/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
final class CloseFilesAction extends AbstractAction {
  private static final long serialVersionUID = 1L;

  private final FileSetTreeModel model;
  private final Collection<TreePath> selection;	// reference to somebody else's copy

  public CloseFilesAction(final FileSetTreeModel model, final String name, final Collection<TreePath> selection) {
    super(name);
    this.model = model;
    this.selection = selection;
    setEnabled(false);
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(final ActionEvent e) {
    model.remove(new LinkedHashSet<TreePath>(selection));
  }

}
