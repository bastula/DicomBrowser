/**
 * Copyright (c) 2008-2011 Washington University
 */
package org.nrg.dcm.edit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.util.TagUtils;
import org.dom4j.DocumentException;
import org.nrg.dcm.edit.DicomUtils.DicomObjectException;
import org.nrg.dcm.edit.gen.UIDGenerator;
import org.nrg.dcm.io.BatchExporter;
import org.nrg.dcm.io.CStoreExporter;
import org.nrg.dcm.io.DicomObjectExporter;
import org.nrg.dcm.io.NewRootFileExporter;
import org.nrg.dcm.io.TransferCapabilityExtractor;
import org.nrg.io.FileWalkIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public final class CSVRemapper {
    private static final int DICOM_DEFAULT_PORT = 104;
    private static final String AE_TITLE = "DicomRemap";

    private static final class RemapColumn {
        final String level;
        final int index;
        final int tag;

        RemapColumn(final String level, final int index, final int tag) {
            this.level = level;
            this.index = index;
            this.tag = tag;
        }

        String getLevel() { return level; }
        int getIndex() { return index; }
        int getTag() { return tag; }

        public String toString() {
            final StringBuilder sb = new StringBuilder("RemapColumn ");
            sb.append(index);
            sb.append(" ");
            sb.append(TagUtils.toString(tag));
            sb.append(" (level ");
            sb.append(level);
            sb.append(")");
            return sb.toString();
        }
    }

    private static final class RemapContext {
        final String level;
        final Map<Integer,String> selectionKeys;

        RemapContext(final String level, final Map<Integer,String> keys) {
            this.level = level;
            this.selectionKeys = new LinkedHashMap<Integer,String>(keys);
        }

        public boolean equals(final Object o) {
            if (!(o instanceof RemapContext)) return false;
            final RemapContext other = (RemapContext)o;
            return level.equals(other.level) && selectionKeys.equals(other.selectionKeys);
        }

        public int hashCode() {
            int result = 17;
            result = 37*result + level.hashCode();
            result = 37*result + selectionKeys.hashCode();
            return result;
        }

        public String toString() {
            final StringBuilder sb = new StringBuilder("RemapContext (level ");
            sb.append(level);
            sb.append("): ");
            for (final Map.Entry<Integer,String> e : selectionKeys.entrySet()) {
                sb.append(TagUtils.toString(e.getKey()));
                sb.append("=\"");
                sb.append(e.getValue());
                sb.append("\" ");
            }
            return sb.toString();
        }
    }

    private static final class RemapWithContext {
        final RemapContext context;
        final RemapColumn column;

        RemapWithContext(final RemapContext context, final RemapColumn column) {
            this.context = context;
            this.column = column;
        }

        public boolean equals(final Object o) {
            if (!(o instanceof RemapWithContext)) return false;
            final RemapWithContext other = (RemapWithContext)o;
            return context.equals(other.context) && column.equals(other.column);
        }

        public int hashCode() {
            int result = 17;
            result = 37 * result + context.hashCode();
            result = 37 * result + column.hashCode();
            return result;
        }

        public String toString() {
            final StringBuilder sb = new StringBuilder("RemapWithContext: ");
            sb.append(column);
            sb.append(" in ");
            sb.append(context);
            return sb.toString();
        }
    }

    private static final class InvalidCSVException extends Exception {
        private static final long serialVersionUID = 1L;
        InvalidCSVException(final String msg) {
            super(msg);
        }
    }

    private static final class InvalidRemapsException extends Exception {
        private static final long serialVersionUID = 1L;
        private static final String LINE_SEPARATOR = System.getProperty("line.separator");
        final Collection<RemapWithContext> underspecified;
        final Map<RemapWithContext,Collection<String>> overspecified;

        InvalidRemapsException(Collection<RemapWithContext> underspecified, Map<RemapWithContext,Collection<String>> overspecified) {
            this.underspecified = underspecified;
            this.overspecified = overspecified;
            assert !underspecified.isEmpty() || !overspecified.isEmpty();
        }

        public String toString() {
            final StringBuilder sb = new StringBuilder();
            if (!underspecified.isEmpty()) {
                sb.append("The following remaps were not assigned a value:");
                sb.append(LINE_SEPARATOR);
                for (final RemapWithContext rwc : underspecified) {
                    sb.append(rwc);
                    sb.append(LINE_SEPARATOR);
                }
            }
            if (!overspecified.isEmpty()) {
                sb.append("The following remaps were assigned multiple values:");
                sb.append(LINE_SEPARATOR);
                for (final Map.Entry<RemapWithContext,Collection<String>> e : overspecified.entrySet()) {
                    sb.append(e.getKey()).append(" : ").append(e.getValue());
                    sb.append(LINE_SEPARATOR);
                }
            }
            return sb.toString();
        }
    }

    private final ValueGenerator uidGenerator = new UIDGenerator();
    private final Collection<RemapColumn> remaps;
    private final Map<String,Map<Integer,Integer>> selectionKeysToCols;
    private final List<Statement> globalStatements = Lists.newArrayList();
    private final PrintStream messages = System.err;

    public CSVRemapper(final File configFile)
    throws IOException,ParseException,DocumentException,InvalidCSVException {
        final ConfigurableDirectoryRecordFactory factory = new ConfigurableDirectoryRecordFactory(configFile);
        final List<DicomTableEntry> columns = new ArrayList<DicomTableEntry>(factory.getColumns());

        // Collect all remappings, and all levels for which a remapping is defined
        final Collection<String> levels = new HashSet<String>();
        remaps = new ArrayList<RemapColumn>();
        for (int i = 0; i < columns.size(); i++) {
            final DicomTableEntry col = columns.get(i);
            if (col.isSubstitution()) {
                final String level = col.getLevel();
                remaps.add(new RemapColumn(level, i, col.getTag()));
                levels.add(level);
            }
        }

        // For each level for which a remapping is defined, figure out which columns hold the selection keys.
        selectionKeysToCols = new HashMap<String,Map<Integer,Integer>>();
        for (final String level : levels) {
            final Collection<Integer> selectionTags = factory.getSelectionTags(level);
            final Map<Integer,Integer> keysToCols = new LinkedHashMap<Integer,Integer>();
            selectionKeysToCols.put(level, keysToCols);
            for (final int tag : selectionTags) {
                boolean foundTag = false;
                for (int i = 0; i < columns.size(); i++) {
                    final DicomTableEntry col = columns.get(i);
                    if (!col.isSubstitution() && tag == col.getTag()) {
                        foundTag = true;
                        keysToCols.put(tag, i);
                        break;
                    }
                }
                if (!foundTag)
                    throw new InvalidCSVException("Unable to find column for level " + level + " required tag " + TagUtils.toString(tag));
            }
        }
    }

    private Map<RemapWithContext,String> makeAssignments(final CSVReader remapSpreadsheet)
    throws IOException,InvalidRemapsException {
        final Map<RemapWithContext,Collection<String>> assignedValues = Maps.newHashMap();;

        String[] line = remapSpreadsheet.readNext();	// skip the header line
        while (null != (line = remapSpreadsheet.readNext())) {
            // Build the full remapping context for this line
            final Map<String,RemapContext> context = new HashMap<String,RemapContext>();
            for (final String level : selectionKeysToCols.keySet()) {
                final Map<Integer,String> keyValues = new LinkedHashMap<Integer,String>();
                for (final Map.Entry<Integer,Integer> e : selectionKeysToCols.get(level).entrySet()) {
                    keyValues.put(e.getKey(), line[e.getValue()]);
                }
                context.put(level, new RemapContext(level, keyValues));
            }

            // Now collect the assignments
            for (final RemapColumn col : remaps) {
                final RemapWithContext remap = new RemapWithContext(context.get(col.getLevel()), col);
                if (!assignedValues.containsKey(remap)) {
                    assignedValues.put(remap, new LinkedHashSet<String>());
                }
                final String value = line[col.getIndex()];
                if (null != value && !"".equals(value)) {
                    assignedValues.get(remap).add(value);
                }
            }
        }

        // Review the assignments for each remapping in context:
        // n = 0 : error: no value specified
        // n = 1 : perfect!
        // n > 1 : error: multiple values specified for same context
        final Collection<RemapWithContext> underspecified = new LinkedList<RemapWithContext>();
        final Map<RemapWithContext,Collection<String>> overspecified = new LinkedHashMap<RemapWithContext,Collection<String>>();
        final Map<RemapWithContext,String> uniqueAssignments = new LinkedHashMap<RemapWithContext,String>();
        for (final Map.Entry<RemapWithContext,Collection<String>> e : assignedValues.entrySet()) {
            final Iterator<String> vi = e.getValue().iterator();
            if (vi.hasNext()) {
                final String v = vi.next();
                if (vi.hasNext()) {
                    overspecified.put(e.getKey(), e.getValue());
                } else {
                    uniqueAssignments.put(e.getKey(), v);
                }
            } else {
                underspecified.add(e.getKey());
            }
        }

        if (underspecified.isEmpty() && overspecified.isEmpty()) {
            return uniqueAssignments;
        } else {
            throw new InvalidRemapsException(underspecified, overspecified);
        }
    }

    private static Constraint makeConstraint(final RemapWithContext remap) {
        final Collection<ConstraintMatch> conditions = new LinkedHashSet<ConstraintMatch>();
        for (Map.Entry<Integer,String> me : remap.context.selectionKeys.entrySet()) {
            conditions.add(new SimpleConstraintMatch(me.getKey(), me.getValue()));
        }
        return new Constraint(new ConstraintConjunction(conditions));
    }

    /**
     * 
     * @param remapSpreadsheet
     * @param out destination URI; must be absolute
     * @param roots
     * @return
     * @throws IOException
     * @throws AttributeException
     * @throws InvalidRemapsException
     * @throws SQLException
     */
    public Map<?,?> apply(final File remapSpreadsheet, final URI out, final Collection<File> files)
    throws IOException,AttributeException,InvalidRemapsException,SQLException {
        final List<Statement> statements = Lists.newArrayList(globalStatements);

        if (null != remapSpreadsheet) {
            IOException ioexception = null;
            final CSVReader reader = new CSVReader(new FileReader(remapSpreadsheet));
            try {
                for (final Map.Entry<RemapWithContext,String> e : makeAssignments(reader).entrySet()) {
                    statements.add(new Statement(makeConstraint(e.getKey()),
                            new Assignment(e.getKey().column.getTag(), e.getValue())));
                }
            } catch (IOException e) {
                throw ioexception = e;
            } catch (InvalidRemapsException e) {
                throw e;
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw null == ioexception ? e : ioexception;
                }
            }
        }

        final DicomObjectExporter exporter;

        int count;
        if (!out.isAbsolute()) {
            throw new IllegalArgumentException("destination URI must be absolute");
        }
        if ("file".equals(out.getScheme())) {
            final Set<File> roots = new LinkedHashSet<File>();	// only directories can be roots
            for (final File file : files) {
                if (file.isDirectory()) {
                    roots.add(file);
                } else if (file.exists()) {
                    roots.add(file.getParentFile());
                }
            }
            exporter = new NewRootFileExporter(AE_TITLE, new File(out), roots);
            count = 0;
        } else if ("dicom".equals(out.getScheme())) {
            final String locAETitle = out.getUserInfo();
            final String destHost = out.getHost();
            final int destPort = -1 == out.getPort() ? DICOM_DEFAULT_PORT : out.getPort();
            final String destAETitle = out.getPath().replaceAll("/", "");
            final FileWalkIterator walker = new FileWalkIterator(files,
                    new StreamProgressMonitor(messages, "Searching", "original DICOM"));
            final TransferCapability[] tcs = TransferCapabilityExtractor.getTransferCapabilities(walker, TransferCapability.SCU);
            count = walker.getCount();
            exporter = new CStoreExporter(destHost, Integer.toString(destPort), false,
                    destAETitle, locAETitle, tcs);
        } else {
            throw new UnsupportedOperationException("no exporter defined for URI scheme " + out.getScheme());
        }

        final BatchExporter batch = new BatchExporter(exporter, statements, new FileWalkIterator(files, null));
        batch.setProgressMonitor(new StreamProgressMonitor(messages, "Processing", "modified DICOM", count), 0);
        batch.run();
        return batch.getFailures();
    }

    public void includeStatements(final File dasFile) throws IOException {
        final InputStream in = new FileInputStream(dasFile);
        includeStatements(in);
        try { in.close(); } catch (IOException ignore) {}
    }

    public void includeStatements(final InputStream in) throws IOException {
        try {
            final ScriptApplicator applicator = new ScriptApplicator(in,
                    new HashMap<String,ScriptFunction>(),
                    Collections.singletonMap("UID", uidGenerator));
            globalStatements.addAll(applicator.getStatements());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            final Logger log = LoggerFactory.getLogger(CSVRemapper.class);
            log.error("Error processing DICOM anonymization script", e);
        }
    }


    private static void showUsage(final Options opts) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("DicomRemapper [OPTIONS] [directory | file ...]", opts);
    }

    private static URI toURI(final String path) throws URISyntaxException {
        final URI uri = new URI(path.replace(File.separatorChar, '/'));
        if (uri.isAbsolute() && uri.getScheme().length() > 1) {
            return uri;
        } else {
            return new File(path).toURI();
        }
    }

    /**
     * @param args
     */
    public static void main(final String[] args) throws Exception {
        final Options options = new Options();
        options.addOption("h", "help", false, "show this information");

        final Option outputOpt = new Option("o", "output", true, "target for modified files (directory path or dicom: URI)");
        outputOpt.setRequired(true);
        options.addOption(outputOpt);

        final Option dasScriptOpt = new Option("d", "script", true, ".das script file(s) to be applied");
        options.addOption(dasScriptOpt);

        final Option configXMLOpt = new Option("c", "config", true, "configuration XML file");
        options.addOption(configXMLOpt);

        final Option verboseOpt = new Option("x", false, "Display long information about errors");
        options.addOption(verboseOpt);

        final Option valuesOpt = new Option("v", "values", true,
        "CSV spreadsheet file specifying remapped values");
        options.addOption(valuesOpt);

        final CommandLineParser parser = new PosixParser(); 
        final CommandLine cli;
        try {
            cli = parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            showUsage(options);
            System.exit(-1);
            return;
        }
        if (cli.hasOption('h')) {
            showUsage(options);
            System.exit(0);
            return;
        }

        if (cli.hasOption(valuesOpt.getOpt()) && !cli.hasOption(configXMLOpt.getOpt())) {
            System.err.println("Must specify configuration XML if values CSV is provided.");
            System.exit(-4);
        }

        final String specXMLPath = cli.getOptionValue(configXMLOpt.getOpt());
        final File specXMLFile = null == specXMLPath ? null : new File(specXMLPath);
        final CSVRemapper remapper = new CSVRemapper(specXMLFile);

        final String[] dasPaths = cli.getOptionValues(dasScriptOpt.getOpt());
        if (null != dasPaths) {
            for (final String path : dasPaths) {
                final File dasFile = new File(path);
                if (dasFile.isFile() && dasFile.canRead()) {
                    remapper.includeStatements(dasFile);
                } else if ("basic".equals(path)) {	// special case, included in jar
                    final InputStream in = ClassLoader.getSystemResourceAsStream("BALCP.das");
                    if (null != in) {
                        remapper.includeStatements(in);
                        in.close();
                    } else {
                        System.err.println("Warning: unable to find basic anon script");
                    }
                } else {
                    System.err.println("Warning: unable to read anon script " + path);
                }
            }
        }

        final URI out = toURI(cli.getOptionValue(outputOpt.getOpt()));

        final Collection<File> infiles = new ArrayList<File>();
        for (final String path : cli.getArgs()) {
            infiles.add(new File(path.toString()));
        }

        final String remapCSVPath = cli.getOptionValue(valuesOpt.getOpt());
        final File remapCSV = null == remapCSVPath ? null : new File(remapCSVPath);
        final Map<?,?> failures = remapper.apply(remapCSV, out, infiles);
        if (!failures.isEmpty()) {
            final boolean verbose = cli.hasOption(verboseOpt.getOpt());
            System.err.println("Output failed for some objects:");
            for (final Map.Entry<?,?> me : failures.entrySet()) {
                System.err.print(me.getKey() + ": ");
                final Object v = me.getValue();
                if (v instanceof DicomObjectException) {
                    System.err.println("not DICOM data");
                } else {
                    System.err.println(v);
                    if (verbose && v instanceof Throwable) {
                        ((Throwable)v).printStackTrace(System.err);
                    }
                }
            }
        }
    }
}
