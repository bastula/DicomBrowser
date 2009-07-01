/**
 * Copyright (c) 2008-2009 Washington University
 */
package org.nrg.dcm.edit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.util.TagUtils;
import org.dom4j.DocumentException;
import org.nrg.dcm.FileSet;
import org.nrg.dcm.browse.DicomBrowser;
import org.nrg.dcm.io.BatchExporter;
import org.nrg.dcm.io.CStoreExporter;
import org.nrg.dcm.io.DicomObjectExporter;
import org.nrg.dcm.io.NewRootFileExporter;

import com.Ostermiller.util.CSVParse;
import com.Ostermiller.util.CSVParser;

/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public final class CSVRemapper {
    private static final int DICOM_DEFAULT_PORT = 104;
    private static final String AE_TITLE = "DicomRemapper";

    private final static class RemapColumn {
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

    private final static class RemapContext {
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

    private final static class RemapWithContext {
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

    private final static class InvalidCSVException extends Exception {
	private static final long serialVersionUID = 1L;
	InvalidCSVException(final String msg) {
	    super(msg);
	}
    }

    private final static class InvalidRemapsException extends Exception {
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

    private final UIDGenerator uidGenerator;
    private final Collection<RemapColumn> remaps;
    private final Map<String,Map<Integer,Integer>> selectionKeysToCols;
    private final StatementList globalStatements = new StatementArrayList();
    private final PrintStream messages = System.err;

    public CSVRemapper(final File configFile, final String uidRoot, final URL uidRootServer)
    throws IOException,ParseException,DocumentException,InvalidCSVException,InvalidUIDRootException {
	this.uidGenerator = new UIDGenerator(DicomBrowser.prefs, DicomBrowser.UID_ROOT_PREF, uidRoot, uidRootServer);

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

    private Map<RemapWithContext,String> makeAssignments(final CSVParse remapSpreadsheet)
    throws IOException,InvalidRemapsException {
	final Map<RemapWithContext,Collection<String>> assignedValues = new HashMap<RemapWithContext,Collection<String>>();

	for (String[] line = remapSpreadsheet.getLine(); null != line; line = remapSpreadsheet.getLine()) {
	    if (1 == remapSpreadsheet.getLastLineNumber())	// skip the header line
		continue;

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
	    switch (e.getValue().size()) {
	    case 0:
		underspecified.add(e.getKey());
		break;

	    case 1:
		uniqueAssignments.put(e.getKey(), e.getValue().toArray(new String[0])[0]);
		break;

	    default:
		overspecified.put(e.getKey(), e.getValue());
	    }
	}

	if (underspecified.isEmpty() && overspecified.isEmpty())
	    return uniqueAssignments;
	else
	    throw new InvalidRemapsException(underspecified, overspecified);
    }

    private static Constraint makeConstraint(final RemapWithContext remap, final Collection<File> dicomFiles) {
	final Collection<ConstraintMatch> conditions = new LinkedHashSet<ConstraintMatch>();
	for (Map.Entry<Integer,String> me : remap.context.selectionKeys.entrySet()) {
	    conditions.add(new SimpleConstraintMatch(me.getKey(), me.getValue()));
	}
	return new Constraint(new ConstraintConjunction(conditions), dicomFiles);
    }

    public Map<?,?> apply(final File remapSpreadsheet, final URI out, final Collection<File> roots)
    throws IOException,AttributeException,InvalidRemapsException,SQLException {
	final StatementList statements = new StatementArrayList(globalStatements);

	final FileSet fs = new FileSet(roots.toArray(new File[0]), false,
		new StreamProgressMonitor(messages, "Reading", "source DICOM", roots.size()));
	final Collection<File> files = fs.getDataFiles();

	if (null != remapSpreadsheet) {
	    final Reader csvr = new FileReader(remapSpreadsheet);
	    final CSVParse csvp = new CSVParser(csvr);

	    try {
		for (final Map.Entry<RemapWithContext,String> e : makeAssignments(csvp).entrySet()) {
		    statements.add(new Statement(makeConstraint(e.getKey(), files),
			    new Assignment(e.getKey().column.getTag(), e.getValue())));
		}
	    } catch (IOException e) {
		throw e;
	    } catch (InvalidRemapsException e) {
		throw e;
	    } finally {
		try { csvr.close(); } catch (IOException ignore) {}
	    }
	}

	final DicomObjectExporter exporter;

	final URI dest = out.isAbsolute() ? out : out.resolve(System.getProperty("user.dir"));
	if ("file".equals(dest.getScheme())) {
	    exporter = new NewRootFileExporter(AE_TITLE, new File(dest), roots);
	} else if ("dicom".equals(dest.getScheme())) {
	    final String locAETitle = dest.getUserInfo();
	    final String destHost = dest.getHost();
	    final int destPort = -1 == dest.getPort() ? DICOM_DEFAULT_PORT : dest.getPort();
	    final String destAETitle = dest.getPath().replaceAll("/", "");
	    exporter = new CStoreExporter(destHost, Integer.toString(destPort), false,
		    destAETitle, locAETitle,
		    fs.getTransferCapabilities(TransferCapability.SCU));
	} else {
	    throw new UnsupportedOperationException("no exporter defined for URI scheme " + out.getScheme());
	}

	final BatchExporter batch = new BatchExporter(exporter, statements, files);
	batch.setProgressMonitor(new StreamProgressMonitor(messages, "Processing", "modified DICOM", files.size()), 0);
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
	    final EditDCMLex scanner = new EditDCMLex(in);
	    final EditDCMCup parser = new EditDCMCup(scanner);
	    parser.setGenerator("UID", uidGenerator);
	    globalStatements.add((StatementList)(parser.parse().value));
	} catch (IOException e) {
	    throw e;
	} catch (Exception e) {
	    final Logger log = Logger.getLogger(CSVRemapper.class);
	    log.error("Error processing DICOM anonymization script", e);
	}
    }


    private static void showUsage(final Options opts) {
	final HelpFormatter formatter = new HelpFormatter();
	formatter.printHelp("DicomRemapper [OPTIONS] [DICOM-FILES]", opts);
    }

    /**
     * @param args
     */
    public static void main(final String[] args) throws Exception {
	final Options options = new Options();
	options.addOption("h", "help", false, "show this information");

	final Option outputOpt = new Option("o", "output", true, "target location formodified files (file path or dicom: URI)");
	outputOpt.setRequired(true);
	options.addOption(outputOpt);

	final Option uidRootOpt = new Option("r", "uid-root", true,
	"root UID from which new UIDs are constructed");
	options.addOption(uidRootOpt);

	final Option dasScriptOpt = new Option("d", "script", true, ".das script file(s) to be applied");
	options.addOption(dasScriptOpt);

	final Option configXMLOpt = new Option("c", "config", true, "configuration XML file");
	options.addOption(configXMLOpt);

	final Option valuesOpt = new Option("v", "values", true,
	"CSV spreadsheet file specifying remapped values");
	options.addOption(valuesOpt);

	final Option uidRootServerOpt = new Option("s", "uid-root-server", true,
	"server from which new root UID can be obtained");
	options.addOption(uidRootServerOpt);

	final Option forceNewRootOpt = new Option("n", "force-new-root", true,
	"obtain a new UID root from server, even if a root is already available (requires -s option)");
	options.addOption(forceNewRootOpt);

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

	final String uidServerName = cli.getOptionValue(uidRootServerOpt.getOpt());
	final URL uidServer = null == uidServerName ? null : new URL(uidServerName);
	if (cli.hasOption(forceNewRootOpt.getOpt())) {
	    if (null == uidServer) {
		System.err.println("Can only force new UID root if a root server is specified");
		System.exit(-3);
	    }
	    DicomBrowser.prefs.put(DicomBrowser.UID_ROOT_PREF, new UIDRequester(uidServer).getUID());
	}

	final String specXMLPath = cli.getOptionValue(configXMLOpt.getOpt());
	final File specXMLFile = null == specXMLPath ? null : new File(specXMLPath);
	final String uidRoot = cli.getOptionValue(uidRootOpt.getOpt());
	final CSVRemapper remapper = new CSVRemapper(specXMLFile, uidRoot, uidServer);

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

	final URI out = new URI(cli.getOptionValue(outputOpt.getOpt()));

	final Collection<File> infiles = new ArrayList<File>();
	for (final String path : cli.getArgs()) {
	    infiles.add(new File(path.toString()));
	}

	final String remapCSVPath = cli.getOptionValue(valuesOpt.getOpt());
	final File remapCSV = null == remapCSVPath ? null : new File(remapCSVPath);
	final Map<?,?> failures = remapper.apply(remapCSV, out, infiles);
	if (!failures.isEmpty()) {
	    System.err.println("Output failed for some objects:");
	    for (final Map.Entry<?,?> me : failures.entrySet()) {
		System.err.println(me.getKey() + ": " + me.getValue());
	    }
	}
    }
}
