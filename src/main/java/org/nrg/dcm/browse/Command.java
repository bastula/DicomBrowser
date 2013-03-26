/**
 * Copyright (c) 2006-2011 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.nrg.dcm.edit.Operation;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * Represents a single editor command.
 * This may consist of multiple Operations on multiple Files,
 * since more than one attribute and more than one File may be
 * selected when the command is issued.
 * The collection of Operations that this Command replaces must
 * specify the Files on which each replaced Operation acted,
 * since those Operations may have been assigned under a different
 * selection.
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
final class Command {
    private final Operation ops[];
    private final File[][] files;
    private final Map<Integer,Map<File,Operation>> replaced;

    private final String MULT_OP_FORMAT = " (%1$d attributes)";   // TODO: localize

    // One operation on a set of files
    Command(Operation op, Collection<File> files, Map<File,Operation> replaced, Set<Integer> affectedTags) {
        this.ops = new Operation[]{op};
        this.files = new File[1][];
        this.files[0] = files.toArray(new File[0]);
        this.replaced = Maps.newHashMap();
        for (final int tag : affectedTags) {
            this.replaced.put(tag, Maps.newHashMap(replaced));
        }
    }

    // Multiple operations on the same set of files
    Command(Operation[] ops, Collection<File> files, Map<Integer,Map<File,Operation>> replaced) {
        this.ops = new Operation[ops.length];
        System.arraycopy(ops, 0, this.ops, 0, ops.length);
        this.files = new File[ops.length][];
        final File[] filesArray = files.toArray(new File[0]);
        for (int i = 0; i < ops.length; i++) {
            this.files[i] = filesArray;
        }
        this.replaced = Maps.newHashMap(replaced);
    }

    // Multiple operations, each on its own file set
    Command(SetMultimap<Operation,File> ops, Map<Integer,Map<File,Operation>> replaced) {
        this.ops = ops.keySet().toArray(new Operation[0]);
        this.files = new File[this.ops.length][];
        for (int i = 0; i < this.ops.length; i++) {
            this.files[i] = ops.get(this.ops[i]).toArray(new File[0]);
        }
        this.replaced = Maps.newHashMap(replaced);
    }

    @Override
    public String toString() {
        final SetMultimap<String,String> opdescs = LinkedHashMultimap.create();
        for (final Operation op : ops) {
            opdescs.put(op.getName(), op.toString());
        }
        return Joiner.on(", ").join(Iterables.transform(opdescs.keySet(),
                new Function<String,String>() {
            public String apply(final String name) {
                final StringBuilder sb = new StringBuilder();
                assert opdescs.containsKey(name);
                final int count = opdescs.get(name).size();
                assert count > 0;
                if (1 == count) {
                    sb.append(opdescs.get(name).toArray()[0]);
                } else {
                    sb.append(name);
                    sb.append(String.format(MULT_OP_FORMAT, count));
                }
                return sb.toString();
            }
        }));
    }

    Operation[] getOperations() {
        final Operation[] copy = new Operation[ops.length];
        System.arraycopy(ops, 0, copy, 0, ops.length);
        return copy;
    }

    File[] getFiles(final int opIndex) {
        final File[] copy = new File[files[opIndex].length];
        System.arraycopy(files[opIndex], 0, copy, 0, files[opIndex].length);
        return copy;
    }

    Collection<File> getAllFiles() {
        final Set<File> outfiles = Sets.newHashSet();
        for (int i = 0; i < files.length; i++) {
            outfiles.addAll(Arrays.asList(files[i]));
        }
        return outfiles;
    }


    /**
     * @return a reference to the Command's own instance (not a copy)
     */
    Map<Integer,Map<File,Operation>> getReplaced() {
        return replaced;
    }
}
