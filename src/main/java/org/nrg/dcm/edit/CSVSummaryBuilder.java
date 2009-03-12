/**
 * $Id: CSVSummaryBuilder.java,v 1.9 2008/04/11 17:20:07 karchie Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.dcm.edit;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dom4j.DocumentException;
import org.nrg.dcm.DirectoryRecord;
import org.nrg.dcm.FileSet;
import org.nrg.dcm.ProgressMonitorI;

import com.Ostermiller.util.CSVPrint;
import com.Ostermiller.util.CSVPrinter;

/**
 * Builds a CSV spreadsheet summary of many DICOM files.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
public class CSVSummaryBuilder {
  private static final String EMPTY_STRING = "";
  private static final String[] STRING_ARRAY = new String[0];
  
  private final ConfigurableDirectoryRecordFactory factory;

  private static void makeHeader(final CSVPrint csv, final Collection<DicomTableEntry> cols) {
    final Collection<String> labels = new LinkedList<String>();
    for (final DicomTableEntry col : cols) {
      labels.add(col.getHeader());
    }
    csv.println(labels.toArray(STRING_ARRAY));
  }

  private static void addRow(final CSVPrint csv, final Collection<DicomTableEntry> entries, final Map<Integer,String> values) {
    final Collection<String> cols = new LinkedList<String>();
    for (final DicomTableEntry entry : entries) {
      cols.add(entry.isSubstitution() ? EMPTY_STRING : values.get(entry.getTag()));
    }
    csv.println(cols.toArray(STRING_ARRAY));
  }

  public CSVSummaryBuilder(final File configFile) throws IOException,DocumentException,ParseException {
    factory = new ConfigurableDirectoryRecordFactory(configFile);
  }

  public void run(final CSVPrint spreadsheet, final ProgressMonitorI pm, final File...files) throws Exception {
    // Trawl through the files to build the Directory Record representation
    final FileSet fs = new FileSet(files, null, factory, pm);

    makeHeader(spreadsheet, factory.getColumns());
    
    if (factory.definesPatientRows()) {
      for (final DirectoryRecord patient : fs.getPatientDirRecords()) {
	final Map<Integer,String> patientValues = new HashMap<Integer,String>();
	for (final int tag : patient.getSelectionKeys())
	  patientValues.put(tag, patient.getValue(tag));

	if (factory.definesStudyRows()) {
	  for (final DirectoryRecord study : patient.getLower()) {
	    final Map<Integer,String> studyValues = new HashMap<Integer,String>(patientValues);
	    for (final int tag : study.getSelectionKeys())
	      studyValues.put(tag, study.getValue(tag));

	    if (factory.definesSeriesRows()) {
	      for (final DirectoryRecord series : study.getLower()) {
		final Map<Integer,String> seriesValues = new HashMap<Integer,String>(studyValues);
		for (final int tag : series.getSelectionKeys())
		  seriesValues.put(tag, series.getValue(tag));

		if (factory.definesInstanceRows()) {
		  for (final DirectoryRecord instance : series.getLower()) {
		    final Map<Integer,String> instanceValues = new HashMap<Integer,String>(seriesValues);
		    for (final int tag : instance.getSelectionKeys())
		      instanceValues.put(tag, instance.getValue(tag));

		    addRow(spreadsheet, factory.getColumns(), instanceValues);
		  }
		} else {
		  addRow(spreadsheet, factory.getColumns(), seriesValues);
		}
	      }
	    } else {
	      addRow(spreadsheet, factory.getColumns(), studyValues);
	    }
	  }
	} else {
	  addRow(spreadsheet, factory.getColumns(), patientValues);
	}
      }
    }
    spreadsheet.flush();
  }

  private static void showUsage(final Options opts) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("DicomSummarize [OPTIONS] [DICOM-FILES]", opts);
  }
  

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    final Options options = new Options();
    options.addOption("h", "help", false, "show this information");
    
    final Option configXMLOpt = new Option("c", "config", true, "configuration XML file");
    configXMLOpt.setRequired(true);
    options.addOption(configXMLOpt);
    
    final Option valuesOpt = new Option("v", "values", true,
	"CSV summary spreadsheet file (defaults to standard output)");
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

    final Collection<File> files = new LinkedList<File>();
    for (final String path : cli.getArgs()) {
      files.add(new File(path));
    }

    final OutputStream out;
    if (cli.hasOption(valuesOpt.getOpt())) {
      out = new BufferedOutputStream(new FileOutputStream(cli.getOptionValue(valuesOpt.getOpt())));
    } else {
      out = System.out;
    }

    final File configFile = cli.hasOption(configXMLOpt.getOpt()) ? new File(cli.getOptionValue(configXMLOpt.getOpt())) : null;
    final CSVSummaryBuilder builder = new CSVSummaryBuilder(configFile);
    builder.run(new CSVPrinter(out),
        new StreamProgressMonitor(System.err, "Reading", "DICOM files"),
        files.toArray(new File[0]));
  }
}
