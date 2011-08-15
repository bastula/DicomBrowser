/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.dcm.edit;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public final class VariableAssignmentManager {
    public final static GridBagConstraints labelConstraint = new GridBagConstraints();
    public final static GridBagConstraints valueConstraint = new GridBagConstraints();
    static {
        labelConstraint.gridx = 0;
        labelConstraint.weightx = 0.2;

        valueConstraint.gridx = 1;
        valueConstraint.fill = GridBagConstraints.HORIZONTAL;
        valueConstraint.weightx = 0.8;
    }

    private final Container container;
    private final Map<Variable,VariableRow> variables;
    private final Multimap<Integer,String> values;
    private int visibleRows = 0;

    public VariableAssignmentManager(final Container container,
            final Collection<?> variables, final Multimap<Integer,String> values) {
        this.container = container;
        this.values = values;
        this.variables = Maps.newLinkedHashMap();
        for (final Object o : variables) {
            final Variable v = (Variable)o;
            this.variables.put(v, new VariableRow(v));
        }
    }

    public int getVisibleRows() { return visibleRows; }

    private synchronized void propagate(final Variable root) {
        final Iterator<Map.Entry<Variable,VariableRow>> mei = variables.entrySet().iterator();
        boolean update = false;
        while (mei.hasNext()) {
            final Map.Entry<Variable,VariableRow> me = mei.next();
            if (update) {
                me.getValue().updateDisplay();
            } else if (root == me.getKey()) {
                update = true;
            }
        }
    }

    private final class VariableRow implements DocumentListener {
        private static final int N_READ_TRIES = 16;
        private final Logger logger = LoggerFactory.getLogger(VariableRow.class);
        private final Variable v;
        private final JTextField tf;
        private boolean isPropagating = false;

        VariableRow(final Variable v) {
            this.v = v;
            if (v.isHidden()) {
                tf = new JTextField();
            } else {
                visibleRows++;
                final String desc = v.getDescription();
                final JLabel label = new JLabel(Strings.isNullOrEmpty(desc) ? v.getName() : desc);
                container.add(label, labelConstraint);
                tf = new JTextField(getDisplayText());
                tf.getDocument().addDocumentListener(this);
                container.add(tf, valueConstraint);
            }
        }

        public String getDisplayText() {
            final String text = getText();
            return null == text ? "" : text;
        }

        public String getText() {
            String text = v.getValue();
            if (null == text) {
                final Value vi = v.getInitialValue();
                if (null == vi) {
                    return null;
                } else try {
                    return vi.on(Maps.transformValues(values.asMap(),
                            new Function<Collection<String>,String>() {
                        public String apply(final Collection<String> vals) {
                            return Joiner.on(',').join(vals);
                        }
                    }));
                } catch (ScriptEvaluationException e) {
                    logger.warn("unable to evaluate initial value for " + v, e);
                    return null;
                }
            } else {
                return text;
            }
        }

        public void changedUpdate(final DocumentEvent ev) {
            // Only set the variable value if this is a "real" text change,
            // i.e., one that the user entered rather than a change inherited
            // from the initial value.
            if (!isPropagating) {
                final Document d = ev.getDocument();
                for (int i = 0; i < N_READ_TRIES; i++) {
                    try {
                        v.setValue(d.getText(0, d.getLength()));
                        propagate(v);
                        return;
                    } catch (BadLocationException ignore) {}
                }
                logger.error("Unable to read value from {}", d);
            }
        }

        public void insertUpdate(final DocumentEvent e) {
            changedUpdate(e);
        }

        public void removeUpdate(final DocumentEvent e) {
            changedUpdate(e);
        }

        public void updateDisplay() {
            assert isPropagating == false;
            isPropagating = true;

            final String text = getDisplayText();
            if (!Objects.equal(text, tf.getText())) {
                tf.setText(text);
            }

            assert isPropagating == true;
            isPropagating = false;
        }
    }
}
