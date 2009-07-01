/**
 * Copyright (c) 2006-2009 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.io.IOException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.ResourceBundle;
import java.util.ListResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import org.apache.log4j.Logger;

import org.dcm4che2.data.Tag;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.util.TagUtils;

import org.nrg.attr.ConversionFailureException;
import org.nrg.dcm.FileSet;
import org.nrg.dcm.ProgressMonitorI;
import org.nrg.dcm.DirectoryRecord;
import org.nrg.dcm.edit.Assignment;
import org.nrg.dcm.edit.Constraint;
import org.nrg.dcm.edit.Deletion;
import org.nrg.dcm.edit.Operation;
import org.nrg.dcm.edit.Statement;
import org.nrg.dcm.edit.StatementArrayList;
import org.nrg.dcm.edit.StatementList;
import org.nrg.dcm.io.AdjacentDirFileExporter;
import org.nrg.dcm.io.AdjacentFileExporter;
import org.nrg.dcm.io.BatchExporter;
import org.nrg.dcm.io.CStoreExporter;
import org.nrg.dcm.io.DicomObjectExporter;
import org.nrg.dcm.io.NewRootFileExporter;
import org.nrg.dcm.io.OverwriteFileExporter;
import org.nrg.util.EditProgressMonitor;


/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public class FileSetTableModel extends AbstractTableModel
implements TreeSelectionListener {
    private static final long serialVersionUID = 1;
    private static final String AE_TITLE = "DicomBrowser";

    private final Logger logger = Logger.getLogger(FileSetTableModel.class);

    private static String colNames[] = {"tag", "name", "action", "value"};
    private static final String SENDING_FILES = "Sending files...";
    private static final String WRITING_FILES = "Writing files...";
    private static final String READING_VALS_FORMAT = "Reading values...%1$s";
    private static final String OUT_OF_MEMORY_TITLE = "Out of memory";
    private static final String OUT_OF_MEMORY_MESSAGE =
	"DicomBrowser doesn't have enough memory to do that.\n" +
	"You should probably exit the program, as it may behave\n" +
	"unpredictably.\n" +
	"To avoid running out of memory, don't select so many\n" +
	"files at once.  You could also increase the Java memory\n" +
	"limit, e.g., by running DicomBrowser with the flag\n" +
	"-Xmx128M (or -Xmx512M, or more).";

    private static final ResourceBundle rsrcb = new ListResourceBundle() {
	@Override
	public Object[][] getContents() { return contents; }
	private final Object[][] contents = {       // TODO: LOCALIZE THIS
		{"tag", "Tag"},
		{"name", "Name"},
		{"action", "Action"},
		{"value", "Value"},
		{"preparing-files", "Preparing files..."},
		{"sending-files", "Sending files..."},
		{"reading-values", "Reading values..."},
		{READING_VALS_FORMAT, READING_VALS_FORMAT},
		{"done", ""},
		{SENDING_FILES, SENDING_FILES},
		{WRITING_FILES, WRITING_FILES},
		{"deleted", "(deleted)"},
		{OUT_OF_MEMORY_TITLE, OUT_OF_MEMORY_TITLE},
		{OUT_OF_MEMORY_MESSAGE, OUT_OF_MEMORY_MESSAGE},
	};
    };
    private static final int MAXTAG = Tag.IconImageSequence - 1;
    private static final long CONTENTS_SHOW_INTERVAL = 1000;
    private static final long CONTENTS_UPDATE_INTERVAL = 4000;       // ms, heuristic

    // TODO: remove this
    // name of directory where files that couldn't be matched to a root are saved
    //    private static final String LOST_AND_FOUND_DIR = "lost-and-found";

    private static final int nCols = 4;
    public static final int ACTION_COLUMN = 2;
    public static final int VALUE_COLUMN = 3;

    static final int MAX_VALUES_SHOWN = 4;
    static final String MORE_VALUES = "...";

    private final Executor executor = Executors.newCachedThreadPool();
    private final FileSet fs;
    private final Set<TreePath> fileSelection = new HashSet<TreePath>();
    private final Set<File> selectedFiles = new HashSet<File>();
    private final Map<Integer,Map<File,Operation>> allOps = new HashMap<Integer,Map<File,Operation>>();
    private final DicomBrowser browser;

    private StatusBar.TaskMonitor cachingProgress = null;

    private MultiValueAttribute[] contents = new MultiValueAttribute[0];

    private class CacheBuilder implements Runnable {
	private final boolean wipe;
	public CacheBuilder(final boolean shouldWipeTableFirst) {
	    super();
	    wipe = shouldWipeTableFirst;
	}

	public void run() {
	    cacheValues(wipe);
	}
    };


    public FileSetTableModel(final DicomBrowser browser, final FileSet fs) {
	super();
	this.fs = fs;
	this.browser = browser;
    }


    public void dispose() {
	allOps.clear();
	fs.dispose();
    }

    private void collectReferencedFiles(final DirectoryRecord root, final Set<File> files) {
	final Queue<DirectoryRecord> records = new LinkedList<DirectoryRecord>();
	records.add(root);
	while (records.peek() != null) {
	    final DirectoryRecord dr = records.poll();
	    final String rfpath = dr.getValue(Tag.ReferencedFileID);
	    if (rfpath != null)
		files.add(new File(rfpath));
	    records.addAll(dr.getLower());
	}
    }

    private void updateValues(final Map<Integer,Set<String>> vals, final Collection<Integer> modified) {
	assert vals.keySet().containsAll(modified);
	final LinkedList<MultiValueAttribute> newContents = new LinkedList<MultiValueAttribute>();
	for (final Map.Entry<Integer,Set<String>> e : vals.entrySet()) {
	    final int tag = e.getKey();
	    newContents.add(new MultiValueAttribute(tag, modified.contains(tag), e.getValue().toArray(new String[0])));
	}
	contents = newContents.toArray(new MultiValueAttribute[0]);
	fireTableDataChanged();
    }

    /**
     * Based on the current fileSelection, updates the list of selected files,
     * the attribute values for display, and the selected actions
     * @param wipeTableFirst true if the existing table contents should be wiped
     *                            immediately rather than waiting for the first
     *                            update.  This isn't always obvious in theory,
     *                            so it mostly reflects playing around and seeing
     *                            where an immediate wipe seemed appropriate.
     */
    private void cacheValues(boolean wipeTableFirst) {
	if (cachingProgress != null)
	    cachingProgress.cancel();

	final List<File> localSelectedFiles;
	synchronized(selectedFiles) {
	    selectedFiles.clear();
	    for (final TreePath tp : fileSelection) {
		final DirectoryRecord dr = (DirectoryRecord)tp.getLastPathComponent();
		collectReferencedFiles(dr, selectedFiles);
	    }
	    localSelectedFiles = new LinkedList<File>(selectedFiles);
	}

	final Map<Integer,Set<String>> vals = new TreeMap<Integer,Set<String>>();
	final Collection<Integer> modified = new HashSet<Integer>();

	if (wipeTableFirst)
	    updateValues(vals, modified);
	long lastUpdate = new Date().getTime() - CONTENTS_UPDATE_INTERVAL + CONTENTS_SHOW_INTERVAL;

	cachingProgress = browser.statusBar.getTaskMonitor(0, localSelectedFiles.size(),
		rsrcb.getString("reading-values"), rsrcb.getString("done"));

	final ProgressMonitorI localTaskMonitor = cachingProgress;

	int progress = 0;
	for (final File file : localSelectedFiles) {
	    cachingProgress.setNote(String.format(rsrcb.getString(READING_VALS_FORMAT), file.getName()));
	    final Map<Integer,String> fv;
	    try {
		fv = fs.getValuesFromFile(file, 0, MAXTAG);
	    } catch (ConversionFailureException e) {
		e.printStackTrace();
		logger.error("error caching values", e);
		continue;     // move on to next file
	    } catch (IOException e) {
		e.printStackTrace();
		logger.error("error caching values", e);
		continue;     // move on to next file
	    } catch (SQLException e) {
		e.printStackTrace();
		logger.error("error cachine values", e);
		continue;  // move on to next file
	    } catch (OutOfMemoryError e) {
		JOptionPane.showMessageDialog(browser, rsrcb.getString(OUT_OF_MEMORY_MESSAGE),
			rsrcb.getString(OUT_OF_MEMORY_TITLE), JOptionPane.ERROR_MESSAGE);
		break;
	    }
	    finally {
		if (localTaskMonitor.isCanceled()) {
		    fireTableDataChanged();
		    localTaskMonitor.close();
		    if (localTaskMonitor == cachingProgress)      // TODO: needs synchronization?
			cachingProgress = null;
		    return;     // only way to cancel this is to make a new tree selection
		}
		cachingProgress.setProgress(++progress);
	    }

	    // Build the values for the fileSelection with operations applied
	    // Some operations may add new attributes, so the complete set of tags
	    // comes from both the files and the operations.
	    final Collection<Integer> tags = new TreeSet<Integer>(fv.keySet());
	    tags.addAll(allOps.keySet());
	    for (final int tag : tags) {
		if (!vals.containsKey(tag)) {
		    vals.put(tag, new HashSet<String>());
		}
		if (allOps.containsKey(tag) && allOps.get(tag).containsKey(file)) {
		    modified.add(tag);
		    final String val = allOps.get(tag).get(file).apply(fv);
		    vals.get(tag).add(null == val ? rsrcb.getString("deleted") : val);
		} else {
		    vals.get(tag).add(fv.get(tag));
		}
	    }

	    if (new Date().getTime() - lastUpdate > CONTENTS_UPDATE_INTERVAL) {
		updateValues(vals, modified);
		lastUpdate = new Date().getTime();
	    }
	}

	updateValues(vals, modified);

	cachingProgress.close();
	cachingProgress = null;
    }

    /* (non-Javadoc)
     * @see javax.swing.event.TreeSelectionListener#valueChanged()
     */
    public void valueChanged(TreeSelectionEvent e) {
	final TreePath[] tps = e.getPaths();
	boolean needsWipe = false;
	for (int i = 0; i < tps.length; i++)
	    if (e.isAddedPath(i))
		fileSelection.add(tps[i]);
	    else {
		needsWipe = true;       // some of the attributes now displayed may be gone
		fileSelection.remove(tps[i]);
	    }

	// Change the table contents to reflect the new fileSelection.
	// This is a little time-consuming, so we do it in a separate thread.
	executor.execute(new CacheBuilder(needsWipe));
    }


    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() { return nCols; }

    @Override
    public String getColumnName(final int col) {
	return rsrcb.getString(colNames[col]);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount() {
	return contents.length;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
	assert 0 <= rowIndex;

	synchronized(contents) {
	    // These weird conditions can happen if contents update between
	    // the getRowCount() and the getValueAt(); don't sweat it.
	    if (rowIndex >= contents.length || contents[rowIndex] == null)
		return null;

	    switch (columnIndex) {
	    case 0: return contents[rowIndex].getTagString();
	    case 1: return contents[rowIndex].getNameString();
	    case ACTION_COLUMN: {
		final int tag = contents[rowIndex].getTag();
		final Set<Operation> ops = new HashSet<Operation>();
		synchronized(selectedFiles) {
		    for (File file : selectedFiles) {
			if (allOps.containsKey(tag)) {
			    ops.add(allOps.get(tag).get(file));  // null here = KEEP
			} else {
			    ops.add(null);       // implicit KEEP
			}
			if (ops.size() > 1)
			    return OperationFactory.getMultipleName();
		    }
		}
		assert ops.size() == 1;
		for (Operation op : ops) {
		    return (op == null) ? OperationFactory.getDefaultName() : op.getName();
		}
		assert false;
		return null;
	    }
	    case VALUE_COLUMN: return contents[rowIndex];
	    default: assert false : "column index out of range"; return null;
	    }
	}
    }


    @Override
    public void setValueAt(final Object o, final int row, final int col) {
	assert VALUE_COLUMN == col;
	if (o == null) return;      // this can happen when an editing operation is canceled
	if (o instanceof Operation) {
	    doOperation((Operation)o);
	} else if (o instanceof String) {
	    // If this attribute has a single value over the fileSelection,
	    // and it hasn't changed, return without doing anything.
	    if (contents[row].size() == 1 && contents[row].toString().equals(o)) {
		return;
	    }

	    final String val = (String)o;
	    doOperation(OperationFactory.getInstance(OperationFactory.ASSIGN,
		    contents[row].getTag(), val));

	    // if we're in the middle of building the values table,
	    // we'll need to start over.
	    if (cachingProgress != null) {
		executor.execute(new CacheBuilder(false));
	    }

	} else if (o instanceof MultiValueAttribute) { // only on canceled edit
	    if (!contents[row].equals(o)) {
		throw new IllegalArgumentException("setValueAt() given unknown attribute: " + o);
	    }
	} else {
	    throw new IllegalArgumentException("setValueAt() given unknown object type: " + o.getClass().getName());
	}
	fireTableRowsUpdated(row, row);     // needed to update Action
    }


    /**
     * Perform the indicated operations, update the table entries,
     * and add a single corresponding Command to the browser's Undo stack.
     * @param op
     */
    public synchronized void doOperation(final Operation op) {
	// The following loop has the effect of clearing any previous
	// operations on the selected files, so any replaced operations
	// should be saved to put into the undo stack
	final Map<File,Operation> replacedOps = addOperation(op, selectedFiles.toArray(new File[0]));
	browser.add(new Command(op, selectedFiles, replacedOps));

	// Assign the modified row contents
	final int tag = op.getTag();

	boolean applied = false;
	for (int row = 0; row < contents.length; row++) {
	    if (tag == contents[row].getTag()) {
		if (op instanceof Assignment) {
		    // Our assignments are simple string assignments that don't depend
		    // on other tags, so we can get the new value directly.
		    contents[row] = new MultiValueAttribute(tag, true, ((Assignment)op).apply(null));
		} else if (op instanceof Deletion) {
		    // Attribute deletion is even simpler.
		    contents[row] = new MultiValueAttribute(tag, true);
		} else {
		    // Other operations are more complicated and require values rebuild.
		    // This is a little time-consuming, so we do it in a separate thread.
		    executor.execute(new CacheBuilder(false));
		}
		applied = true;
		fireTableRowsUpdated(row, row);
	    }
	}

	if (false == applied) {
	    // The only way an operation can be applied to a nonexistent row
	    // is if we're creating a new row.  Insert the new one in its correct
	    // place (in increasing tag order).
	    assert op instanceof Assignment;
	    final LinkedList<MultiValueAttribute> tcontents = new LinkedList<MultiValueAttribute>(Arrays.asList(contents));
	    final MultiValueAttribute value = new MultiValueAttribute(tag, true, ((Assignment)op).apply(null));
	    int newRow = -1;
	    for (final ListIterator<MultiValueAttribute> i = tcontents.listIterator(); i.hasNext();) {
		final int tt = i.next().getTag();
		if (tag < tt) {
		    i.previous();
		    i.add(value);
		    newRow = i.previousIndex();
		    break;
		}
	    }
	    if (newRow < 0) {
		tcontents.add(value);
		newRow = contents.length;
	    }
	    contents = tcontents.toArray(new MultiValueAttribute[0]);
	    fireTableRowsInserted(newRow,newRow);
	}
    }


    /**
     * Builds a record of replaced operations.
     * The caller is responsible for updating the table.
     * @param ops Operations to be applied
     * @return Map of replaced Operations and the File to which they applied.
     */
    private Map<File,Operation> addOperation(final Operation op, final File...files) {
	final int tag = op.getTag();
	final Map<File,Operation> replaced = new HashMap<File,Operation>();
	for (final File file : files) {
	    if (!allOps.containsKey(tag)) {
		allOps.put(tag, new HashMap<File,Operation>());
	    }
	    if (allOps.get(tag).containsKey(file)) {
		replaced.put(file, allOps.get(tag).get(file));
	    }
	    allOps.get(tag).put(file, op);
	}
	return replaced;
    }


    public Command addOperations(final String name, final int...rows) {
	if (rows.length == 0)
	    throw new IllegalArgumentException("operation requires at least one selected attribute");

	final Operation[] ops = new Operation[rows.length];
	final File[] selected = selectedFiles.toArray(new File[0]);
	final Map<Integer,Map<File,Operation>> replaced = new HashMap<Integer,Map<File,Operation>>();

	for (int i = 0; i < rows.length; i++) {
	    final int tag = contents[rows[i]].getTag();       // TODO: may require translation if sorter is applied
	    ops[i] = OperationFactory.getInstance(name, tag);
	    if (replaced.containsKey(tag))
		throw new IllegalArgumentException("multiple operations specified for tag " + TagUtils.toString(tag));
	    replaced.put(tag, addOperation(ops[i], selected));
	}

	final Command command = new Command(ops, selectedFiles, replaced);
	refreshAfterCommand(command);

	return command;
    }


    /**
     * Applies the given parsed script.
     * @param statements Statements parsed from anonymization script
     * @param onlySelected if true, script is applied only to selected files; otherwise, to the whole file set
     * @return Command representing this script action
     */
    public Command doScript(final StatementList statements, final boolean onlySelected, ProgressMonitor pm) {
	final Map<Operation,Set<File>> ops = new HashMap<Operation,Set<File>>();
	final Map<Integer,Map<File,Operation>> replaced = new HashMap<Integer,Map<File,Operation>>();

	if (null == statements) {
	    return null;
	}

	final Collection<File> files;
	try {
	    files = onlySelected ? selectedFiles : fs.getDataFiles();
	} catch (SQLException e) {
	    JOptionPane.showMessageDialog(browser.getFrame(),
		    "Error getting file list: " + e.getMessage(),
		    e.getMessage(), JOptionPane.ERROR_MESSAGE);
	    return null;    // TODO: is this okay?
	}
	int progress = 0;
	pm.setMinimum(0);
	pm.setMaximum(files.size());
	pm.setProgress(progress);

	for (final File file : files) {
	    try {
		for (final Object opo : statements.getOperations(file)) {
		    final Operation op = (Operation)opo;
		    if (!ops.containsKey(op)) {
			ops.put(op, new HashSet<File>());
		    }
		    replaced.put(op.getTag(), addOperation(op, file));
		    ops.get(op).add(file);
		}
	    } catch (IOException e) { // TODO: localize
		JOptionPane.showMessageDialog(browser.getFrame(),
			"Error reading file " + file.getName() + ": " + e.getMessage(),
			e.getMessage(), JOptionPane.ERROR_MESSAGE);
	    }
	    pm.setProgress(++progress);
	}

	final Command command = new Command(ops, replaced);
	refreshAfterCommand(command);

	return command;
    }

    /**
     * If the given Command affects any currently selected files,
     * rebuild the table.
     * @param c
     */
    private void refreshAfterCommand(final Command c) {
	final Set<File> files = new HashSet<File>(c.getAllFiles());
	for (final Map<File,Operation> map : c.getReplaced().values())
	    files.addAll(map.keySet());

	files.retainAll(selectedFiles);

	if (!files.isEmpty()) {
	    executor.execute(new CacheBuilder(false));
	}
    }


    /**
     * Does a Command.  (This is probably always a Redo.)
     * @param c
     */
    public void redo(final Command c) {
	final Operation[] ops = c.getOperations();
	for (int i = 0; i < ops.length; i++)
	    addOperation(ops[i], c.getFiles(i));
	refreshAfterCommand(c);
    }


    /**
     * Undoes a Command.
     * @param c
     */
    public void undo(final Command c) {
	// Clear operation
	Set<Map.Entry<File,Operation>> restoredOps = new HashSet<Map.Entry<File,Operation>>();

	final Operation[] ops = c.getOperations();
	for (int i = 0; i < ops.length; i++) {
	    final int tag = ops[i].getTag();
	    for (final File file : c.getFiles(i)) {
		allOps.get(tag).remove(file);
		if (allOps.get(tag).isEmpty())
		    allOps.remove(tag);
	    }
	    restoredOps.addAll(c.getReplaced().get(tag).entrySet());
	}

	for (final Map.Entry<File,Operation> e : restoredOps) {
	    addOperation(e.getValue(), e.getKey());
	}

	refreshAfterCommand(c);
    }


    /**
     * Build a list of Statements equivalent to our operation map
     * @return head of Statement list
     */
    private StatementList buildStatements() {
	// Build a list of Statements equivalent to our operation map
	final Map<Operation,Set<File>> ops = new HashMap<Operation,Set<File>>();
	for (final Map<File,Operation> tagops : allOps.values()) {
	    for (final Map.Entry<File,Operation> e : tagops.entrySet()) {
		final Operation op = e.getValue();
		if (!ops.containsKey(op))
		    ops.put(op, new HashSet<File>());
		ops.get(op).add(e.getKey());
	    }
	}

	final StatementList statements = new StatementArrayList();
	for (final Map.Entry<Operation,Set<File>> e : ops.entrySet()) {
	    statements.add(new Statement(new Constraint(null, e.getValue()), e.getKey()));
	}
	return statements;
    }


    /**
     * Send the modified files (all or only the selected files) to a
     * DICOM receiver.
     * @param host DNS name of the remote host
     * @param port TCP port of the remote host
     * @param aeTitle DICOM Application Entity Name of the remote host
     * @param allFiles true if all files in the file set should be sent
     */
    void send(final String host, final String port, final String aeTitle,
	    final boolean isTLS, final boolean allFiles) {
	try {
	    final Set<File> files;
	    final TransferCapability[] tcs;
	    synchronized(this) {
		if (allFiles) {
		    files = fs.getDataFiles();
		    tcs = fs.getTransferCapabilities(TransferCapability.SCU);
		} else {
		    files = selectedFiles;
		    tcs = fs.getTransferCapabilities(TransferCapability.SCU, selectedFiles);
		}
	    }

	    if (logger.isDebugEnabled()) {
		logger.debug("Requested transfer capabilities:");
		for (final TransferCapability tc : tcs) {
		    logger.debug("SOP Class " + tc.getSopClass() + " " + tc.getRole());
		    logger.debug("TSUIDs " + Arrays.toString(tc.getTransferSyntax()));
		}
	    }

	    final DicomObjectExporter exporter = new CStoreExporter(host, port, isTLS, aeTitle, tcs);
	    final EditProgressMonitor pm = SwingProgressMonitor.getMonitor(browser.getFrame(),
		    rsrcb.getString(SENDING_FILES), "", 0, files.size());

	    final BatchExporter batch = new BatchExporter(exporter, buildStatements(), files);
	    batch.setProgressMonitor(pm, 0);
	    executor.execute(batch);
	} catch (SQLException e) {
	    logger.error("unable to get data files", e);
	    JOptionPane.showMessageDialog(browser.getFrame(),      // TODO: localize
		    "Unable to retrieve data files from database: " + e.getMessage(),
		    "Send failed",
		    JOptionPane.ERROR_MESSAGE);
	    return;
	}
    }


    private void export(final DicomObjectExporter exporter, final boolean allFiles) {
	final SortedSet<File> files;
	try {
	    files = new TreeSet<File>(allFiles ? fs.getDataFiles() : selectedFiles);
	} catch (SQLException e) {
	    logger.error("unable to get data files", e);
	    JOptionPane.showMessageDialog(browser.getFrame(),      // TODO: localize
		    "Unable to retrieve data files from database: " + e.getMessage(),
		    "Export failed",
		    JOptionPane.ERROR_MESSAGE);
	    return;
	}

	final EditProgressMonitor pm = SwingProgressMonitor.getMonitor(browser.getFrame(),
		rsrcb.getString(WRITING_FILES), "", 0, files.size());

	final BatchExporter batch =
	    new BatchExporter(exporter, buildStatements(), files);
	batch.setProgressMonitor(pm, 0);
	executor.execute(batch);
	// TODO: set a check for errors
    }

    void saveInAdjacentDir(final String format, final boolean allFiles) {
	export(new AdjacentDirFileExporter(AE_TITLE, format), allFiles);
    }

    void saveInAdjacentFile(final String suffix, final boolean allFiles) {
	export(new AdjacentFileExporter(AE_TITLE, suffix), allFiles);
    }


    void saveInNewRoot(final String rootpath, final boolean allFiles) {
	final DicomObjectExporter exporter;
	try {
	    exporter = new NewRootFileExporter(AE_TITLE, new File(rootpath), fs.getRoots());
	} catch (Exception e) {
	    logger.error("unable to resolve data roots", e);
	    JOptionPane.showMessageDialog(browser.getFrame(),      // TODO: localize
		    "Error in organizing output files: " + e.getMessage(),
		    "Export failed",
		    JOptionPane.ERROR_MESSAGE);
	    return;
	}
	export(exporter, allFiles);
    }

 
    void overwrite(final boolean allFiles) {
	export(new OverwriteFileExporter(AE_TITLE), allFiles);
    }
}