/**
 * Copyright (c) 2012 Washington University
 */
package org.nrg.dcm.browse;

import java.awt.Component;
import java.util.Map;

import javax.swing.JOptionPane;

import org.nrg.dcm.io.BatchExporter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

final class ExportFailureHandler implements Runnable {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private final Component parent;
    private final BatchExporter exporter;

    ExportFailureHandler(final BatchExporter exporter, final Component frame) {
        this.exporter = exporter;
        this.parent = frame;
    }

    public void run() {
        exporter.run();
        final Map<?,Throwable> failures = exporter.getFailures();
        if (!failures.isEmpty()) {  // TODO: localize
            final StringBuilder message = new StringBuilder("Error: ");
            message.append(failures.size());
            message.append(" object");
            if (failures.size() > 1) {
                message.append("s");
            }
            message.append(" could not be exported").append(LINE_SEPARATOR);
            final ListMultimap<Class<?>, Throwable> throwables = ArrayListMultimap.create();
            for (final Throwable t : failures.values()) {
                throwables.put(t.getClass(), t);
            }
            final boolean multipleCauses = throwables.keySet().size() > 1;
            for (final Class<?> clazz : throwables.keySet()) {
                if (multipleCauses) {
                    message.append(throwables.get(clazz).size());
                    message.append(" objects(s): ");
                }
                message.append(clazz.getSimpleName()).append(": ");
                message.append(throwables.get(clazz).get(0).getMessage());
            }
            JOptionPane.showMessageDialog(parent, message,
                    "Export failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }        
}