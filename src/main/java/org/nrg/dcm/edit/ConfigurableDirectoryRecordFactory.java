/**
 * Copyright (c) 2008-2012 Washington University
 */
package org.nrg.dcm.edit;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dcm4che2.data.Tag;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.nrg.dcm.DirectoryRecord;
import org.nrg.dcm.DirectoryRecord.AbstractFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 */
public final class ConfigurableDirectoryRecordFactory extends AbstractFactory {
    private static final String ROOT_ELEM_NAME = "Columns";
    private static final String GLOBAL_ELEM_NAME = "Global";
    private static final String PATIENT_ELEM_NAME = "Patient";
    private static final String STUDY_ELEM_NAME = "Study";
    private static final String SERIES_ELEM_NAME = "Series";
    private static final String INSTANCE_ELEM_NAME = "Instance";
    private static final String[] LEVEL_NAMES = {GLOBAL_ELEM_NAME, PATIENT_ELEM_NAME, STUDY_ELEM_NAME, SERIES_ELEM_NAME, INSTANCE_ELEM_NAME};
    private static final String SUBST_ATTR_NAME = "remap";
    private static final String APPEND_COL_ATTR_NAME = "append";
    private static final Pattern tagPattern = Pattern.compile("\\A\\(([\\da-fA-F]{1,4}),([\\da-fA-F]{1,4})\\)\\z");
    private static final Map<String,Integer[]> selectionTags = ImmutableMap.of(GLOBAL_ELEM_NAME, new Integer[0],
            PATIENT_ELEM_NAME, new Integer[]{Tag.PatientName,Tag.PatientID},
            STUDY_ELEM_NAME, new Integer[]{Tag.StudyInstanceUID},
            SERIES_ELEM_NAME, new Integer[]{Tag.SeriesInstanceUID},
            INSTANCE_ELEM_NAME, new Integer[]{Tag.SOPInstanceUID});

    private final List<DicomTableEntry> columns;
    private final Map<String,Collection<Integer>> levelTags;

    public ConfigurableDirectoryRecordFactory(File configFile)
    throws IOException,ParseException,DocumentException {
        // Must iterate in increasing order of specificity (Global, then Patient, ..., and Instance last)
        levelTags = Maps.newLinkedHashMap();
        for (final String level : LEVEL_NAMES) {
            levelTags.put(level, new TreeSet<Integer>());
        }

        if (null == configFile) {
            columns = Collections.emptyList();
            return;
        }

        // Read the configuration file into DOM
        final Element rootElement = parseConfigFile(configFile).getRootElement();
        if (null == rootElement)
            throw new ParseException("no root element found", 0);
        if (!ROOT_ELEM_NAME.equals(rootElement.getName())) {
            throw new ParseException("invalid root element (" + rootElement.getName()
                    + "), must be <" + ROOT_ELEM_NAME + ">", 0);
        }

        final Collection<DicomTableEntry> bodyCols = Lists.newArrayList();
        final Collection<DicomTableEntry> appendedCols = Lists.newArrayList();

        // Extract the spreadsheet column specification from the DOM
        for (final Object eo : rootElement.elements()) {
            assert eo instanceof Element;
            final Element e = (Element)eo;
            final String level = e.getName();
            final Matcher m = tagPattern.matcher(e.getTextTrim());
            if (!m.matches()) {
                throw new ParseException("invalid DICOM tag " + e.getText(), 0);
            }
            assert 2 == m.groupCount() : "unexpected tag matching group count " + m.groupCount();
            final int group = Integer.parseInt(m.group(1), 16);
            final int element = Integer.parseInt(m.group(2), 16);
            final int tag = (group << 16) | element;

            final Attribute isSubst = e.attribute(SUBST_ATTR_NAME);
            final String substName = (null == isSubst) ? null : isSubst.getValue();
            bodyCols.add(new DicomTableEntry(tag, level));
            if (null != substName && !"".equals(substName)) {
                (attrIsTrue(e, APPEND_COL_ATTR_NAME) ? appendedCols : bodyCols).add(new DicomTableEntry(tag, level, true, substName));
            }
            if (!levelTags.containsKey(level))
                throw new ParseException("invalid record level " + level + "; must be one of " + Arrays.toString(LEVEL_NAMES), 0);
            levelTags.get(level).add(tag);
        }

        // For each level required, ensure that the selection keys are included in the spreadsheet.
        final Collection<DicomTableEntry> selectionCols = Sets.newLinkedHashSet();
        for (final String level : levelTags.keySet()) {
            if (definesRows(level)) {
                for (final int tag : selectionTags.get(level)) {
                    final DicomTableEntry entry = new DicomTableEntry(tag, level);
                    if (!bodyCols.contains(entry)) {
                        selectionCols.add(entry);
                        levelTags.get(level).add(tag);
                    }
                }
            }
        }

        // Build the column set.
        columns = Lists.newArrayList(selectionCols);
        columns.addAll(bodyCols);
        columns.addAll(appendedCols);

        // Configure factory with suitable templates for each level.
        final Collection<Integer> patientTags = Sets.newLinkedHashSet(levelTags.get(PATIENT_ELEM_NAME));
        patientTags.addAll(levelTags.get(GLOBAL_ELEM_NAME));
        addType(DirectoryRecord.Type.PATIENT,
                new DirectoryRecord.Template.Extended(DirectoryRecord.Template.PATIENT, patientTags));
        addType(DirectoryRecord.Type.STUDY,
                new DirectoryRecord.Template.Extended(DirectoryRecord.Template.STUDY, levelTags.get(STUDY_ELEM_NAME)));
        addType(DirectoryRecord.Type.SERIES,
                new DirectoryRecord.Template.Extended(DirectoryRecord.Template.SERIES, levelTags.get(SERIES_ELEM_NAME)));
        addType(DirectoryRecord.Type.INSTANCE,
                new DirectoryRecord.Template.Extended(DirectoryRecord.Template.INSTANCE, levelTags.get(INSTANCE_ELEM_NAME)));
    }

    public boolean isNotEmpty(final Attribute a) {
        return null != a && !Strings.isNullOrEmpty(a.getValue());
    }

    private static final Set<String> XS_BOOL_TRUE_VALUES = ImmutableSet.of("true", "1");

    public boolean attrIsTrue(final Element e, final String name) {
        final Attribute a = e.attribute(name);
        if (null == a) return false;
        final String v = a.getValue();
        return null != v && XS_BOOL_TRUE_VALUES.contains(v);
    }

    public Collection<DicomTableEntry> getColumns() { return Lists.newArrayList(columns); }

    /**
     * Will this factory make at least one row in the spreadsheet for each entity at the indicated level?
     * @param level
     * @return
     */
    private boolean definesRows(final String level) {
        assert levelTags.containsKey(level);
        boolean foundLevel = false;
        for (final Iterator<Map.Entry<String,Collection<Integer>>> i = levelTags.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry<String,Collection<Integer>> e = i.next();
            if (e.getKey().equals(level))
                foundLevel = true;
            if (true == foundLevel && e.getValue().size() > 0)
                return true;
        }
        return false;
    }

    public boolean definesInstanceRows() { return definesRows(INSTANCE_ELEM_NAME); }
    public boolean definesSeriesRows() { return definesRows(SERIES_ELEM_NAME); }
    public boolean definesStudyRows() { return definesRows(STUDY_ELEM_NAME); }
    public boolean definesPatientRows() { return definesRows(PATIENT_ELEM_NAME); }

    public Collection<String> getLevels() {
        return Lists.newArrayList(selectionTags.keySet());
    }

    public Collection<Integer> getSelectionTags(final String level) {
        return selectionTags.containsKey(level) ? Arrays.asList(selectionTags.get(level)) : null;
    }

    private static Document parseConfigFile(final File configFile) throws IOException,DocumentException {
        final SAXReader reader = new SAXReader(false);
        return reader.read(configFile);
    }
}
