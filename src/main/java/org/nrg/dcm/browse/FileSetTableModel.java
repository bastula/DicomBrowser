/**
 * Copyright (c) 2006-2011 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;

import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreePath;

import org.dcm4che2.data.Tag;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.util.TagUtils;
import org.nrg.attr.ConversionFailureException;
import org.nrg.dcm.DirectoryRecord;
import org.nrg.dcm.FileSet;
import org.nrg.dcm.ProgressMonitorI;
import org.nrg.dcm.edit.Assignment;
import org.nrg.dcm.edit.Constraint;
import org.nrg.dcm.edit.Deletion;
import org.nrg.dcm.edit.Operation;
import org.nrg.dcm.edit.ScriptEvaluationException;
import org.nrg.dcm.edit.Statement;
import org.nrg.dcm.io.AdjacentDirFileExporter;
import org.nrg.dcm.io.AdjacentFileExporter;
import org.nrg.dcm.io.BatchExporter;
import org.nrg.dcm.io.CStoreExporter;
import org.nrg.dcm.io.DicomObjectExporter;
import org.nrg.dcm.io.MultifileExporter;
import org.nrg.dcm.io.NewRootFileExporter;
import org.nrg.dcm.io.OverwriteFileExporter;
import org.nrg.util.EditProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;


/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class FileSetTableModel extends AbstractTableModel
implements TreeSelectionListener,MultifileExporter {
    private static final long serialVersionUID = -5593848932119145572L;
    public static final String AE_TITLE = "DicomBrowser";

    private final Logger logger = LoggerFactory.getLogger(FileSetTableModel.class);

    private static final String colNames[] = {"tag", "name", "action", "value"};
    private static final String SENDING_FILES = "Sending files...";
    public static final String WRITING_FILES = "Writing files...";
    public static final String COPYING_FILES = "Copying files...";
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

    public static final ResourceBundle rsrcb = new ListResourceBundle() {
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
                {COPYING_FILES, COPYING_FILES},
                {"deleted", "(deleted)"},
                {OUT_OF_MEMORY_TITLE, OUT_OF_MEMORY_TITLE},
                {OUT_OF_MEMORY_MESSAGE, OUT_OF_MEMORY_MESSAGE},
        };
    };
    private static final int MAXTAG = Tag.IconImageSequence - 1;
    private static final long CONTENTS_SHOW_INTERVAL = 1000;
    private static final long CONTENTS_UPDATE_INTERVAL = 4000;       // ms, heuristic

    private static final int nCols = 4;
    public static final int ACTION_COLUMN = 2;
    public static final int VALUE_COLUMN = 3;

    static final int MAX_VALUES_SHOWN = 4;
    static final String MORE_VALUES = "...";

    public final ExecutorService executor;
    private final FileSet fs;
    private final Set<TreePath> fileSelection = Sets.newLinkedHashSet();
    private final Set<File> selectedFiles = Sets.newLinkedHashSet();
    private final Map<Integer,Map<File,Operation>> allOps = Maps.newHashMap();
    private final DicomBrowser browser;

    private StatusBar.TaskMonitor cachingProgress = null;

    private List<MultiValueAttribute> contents = Lists.newArrayList();

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


    public FileSetTableModel(final DicomBrowser browser, final FileSet fs, final ExecutorService exs) {
        this.fs = fs;
        this.browser = browser;
        this.executor = exs;
    }

    public final SetMultimap<Integer,String> asMultimap() {
        synchronized (contents) {
            final SetMultimap<Integer,String> m = LinkedHashMultimap.create();
            for (final MultiValueAttribute mva: contents) {
                m.putAll(mva.getTag(), Arrays.asList(mva.getValues()));
            }
            return m;
        }
    }

    public void dispose() {
        allOps.clear();
        fs.dispose();
    }

    private void collectReferencedFiles(final DirectoryRecord root, final Set<File> files) {
        final Queue<DirectoryRecord> records = Lists.newLinkedList();
        records.add(root);
        while (records.peek() != null) {
            final DirectoryRecord dr = records.poll();
            final String rfpath = dr.getValue(Tag.ReferencedFileID);
            if (rfpath != null)
                files.add(new File(rfpath));
            records.addAll(dr.getLower());
        }
    }

    private void updateValues(final SetMultimap<Integer,String> vals, final Collection<Integer> modified) {
        assert vals.keySet().containsAll(modified);
        final List<MultiValueAttribute> newContents = Lists.newArrayList();
        for (final Integer tag : vals.keySet()) {
            newContents.add(MultiValueAttribute.Factory.create(fs, tag, modified.contains(tag), vals.get(tag)));
        }
        contents = newContents;
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
        if (cachingProgress != null) {
            cachingProgress.cancel();
        }

        final List<File> localSelectedFiles;
        synchronized(selectedFiles) {
            selectedFiles.clear();
            for (final TreePath tp : fileSelection) {
                final DirectoryRecord dr = (DirectoryRecord)tp.getLastPathComponent();
                collectReferencedFiles(dr, selectedFiles);
            }
            localSelectedFiles = Lists.newArrayList(selectedFiles);
        }

        if (wipeTableFirst) {
            contents.clear();
        }

        long lastUpdate = new Date().getTime() - CONTENTS_UPDATE_INTERVAL + CONTENTS_SHOW_INTERVAL;

        cachingProgress = browser.statusBar.getTaskMonitor(0, localSelectedFiles.size(),
                rsrcb.getString("reading-values"), rsrcb.getString("done"));

        final ProgressMonitorI localTaskMonitor = cachingProgress;

        final SetMultimap<Integer,String> vals = LinkedHashMultimap.create();
        final Set<Integer> modified = Sets.newLinkedHashSet();

        int progress = 0;
        for (final File file : localSelectedFiles) {
            cachingProgress.setNote(String.format(rsrcb.getString(READING_VALS_FORMAT), file.getName()));

            final Map<Integer,String> fv;
            try {
                final Map<Integer,ConversionFailureException> failures = Maps.newLinkedHashMap();
                fv = fs.getValuesFromFile(file, 0, MAXTAG, failures);
                if (failures.isEmpty()) {
                    // Build the values for the selected files with operations applied.
                    // Some operations may add new attributes, so the complete set of tags
                    // comes from both files and operations.
                    final SortedSet<Integer> tags = Sets.newTreeSet(Sets.union(fv.keySet(), allOps.keySet()));
                    for (final int tag : tags) {
                        final Map<File,Operation> tagops = allOps.get(tag);
                        if (null != tagops && tagops.containsKey(file)) {
                            modified.add(tag);
                            try {
                                final String v = tagops.get(file).apply(fv);
                                vals.put(tag, null == v ? rsrcb.getString("deleted") : v);
                            } catch (ScriptEvaluationException e) {
                                logger.error("error applying script", e);
                            }
                        } else {
                            vals.put(tag, fv.get(tag));
                        }
                    }
                } else {
                    logger.error("Conversion errors reading {} : {}", file, failures);
                    continue; // move on to next file
                }
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("error caching values", e);
                continue;     // move on to next file
            } catch (SQLException e) {
                e.printStackTrace();
                logger.error("error caching values", e);
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
        for (int i = 0; i < tps.length; i++) {
            if (e.isAddedPath(i)) {
                fileSelection.add(tps[i]);
            } else {
                needsWipe = true;       // some of the attributes now displayed may be gone
                fileSelection.remove(tps[i]);
            }
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
        return contents.size();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        assert 0 <= rowIndex;

        synchronized(contents) {
            // These weird conditions can happen if contents update between
            // the getRowCount() and the getValueAt(); don't sweat it.
            if (rowIndex >= contents.size()) {
                return null;
            }
            final MultiValueAttribute row = contents.get(rowIndex);
            if (null == row) {
                return null;
            }

            switch (columnIndex) {
            case 0: return row.getTagString();
            case 1: return row.getNameString();

            case ACTION_COLUMN: {
                final int tag = row.getTag();
                final Set<Operation> ops = Sets.newHashSet();
                synchronized(selectedFiles) {
                    for (final File file : selectedFiles) {
                        if (allOps.containsKey(tag)) {
                            ops.add(allOps.get(tag).get(file));  // null here = KEEP
                        } else {
                            ops.add(null);       // implicit KEEP
                        }
                        if (ops.size() > 1) {
                            return OperationFactory.getMultipleName();
                        }
                    }
                }
                assert ops.size() == 1;
                final Operation op = ops.iterator().next();
                return null == op ? OperationFactory.getDefaultName() : op.getName();
            }

            case VALUE_COLUMN: return row;

            default: throw new IndexOutOfBoundsException("bad column index");
            }
        }
    }

    public boolean allowClear(final ListSelectionModel lsm) {
        for (int i = lsm.getMinSelectionIndex(); i <= lsm.getMaxSelectionIndex(); i++) {
            if (lsm.isSelectedIndex(i)) {
                return contents.get(i).isModifiable();
            }
        }
        return false;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == FileSetTableModel.VALUE_COLUMN && contents.get(row).isModifiable();
    }

    @Override
    public void setValueAt(final Object o, final int row, final int col) {
        assert VALUE_COLUMN == col;
        if (o == null) {
            return;      // this can happen when an editing operation is canceled
        } else if (o instanceof Operation) {
            doOperation((Operation)o);
        } else if (o instanceof String) {
            final MultiValueAttribute attr = contents.get(row);
            // If this attribute has a single value over the fileSelection,
            // and it hasn't changed, return without doing anything.
            if (1 == attr.size() && attr.toString().equals(o)) {
                return;
            }

            final String val = (String)o;
            doOperation(OperationFactory.getInstance(OperationFactory.ASSIGN,
                    attr.getTag(), val));

            // if we're in the middle of building the values table,
            // we'll need to start over.
            if (cachingProgress != null) {
                executor.execute(new CacheBuilder(false));
            }
        } else if (o instanceof MultiValueAttribute) { // only on canceled edit
            if (!contents.get(row).equals(o)) {
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
        final Map<File,Operation> replacedOps = addOperation(op, selectedFiles);
        browser.add(new Command(op, selectedFiles, replacedOps));

        // Assign the modified row contents
        //        final int tag = op.getTagSubject();

        boolean applied = false;
        for (int row = 0; row < contents.size(); row++) {
            final int tag = contents.get(row).getTag();
            if (op.getAffectedTags().contains(tag)) {
                if (op instanceof Assignment) {
                    // Our assignments are simple string assignments that don't depend
                    // on other tags, so we can get the new value directly.
                    try {
                        contents.set(row, MultiValueAttribute.Factory.create(fs, tag, true,
                                (((Assignment)op).apply(null))));
                    } catch (ScriptEvaluationException e) {
                        e.printStackTrace();
                    }
                } else if (op instanceof Deletion) {
                    // Attribute deletion is even simpler.
                    contents.set(row, MultiValueAttribute.Factory.create(fs, tag, true, (String)null));
                } else if (op instanceof Keep) {
                    // Keep is sort of complicated and require a values rebuild.
                    // This is time-consuming, so we do it in a separate thread.
                    contents.set(row, null);
                    executor.execute(new CacheBuilder(false));
                } else {
                    throw new UnsupportedOperationException("unknown operation " + op);
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
            String v;
            try {
                v = ((Assignment)op).apply(null);
            } catch (ScriptEvaluationException e) {
                v = null;
            }
            final List<MultiValueAttribute> tcontents = Lists.newArrayList(contents);
            for (final int tag : op.getAffectedTags()) {
                final MultiValueAttribute value = MultiValueAttribute.Factory.create(fs, tag, true, v);
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
                    newRow = contents.size();
                }
                contents = tcontents;
                fireTableRowsInserted(newRow,newRow);
            }
        }
    }

    /**
     * Builds a record of replaced operations.
     * The caller is responsible for updating the table.
     * @param ops Operations to be applied
     * @return Map of replaced Operations and the File to which they applied.
     */
    private Map<File,Operation> addOperation(final Operation op, final Iterable<File> files) {
        final Map<File,Operation> replaced = Maps.newHashMap();
        for (final int tag : op.getAffectedTags()) {
            for (final File file : files) {
                if (!allOps.containsKey(tag)) {
                    allOps.put(tag, Maps.<File,Operation>newHashMap());
                }
                if (allOps.get(tag).containsKey(file)) {
                    replaced.put(file, allOps.get(tag).get(file));
                }
                allOps.get(tag).put(file, op);
            }
        }
        return replaced;        
    }

    private Map<File,Operation> addOperation(final Operation op, final File file) {
        return addOperation(op, Collections.singleton(file));
    }

    public Command addOperations(final String name, final int...rows) {
        if (rows.length == 0) {
            throw new IllegalArgumentException("operation requires at least one selected attribute");
        }

        final Operation[] ops = new Operation[rows.length];
        final Map<Integer,Map<File,Operation>> replaced = Maps.newHashMap();

        for (int i = 0; i < rows.length; i++) {
            final int tag = contents.get(rows[i]).getTag();       // TODO: may require translation if sorter is applied
            ops[i] = OperationFactory.getInstance(name, tag);
            if (replaced.containsKey(tag)) {
                throw new IllegalArgumentException("multiple operations specified for tag " + TagUtils.toString(tag));
            }
            replaced.put(tag, addOperation(ops[i], selectedFiles));
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
    public Command doScript(final Iterable<Statement> statements, final boolean onlySelected, ProgressMonitor pm) {
        final SetMultimap<Operation,File> ops = LinkedHashMultimap.create();
        final Map<Integer,Map<File,Operation>> replaced = Maps.newHashMap();

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
                for (final Object opo : Statement.getOperations(statements, file)) {
                    final Operation op = (Operation)opo;
                    for (final int tag : op.getAffectedTags()) {
                        replaced.put(tag, addOperation(op, file));
                    }
                    ops.put(op, file);
                }
            } catch (IOException e) { // TODO: localize
                JOptionPane.showMessageDialog(browser.getFrame(),
                        "Error reading file " + file.getName() + ": " + e.getMessage(),
                        e.getMessage(), JOptionPane.ERROR_MESSAGE);
            } catch (ScriptEvaluationException e) {
                JOptionPane.showMessageDialog(browser.getFrame(),
                        "Error applying script: " + e.getMessage(),
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
        final Set<File> files = Sets.newHashSet(c.getAllFiles());
        for (final Map<File,Operation> map : c.getReplaced().values()) {
            files.addAll(map.keySet());
        }

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
        for (int i = 0; i < ops.length; i++) {
            addOperation(ops[i], Arrays.asList(c.getFiles(i)));
        }
        refreshAfterCommand(c);
    }


    /**
     * Undoes a Command.
     * @param c
     */
    public void undo(final Command c) {
        // Clear operation
        Set<Map.Entry<File,Operation>> restoredOps = Sets.newLinkedHashSet();

        final Operation[] ops = c.getOperations();
        for (int i = 0; i < ops.length; i++) {
            for (final int tag : ops[i].getAffectedTags()) {
                for (final File file : c.getFiles(i)) {
                    allOps.get(tag).remove(file);
                    if (allOps.get(tag).isEmpty()) {
                        allOps.remove(tag);
                    }
                }
                restoredOps.addAll(c.getReplaced().get(tag).entrySet());
            }
        }

        for (final Map.Entry<File,Operation> e : restoredOps) {
            addOperation(e.getValue(), e.getKey());
        }

        refreshAfterCommand(c);
    }

    public int size() throws SQLException {
        return fs.size();
    }


    /**
     * Build a list of Statements equivalent to our operation map
     * @return head of Statement list
     */
    private List<Statement> buildStatements() {
        // Build a list of Statements equivalent to our operation map
        final SetMultimap<Operation,File> ops = LinkedHashMultimap.create();
        for (final Map<File,Operation> tagops : allOps.values()) {
            for (final Map.Entry<File,Operation> e : tagops.entrySet()) {
                ops.put(e.getValue(), e.getKey());
            }
        }

        final List<Statement> statements = Lists.newArrayList();
        for (final Operation op : ops.keySet()) {
            statements.add(new Statement(new Constraint(null, ops.get(op)), op));
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
    void send(final String host, final String port, final String remAETitle,
            final boolean isTLS, final String locAETitle, final boolean allFiles) {
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

            final DicomObjectExporter exporter = new CStoreExporter(host, port, isTLS, remAETitle, locAETitle, tcs);
            final EditProgressMonitor pm = SwingProgressMonitor.getMonitor(browser.getFrame(),
                    rsrcb.getString(SENDING_FILES), "", 0, files.size());

            final BatchExporter batch = new BatchExporter(exporter, buildStatements(), files);
            batch.setProgressMonitor(pm, 0);
            executor.execute(new ExportFailureHandler(batch, browser.getFrame()));
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
            files = Sets.newTreeSet(allFiles ? fs.getDataFiles() : selectedFiles);
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

        final BatchExporter batch = new BatchExporter(exporter, buildStatements(), files);
        batch.setProgressMonitor(pm, 0);
        executor.execute(new ExportFailureHandler(batch, browser.getFrame()));
    }

    public void saveInAdjacentDir(final String format, final boolean allFiles) {
        export(new AdjacentDirFileExporter(AE_TITLE, format), allFiles);
    }

    public void saveInAdjacentFile(final String suffix, final boolean allFiles) {
        export(new AdjacentFileExporter(AE_TITLE, suffix), allFiles);
    }

    public void saveInNewRoot(final String rootpath, final boolean allFiles) {
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

    public void overwrite(final boolean allFiles) {
        export(new OverwriteFileExporter(AE_TITLE), allFiles);
    }
}