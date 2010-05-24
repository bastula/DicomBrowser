/**
 * Copyright (c) 2006-2010 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.UIManager;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import org.nrg.dcm.FileSet;

import org.nrg.dcm.edit.ScriptApplicator;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 */
public final class DicomBrowser extends JPanel
implements ActionListener,ComponentListener,ListSelectionListener,TreeSelectionListener {
    private static final long serialVersionUID = 1; 

    private static final String FILE_MENU = "File";
    private static final String OPEN_ITEM = "Open...";
    private static final String OPEN_NEW_WINDOW_ITEM = "Open in new window...";
    private static final String SEND_ITEM = "Send...";
    private static final String SAVE_ITEM = "Save...";
    private static final String CLOSE_WIN_ITEM = "Close window";
    private static final String UNDO_ITEM = "Undo";
    private static final String REDO_ITEM = "Redo";
    private static final String EDIT_MENU = "Edit";
    private static final String KEEP_ITEM = "Keep";
    private static final String CLEAR_ITEM = "Clear";
    private static final String DELETE_ITEM = "Delete";
    private static final String ADD_ITEM = "Add new attribute...";
    private static final String APPLY_SCRIPT_ITEM = "Apply anonymization script...";
    private static final String VIEW_MENU = "View";
    private static final String VIEW_ITEM = "View selected images";
    private static final String CLOSE_FILES_ITEM = "Close selected files";
    private static final String HELP_MENU = "Help";
    private static final String ABOUT_ITEM = "About DicomBrowser...";
    private static final String ABOUT_TITLE = "DicomBrowser";
    private static final String SELECT_FILES = "Select DICOM files";
    private static final String CHECKING_FILES = "Checking files...";
    private static final String OPENING_SCRIPT = "Opening anonymization script...";
    private static final String SCRIPT_FILTER_DESCRIPTION = "DICOM anonymization script (.das)";
    private static final String BAD_SCRIPT_TITLE = "Error in script";
    private static final String BAD_SCRIPT_MSG_FORMAT = "Error in script: %1$s";
    private static final String ALL_OR_SELECTED_FILES_QUESTION = "Apply script to selected files only or to all files?";
    private static final String ALL_FILES_OPTION = "All files";
    private static final String SELECTED_FILES_OPTION = "Selected files only";
    private static final String CANCEL_OPTION = "Cancel";
    private static final String APPLYING_SCRIPT = "Applying script...";
    private static final String FILE_LOAD_FAILED_TITLE = "File load failed";
    private static final String FILE_LOAD_FAILED_FORMAT = "Unable to read file: %1$s";
    private static final String TRUNCATE_FORMAT = "%1$s...(truncated)";

    private static final String SCRIPT_SUFFIX = ".das";

    private static final ResourceBundle rsrcb = new ListResourceBundle() {
	@Override
	public Object[][] getContents() { return contents; }
	private final Object[][] contents = {       // TODO: LOCALIZE THIS
		{FILE_MENU, FILE_MENU},
		{OPEN_ITEM, OPEN_ITEM},
		{OPEN_NEW_WINDOW_ITEM, OPEN_NEW_WINDOW_ITEM},
		{SEND_ITEM, SEND_ITEM},
		{SAVE_ITEM, SAVE_ITEM},
		{CLOSE_WIN_ITEM, CLOSE_WIN_ITEM},
		{EDIT_MENU, EDIT_MENU},
		{KEEP_ITEM, KEEP_ITEM},
		{CLEAR_ITEM, CLEAR_ITEM},
		{DELETE_ITEM, DELETE_ITEM},
		{ADD_ITEM, ADD_ITEM},
		{APPLY_SCRIPT_ITEM, APPLY_SCRIPT_ITEM},
		{UNDO_ITEM, UNDO_ITEM},
		{REDO_ITEM, REDO_ITEM},
		{VIEW_MENU, VIEW_MENU},
		{VIEW_ITEM, VIEW_ITEM},
		{CLOSE_FILES_ITEM, CLOSE_FILES_ITEM},
		{HELP_MENU, HELP_MENU},
		{ABOUT_ITEM, ABOUT_ITEM},
		{ABOUT_TITLE, ABOUT_TITLE},
		{SELECT_FILES, SELECT_FILES},
		{CHECKING_FILES, CHECKING_FILES},
		{OPENING_SCRIPT, OPENING_SCRIPT},
		{SCRIPT_FILTER_DESCRIPTION, SCRIPT_FILTER_DESCRIPTION},
		{BAD_SCRIPT_TITLE,BAD_SCRIPT_TITLE},
		{BAD_SCRIPT_MSG_FORMAT,BAD_SCRIPT_MSG_FORMAT},
		{ALL_OR_SELECTED_FILES_QUESTION,ALL_OR_SELECTED_FILES_QUESTION},
		{ALL_FILES_OPTION,ALL_FILES_OPTION},
		{SELECTED_FILES_OPTION,SELECTED_FILES_OPTION},
		{CANCEL_OPTION,CANCEL_OPTION},
		{APPLYING_SCRIPT,APPLYING_SCRIPT},
		{FILE_LOAD_FAILED_TITLE,FILE_LOAD_FAILED_TITLE},
		{FILE_LOAD_FAILED_FORMAT,FILE_LOAD_FAILED_FORMAT},
		{TRUNCATE_FORMAT, TRUNCATE_FORMAT},
	};
    };

    static final boolean isMacOS = System.getProperty("mrj.version") != null;

    public static final String OPEN_DIR_PREF = "choose.dir";
    public static final String SCRIPT_DIR_PREF = "script.dir";
    public static final String MAX_LEN_PREF = "value.maxlen";
    public static final String UID_ROOT_PREF = "uid.root";
    public static final String LAST_UID_FRAG_PREF = "list.uid.frag";
    public static final Preferences prefs = Preferences.userNodeForPackage(DicomBrowser.class);

    final class PopupListener extends MouseAdapter {
	private final JPopupMenu popup;
	PopupListener(final JPopupMenu popup) {
	    super();
	    this.popup = popup;
	}

	@Override
	public void mousePressed(MouseEvent e) { checkPopup(e); }

	@Override
	public void mouseReleased(MouseEvent e) { checkPopup(e); }

	private void checkPopup(MouseEvent e) {
	    if (e.isPopupTrigger()) {
		popup.show(e.getComponent(), e.getX(), e.getY());
	    }
	}
    }

    final class CommandAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
	private final String name;

	public CommandAction(final String name, final int mnemonic) {
	    super(name);
	    this.name = name;
	    putValue(MNEMONIC_KEY, mnemonic);
	}

	public void actionPerformed(ActionEvent e) {
	    add(tableModel.addOperations(name, table.getSelectedRows()));
	}
    }

    final class UndoAction extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public UndoAction() {
	    super(rsrcb.getString(UNDO_ITEM));
	    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	public void actionPerformed(ActionEvent e) {
	    final Command c = moveCommand(redoable, redoAction, rsrcb.getString(REDO_ITEM),
		    undoable, this, rsrcb.getString(UNDO_ITEM));
	    tableModel.undo(c);
	}
    }

    final class RedoAction extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public RedoAction() {
	    super(rsrcb.getString(REDO_ITEM));
	    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	public void actionPerformed(ActionEvent e) {
	    final Command c = moveCommand(undoable, undoAction, rsrcb.getString(UNDO_ITEM),
		    redoable, this, rsrcb.getString(REDO_ITEM));
	    tableModel.redo(c);
	}
    }

    final class AddAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
	public AddAction() {
	    super(rsrcb.getString(ADD_ITEM));
	    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	public void actionPerformed(ActionEvent e) {
	    new AttributeAddDialog(frame, tableModel).setVisible(true);
	}
    }

    private final Action keepAction, clearAction, deleteAction, addAction;
    private final Action undoAction, redoAction;
    private final ViewSlicesAction viewAction;
    private final CloseFilesAction closeFilesAction;

    private final Collection<Action> needsAttrSelection = new LinkedHashSet<Action>();
    private final Collection<Action> needsFileSelection = new LinkedHashSet<Action>();

    private JMenuItem saveItem, sendItem;

    private final Stack<Command> undoable = new Stack<Command>();
    private final Stack<Command> redoable = new Stack<Command>();

    /**
     * Returns the localized version of the indicated label
     * @param label
     * @return localized String
     */
    public static String getString(final String label) {
	return rsrcb.getString(label);
    }


    // maximum value length; 64 is the max length for UI, LO, PN, so
    // this covers most field types
    static private final int defaultMaxValueLen = 64;

    static private final int[] columnWidths = { 80, 160, 80, 380 };

    private final JFrame frame;
    private final JSplitPane splitPane;

    private final JTree tree;
    private final FileSetTreeModel treeModel;

    private final JTable table;
    private final FileSetTableModel tableModel;
    private final AttrTableCellEditor cellEditor;

    final StatusBar statusBar;

    public DicomBrowser(final JFrame frame, final FileSet fs) {
	super(new BorderLayout());
	this.frame = frame;

	treeModel = new FileSetTreeModel(frame, fs);
	tableModel = new FileSetTableModel(this, fs);

	tree = new JTree(treeModel);
	tree.setRootVisible(false);
	tree.setShowsRootHandles(true);
	tree.addTreeSelectionListener(this);
	tree.addTreeSelectionListener(tableModel);

	final JScrollPane treeView = new JScrollPane(tree);
	treeView.setMinimumSize(new Dimension(200,200));
	treeView.setPreferredSize(new Dimension(200,200));


	// Set up Actions: these can be invoked from the menu bar or from a popup.
	keepAction = new CommandAction(rsrcb.getString(KEEP_ITEM), KeyEvent.VK_K);
	keepAction.setEnabled(false);
	needsAttrSelection.add(keepAction);

	clearAction = new CommandAction(rsrcb.getString(CLEAR_ITEM), KeyEvent.VK_C);
	clearAction.setEnabled(false);
	needsAttrSelection.add(clearAction);

	deleteAction = new CommandAction(rsrcb.getString(DELETE_ITEM), KeyEvent.VK_D);
	deleteAction.setEnabled(false);
	needsAttrSelection.add(deleteAction);

	addAction = new AddAction();
	addAction.setEnabled(false);
	needsFileSelection.add(addAction);

	viewAction = new ViewSlicesAction(VIEW_ITEM, fileSelection);
	viewAction.setEnabled(false);
	needsFileSelection.add(viewAction);

	closeFilesAction = new CloseFilesAction(treeModel, CLOSE_FILES_ITEM, fileSelection);
	closeFilesAction.setEnabled(false);
	needsFileSelection.add(closeFilesAction);

	undoAction = new UndoAction();
	undoAction.setEnabled(false);

	redoAction = new RedoAction();
	redoAction.setEnabled(false);

	// Set up the tree popup menu
	final JPopupMenu treePopup = new JPopupMenu();
	treePopup.add(new JMenuItem(viewAction));
	treePopup.add(new JMenuItem(closeFilesAction));

	final MouseListener treePopupListener = new PopupListener(treePopup);
	tree.addMouseListener(treePopupListener);

	table = new JTable(tableModel) {
	    private static final long serialVersionUID = 1;
	    @Override
	    public boolean isCellEditable(int row, int col) {
		return col == FileSetTableModel.VALUE_COLUMN;      // only operationFactory is editable
	    }
	};

	table.getSelectionModel().addListSelectionListener(this);   // for menu enable/disable


	// Set up the table popup menu
	final JPopupMenu tablePopup = new JPopupMenu();
	tablePopup.add(new JMenuItem(undoAction));
	tablePopup.add(new JMenuItem(redoAction));
	tablePopup.addSeparator();
	tablePopup.add(new JMenuItem(keepAction));
	tablePopup.add(new JMenuItem(clearAction));
	tablePopup.add(new JMenuItem(deleteAction));
	tablePopup.add(new JMenuItem(addAction));

	final MouseListener tablePopupListener = new PopupListener(tablePopup);
	table.addMouseListener(tablePopupListener);


	// specialize editing of value column
	final TableColumn valueCol = table.getColumnModel().getColumn(FileSetTableModel.VALUE_COLUMN);
	cellEditor = new AttrTableCellEditor(frame, table);
	tree.addTreeSelectionListener(cellEditor);
	valueCol.setCellEditor(cellEditor);

	final JScrollPane tableView = new JScrollPane(table);
	table.setPreferredScrollableViewportSize(new Dimension(600,600));
	for (int i = 0; i < columnWidths.length; i++)
	    table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);

	splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	splitPane.setLeftComponent(treeView);
	splitPane.setRightComponent(tableView);
	add(splitPane, BorderLayout.CENTER);
	frame.addComponentListener(this);

	statusBar = new StatusBar();
	add(statusBar, BorderLayout.SOUTH);
    }


    public void componentHidden(final ComponentEvent e) {}
    public void componentMoved(final ComponentEvent e) {}
    public void componentShown(final ComponentEvent e) {}

    // The main frame has been resized; resize the contents accordingly.
    public void componentResized(final ComponentEvent e) {
	assert e.getComponent() == frame;
	final Dimension paned = frame.getContentPane().getSize();
	final int newHeight = paned.height-statusBar.getSize().height;
	splitPane.setSize(paned.width, newHeight);

	final Insets insets = splitPane.getInsets();
	final int borderHeight = insets.bottom + insets.top + 1;    // why 1? it works.
	for (int i = 0; i < splitPane.getComponentCount(); i++) {
	    final Component c = splitPane.getComponent(i);
	    final Dimension d = c.getSize();
	    d.height = newHeight - borderHeight;
	    c.setSize(d);
	}
    }

    public void actionPerformed(final ActionEvent e) {
	final String command = e.getActionCommand();
	if (command.equals(getString(OPEN_ITEM))) {
	    openFiles(this);
	} else if (command.equals(getString(OPEN_NEW_WINDOW_ITEM))) {
	    openFiles(null);
	} else if (command.equals(getString(SEND_ITEM))) {
	    cellEditor.stopCellEditing();
	    final JDialog sendDialog = new CStoreDialog(this, tableModel);
	    sendDialog.setVisible(true);
	} else if (command.equals(getString(SAVE_ITEM))) {
	    cellEditor.stopCellEditing();
	    final JDialog saveDialog = new SaveDialog(this, tableModel);
	    saveDialog.setVisible(true);
	} else if (command.equals(getString(CLOSE_WIN_ITEM))) {
	    closeBrowser();
	} else if (command.equals(getString(APPLY_SCRIPT_ITEM))) {
	    cellEditor.stopCellEditing();
	    applyScript();
	} else if (command.equals(getString(ABOUT_ITEM))) {
	    final JDialog aboutDialog = new AboutDialog(frame, getString(ABOUT_TITLE));
	    aboutDialog.setVisible(true);
	} else
	    throw new RuntimeException("Unimplemented operation: " + command);
    }


    public void doCommandOnSelection(final String name) {
	add(tableModel.addOperations(name, table.getSelectedRows()));
    }

    void clearTreeSelection() {
	tree.getSelectionModel().clearSelection();
    }


    /**
     * Add a command to the Undo queue.  This also clears the Redo queue
     * (since no commands have been issued in this state).  
     * @param c Command to be enqueued
     */
    void add(final Command c) {
	undoable.push(c);
	undoAction.putValue(Action.NAME, UNDO_ITEM + " " + c.toString());
	undoAction.setEnabled(true);

	redoable.clear();
	redoAction.putValue(Action.NAME, REDO_ITEM);
	redoAction.setEnabled(false);
    }


    /**
     * Moves a Command from one queue to another and makes appopriate menu changes.
     * @param to
     * @param toAction
     * @param toText
     * @param from
     * @param fromAction
     * @param fromText
     * @return The Command that was moved
     */
    private static Command moveCommand(final Stack<Command> to, final Action toAction, final String toText,
	    Stack<Command> from, final Action fromAction, final String fromText) {
	assert from.size() > 0;
	final Command c = from.pop();

	if (from.isEmpty()) {
	    fromAction.putValue(Action.NAME, fromText);
	    fromAction.setEnabled(false);
	} else {
	    fromAction.putValue(Action.NAME, fromText + " " + from.peek().toString());
	    assert fromAction.isEnabled();
	}

	to.push(c);

	toAction.putValue(Action.NAME, toText + " " + c.toString());
	toAction.setEnabled(true);

	return c;
    }


    /**
     * Some menu items are enabled only if some attributes are selected
     */
    public void valueChanged(ListSelectionEvent e) {
	final ListSelectionModel lsm = (ListSelectionModel)e.getSource();
	final boolean notEmpty = !lsm.isSelectionEmpty();
	for (final Action action : needsAttrSelection)
	    action.setEnabled(notEmpty);
    }


    /**
     * Some actions are enabled only if some files are selected
     */
    private final Collection<TreePath> fileSelection = new HashSet<TreePath>();
    public void valueChanged(final TreeSelectionEvent e) {
	final TreePath[] tps = e.getPaths();
	for (int i = 0; i < tps.length; i++)
	    if (e.isAddedPath(i)) {
		fileSelection.add(tps[i]);
	    } else {
		fileSelection.remove(tps[i]);
	    }

	final boolean someSelected = !fileSelection.isEmpty();
	for (final Action action : needsFileSelection) {
	    action.setEnabled(someSelected);
	}
    }


    private void add(final File[] files) {
	assert !SwingUtilities.isEventDispatchThread();
	treeModel.add(Arrays.asList(files));
	if (treeModel.getChildCount(treeModel.getRoot()) > 0) {
	    // now there are some files, so we can save or send them.
	    SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    sendItem.setEnabled(true);
		    saveItem.setEnabled(true);
		}
	    });
	}
    }


    /**
     * Allows the user to choose a script to apply to either
     * all files or just the selected files.
     *
     */
    private void applyScript() {
	final JFileChooser fc = new JFileChooser(prefs.get(SCRIPT_DIR_PREF, null));
	fc.addChoosableFileFilter(new FileFilter() {
	    @Override
	    public boolean accept(final File f) {
		return f.isDirectory() || f.getName().endsWith(SCRIPT_SUFFIX);
	    }
	    @Override
	    public String getDescription() {
		return SCRIPT_FILTER_DESCRIPTION;
	    }
	});
	fc.setDialogTitle(getString(OPENING_SCRIPT));
	fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

	if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
	    prefs.put(SCRIPT_DIR_PREF, fc.getCurrentDirectory().getPath());
	    final ScriptApplicator applicator;
	    try {
		final FileInputStream fin = new FileInputStream(fc.getSelectedFile());
		try {
		    applicator = new ScriptApplicator(fin);
		} finally {
		    fin.close();
		}
	    } catch (Throwable t) {
		JOptionPane.showMessageDialog(frame,
			String.format(getString(BAD_SCRIPT_MSG_FORMAT), t.getMessage()),
			getString(BAD_SCRIPT_TITLE),
			JOptionPane.ERROR_MESSAGE);
		return;
	    }

	    final String[] options = {
		    getString(ALL_FILES_OPTION), getString(SELECTED_FILES_OPTION), getString(CANCEL_OPTION) 
	    };
	    final int option = JOptionPane.showOptionDialog(frame,
		    getString(ALL_OR_SELECTED_FILES_QUESTION), getString(OPENING_SCRIPT), 
		    JOptionPane.YES_NO_CANCEL_OPTION,
		    JOptionPane.QUESTION_MESSAGE, null,
		    options, options[2]);

	    switch (option) {
	    case 0:
	    case 1:
		final ProgressMonitor pm = new ProgressMonitor(this, rsrcb.getString(APPLYING_SCRIPT),
			null, 0, 100);      // the thread will set the bounds.

		// Generate a Command, apply it, and add it to the undo list.
		final Runnable commandWorker = new Runnable() {
		    public void run() {
			final Command command =
			    tableModel.doScript(applicator.getStatements(), 1==option, pm);
			if (command != null) {
			    add(command);
			}
		    }
		};
		new Thread(commandWorker).start();
		break;

	    case 2:
	    default:
		return;
	    }
	}
    }


    /**
     * Handles the slow work of loading a FileSet, then calls createAndShowGUI when ready.
     * @author Kevin A. Archie <karchie@npg.wustl.edu>
     */
    private static final class FileSetReader implements Runnable {
	private final File[] files;
	private final DicomBrowser browser;
	FileSetReader(final DicomBrowser browser, final File[] files) {
	    this.files = files;
	    this.browser = browser;
	}
	public void run() {
	    if (browser == null) {
		final FileSet fs;
		try {
		    fs = new FileSet(files, true, SwingProgressMonitor.getMonitor(null, rsrcb.getString(CHECKING_FILES), "", 0, 100));
		    fs.setMaxValueLength(prefs.getInt(MAX_LEN_PREF, defaultMaxValueLen));
		    fs.setTruncateFormat(rsrcb.getString(TRUNCATE_FORMAT));
		} catch (IOException e) {
		    JOptionPane.showMessageDialog(null,
			    String.format(FILE_LOAD_FAILED_FORMAT, e.getMessage()),
			    FILE_LOAD_FAILED_TITLE,
			    JOptionPane.ERROR_MESSAGE);
		    return;
		} catch (SQLException e) {
		    JOptionPane.showMessageDialog(null,
			    String.format(FILE_LOAD_FAILED_FORMAT, e.getMessage()),
			    FILE_LOAD_FAILED_TITLE,
			    JOptionPane.ERROR_MESSAGE);
		    return;

		}
		SwingUtilities.invokeLater(new Runnable() {
		    public void run() { createAndShowGUI(fs); }
		});
	    } else {
		browser.add(files);
	    }
	}
    }

    /**
     * From the indicated window, starts opening the named files into a new window.
     * @param frame JFrame where the open command was issues (may be null)
     * @param files List of files to be opened (if null or zero size, opens a Chooser)
     */
    private static void openFiles(final DicomBrowser browser, final File...files) {
	// need to be in Swing thread to use chooseFiles
	if (files == null || files.length == 0) {
	    SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    final JFrame frame = browser == null ? null : browser.frame;
		    final File[] chosen = chooseFiles(frame);
		    if (chosen != null && chosen.length > 0)
			new Thread(new FileSetReader(browser, chosen)).start();
		}
	    });
	} else {
	    new Thread(new FileSetReader(browser, files)).start();
	}
    }

    public JFrame getFrame() { return frame; }

    private void closeBrowser() {
	frame.dispose();
	tableModel.dispose();
    }

    /**
     * Create a Frame to show one patient/dataset worth of data
     * This must be executed in the Swing thread
     */
    private static void createAndShowGUI(final FileSet fs) {
	final JFrame frame = new JFrame("DICOM browser");
	InterfaceCounter.getInstance().register(frame);
	frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

	final DicomBrowser browser = new DicomBrowser(frame, fs);
	browser.setOpaque(true);
	frame.setContentPane(browser);

	final JMenuBar menuBar = new JMenuBar();
	frame.setJMenuBar(menuBar);
	final JMenu fileMenu = new JMenu(rsrcb.getString(FILE_MENU));
	fileMenu.setMnemonic(KeyEvent.VK_F);
	menuBar.add(fileMenu);

	final JMenuItem openItem = new JMenuItem(rsrcb.getString(OPEN_ITEM));
	openItem.setMnemonic(KeyEvent.VK_O);
	openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	openItem.addActionListener(browser);
	fileMenu.add(openItem);

	final JMenuItem openNewWinItem = new JMenuItem(rsrcb.getString(OPEN_NEW_WINDOW_ITEM));
	openNewWinItem.setMnemonic(KeyEvent.VK_N);
	openNewWinItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	openNewWinItem.addActionListener(browser);
	fileMenu.add(openNewWinItem);

	fileMenu.add(new JMenuItem(browser.closeFilesAction));

	fileMenu.addSeparator();

	browser.sendItem = new JMenuItem(rsrcb.getString(SEND_ITEM));
	browser.sendItem.setMnemonic(KeyEvent.VK_E);
	browser.sendItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	browser.sendItem.addActionListener(browser);
	fileMenu.add(browser.sendItem);

	browser.saveItem = new JMenuItem(rsrcb.getString(SAVE_ITEM));
	browser.saveItem.setMnemonic(KeyEvent.VK_S);
	browser.saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	browser.saveItem.addActionListener(browser);
	fileMenu.add(browser.saveItem);

	try {
	    if (0 == fs.size()) {
		browser.sendItem.setEnabled(false);
		browser.saveItem.setEnabled(false);
	    }
	} catch (SQLException e) {
	    browser.sendItem.setEnabled(false);
	    browser.saveItem.setEnabled(false);
	}

	fileMenu.addSeparator();

	final JMenuItem closeItem = new JMenuItem(rsrcb.getString(CLOSE_WIN_ITEM));
	closeItem.setMnemonic(KeyEvent.VK_C);
	closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	closeItem.addActionListener(browser);
	fileMenu.add(closeItem);

	final JMenu editMenu = new JMenu(rsrcb.getString(EDIT_MENU));
	editMenu.setMnemonic(KeyEvent.VK_E);
	menuBar.add(editMenu);

	editMenu.add(new JMenuItem(browser.undoAction));
	editMenu.add(new JMenuItem(browser.redoAction));
	editMenu.addSeparator();
	editMenu.add(new JMenuItem(browser.keepAction));
	editMenu.add(new JMenuItem(browser.clearAction));
	editMenu.add(new JMenuItem(browser.deleteAction));
	editMenu.add(new JMenuItem(browser.addAction));
	editMenu.addSeparator();

	final JMenuItem applyScriptItem = new JMenuItem(rsrcb.getString(APPLY_SCRIPT_ITEM));
	applyScriptItem.setMnemonic(KeyEvent.VK_A);
	applyScriptItem.addActionListener(browser);
	editMenu.add(applyScriptItem);

	final JMenu viewMenu = new JMenu(rsrcb.getString(VIEW_MENU));
	menuBar.add(viewMenu);
	viewMenu.add(new JMenuItem(browser.viewAction));

	if (isMacOS) {
	    new MacOSAppEventHandler(browser.frame, ABOUT_TITLE);
	} else {
	    final JMenu helpMenu = new JMenu(rsrcb.getString(HELP_MENU));
	    helpMenu.setMnemonic(KeyEvent.VK_H);
	    menuBar.add(helpMenu);

	    final JMenuItem aboutItem = new JMenuItem(ABOUT_ITEM);
	    aboutItem.setMnemonic(KeyEvent.VK_A);
	    aboutItem.addActionListener(browser);
	    helpMenu.add(aboutItem);
	}

	frame.pack();
	frame.setVisible(true);
    }

    /**
     * Open a Chooser dialog so the user can select files.
     * This must be executed in the Swing thread
     */
    private static File[] chooseFiles(Component parent) {
	final JFileChooser fc = new JFileChooser(prefs.get(OPEN_DIR_PREF, null));
	fc.setDialogTitle(SELECT_FILES);
	fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
	fc.setMultiSelectionEnabled(true);
	if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
	    prefs.put(OPEN_DIR_PREF, fc.getCurrentDirectory().getPath());
	    return fc.getSelectedFiles();
	} else
	    return null;
    }


    public static void main(final String[] args) {
	// if DicomBrowser.value.maxlen is set, assign the maximum value length preference
	final String maxValueLen = System.getProperty("DicomBrowser." + MAX_LEN_PREF);
	if (null != maxValueLen) {
	    prefs.putInt(MAX_LEN_PREF, Integer.parseInt(maxValueLen));
	}

	SwingUtilities.invokeLater(new Runnable() {
	    public void run() { 
		try {
		    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {} // no big deal, we'll use default instead.
	    }
	});

	final File[] files = new File[args.length];
	for (int i = 0; i < args.length; i++) {
	    files[i] = new File(args[i]);
	}

	new FileSetReader(null, files).run();
    }
}