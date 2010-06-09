/**
 * Copyright (c) 2009,2010 Washington University
 */
package org.nrg.dcm.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public final class NewRootFileExporter extends AbstractFileExporter implements
	DicomObjectExporter {
    private static final char PATH_SEPARATOR_CHAR = '/';
    private static final String PATH_SEPARATOR = "/";
    
    private final Logger logger = LoggerFactory.getLogger(NewRootFileExporter.class);
    private final Map<String[],File> newRoots;

    /**
     * @param outRoot New root directory under which all remapped files will be placed
     * @param roots Existing root directories in which source files are found
     * @throws IOException If a root directory (existing or new) cannot be converted to canonical form
     */
    public NewRootFileExporter(final String aeTitle, final File outRoot, final Collection<File> roots)
    throws IOException {
	super(aeTitle);
	final String canonicalRootPath = outRoot.getCanonicalPath();
	final File canonicalRoot = new File(canonicalRootPath);

	// Arrange the roots in order of decreasing length, so that more specific matches
	// are found before less specific matches.
	final List<String[]> lengthSortedPaths = new ArrayList<String[]>(roots.size());
	final Map<String[],File> pathToRoots = new HashMap<String[],File>();
	for (final File root : roots) {
	    if (root.exists()) {
		if (!root.isDirectory()) {
		    throw new IllegalArgumentException("root " + root + " is not a directory");
		}
		final String[] path = explodePath(root.getCanonicalPath());
		lengthSortedPaths.add(path);
		pathToRoots.put(path, root);
	    }
	}
	Collections.sort(lengthSortedPaths, new Comparator<String[]>() {
	    public int compare(String[] o1, String[] o2) { return o2.length - o1.length; } // sorts in descending length order
	});

	// Verify that the proposed new root doesn't fall under one of the input roots.
	final String[] explodedCanonicalRoot = explodePath(canonicalRootPath);
	for (final Iterator<String[]> pathi = lengthSortedPaths.iterator(); pathi.hasNext(); ) {
	    final String[] path = pathi.next();
	    if (null != relativePath(explodedCanonicalRoot, path)) {
		pathi.remove();
		logger.warn("Proposed root {} is under existing root, skipping", path);
	    }
	}

	// Construct the new root paths
	newRoots = new LinkedHashMap<String[],File>();
	for (final Map.Entry<String[],String> e : buildMinimalUniqueNames(lengthSortedPaths).entrySet()) {
	    newRoots.put(e.getKey(), new File(canonicalRoot, e.getValue()));
	}
    }

    private final String LINE_SEPARATOR = System.getProperty("line.separator");

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
	final StringBuilder sb = new StringBuilder(super.toString());
	sb.append(":").append(LINE_SEPARATOR);
	for (final Map.Entry<String[],File> e : newRoots.entrySet()) {
	    join(sb, e.getKey(), PATH_SEPARATOR);
	    sb.append(" -> ");
	    sb.append(e.getValue());
	    sb.append(LINE_SEPARATOR);
	}
	return sb.toString();
    }

    private static StringBuilder join(final StringBuilder sb,
	    final String[] parts, final String separator,
	    final int offset, final int n) {
	sb.append(parts[offset]);
	for (int i = 1; i < n; i++) {
	    sb.append(separator);
	    sb.append(parts[offset + i]);
	}
	return sb;
    }

    private static StringBuilder join(final StringBuilder sb,
	    final String[] parts, final String separator) {
	return join(sb, parts, separator, 0, parts.length);
    }

    private static String join(final String[] parts, final String separator, final int offset) {
	return join(new StringBuilder(), parts, separator, offset, parts.length - offset).toString();
    }

   private static String[] explodePath(final String path) {
	return path.replace(File.separatorChar, PATH_SEPARATOR_CHAR).split(PATH_SEPARATOR);
    }

    private static String relativePath(final String[] path, final String[] root) {
	if (path.length < root.length) {
	    return null;
	}
	for (int i = 0; i < root.length; i++) {
	    if (!path[i].equals(root[i])) {
		return null;
	    }
	}
	return join(path, PATH_SEPARATOR, root.length);
    }

    private static String relativePath(final String path, final String[] root) {
	return relativePath(explodePath(path), root);
    }

    private static Map<String[],String> buildMinimalUniqueNames(final Collection<String[]> paths) {
	final Map<String[],String> names = new LinkedHashMap<String[],String>();
	for (final String[] path : paths) {
	    assert path.length > 0;
	    final String base = path[path.length - 1];
	    String name = base;
	    for (int i = 1; names.containsValue(name); i++) {
		name = base + "_" + i;
	    }
	    names.put(path, name);
	}

	assert names.size() == new HashSet<String>(names.values()).size();

	return names;
    }

    public File map(final File source) {
	String path;
	try {
	    path = source.getCanonicalPath();
	} catch (IOException e) {
	    path = source.getAbsolutePath();
	}
	for (final Map.Entry<String[],File> e : newRoots.entrySet()) {
	    final String[] rootPath = e.getKey();
	    final String relativePath = relativePath(path, rootPath);
	    if (null != relativePath) {
		return new File(e.getValue(), relativePath);
	    }
	}
	return null;
    }
}
