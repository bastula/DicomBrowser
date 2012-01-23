/**
 * Copyright (c) 2006-2009,2012 Washington University
 */
package org.nrg.dcm.browse;

import java.util.Stack;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JDialog;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.JComboBox;
import javax.swing.JButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;


/**
 * Dialog box for entering destination information to send DICOM files.
 * @author Kevin A. Archie <karchie@wustl.edu>
 */
final class CStoreDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;

    private static final String TITLE = "Send files to DICOM receiver";  // TODO: localize
    private static final String SEND_BUTTON = "Send";            // TODO: localize
    private static final String CANCEL_BUTTON = "Cancel";        // TODO: localize
    private static final String[] whichOpts = {"Send all files", "Send only selected files"};     // TODO: localize

    private static final String DEFAULT_AE_TITLE = "NRG-C-STORE-SCU";
    private static final String AE_HISTORY = "AE-history";  //  @jve:decl-index=0:

    private static final int ALL_FILES_INDEX = 0;

    private final Logger logger = LoggerFactory.getLogger(CStoreDialog.class);

    private JPanel jContentPane = null;

    private JLabel hostLabel = null;

    private JLabel localAELabel = null;

    private JLabel remoteAELabel = null;

    private JComboBox whichFiles = null;

    private JButton sendButton = null;

    private JButton cancelButton = null;

    private JComboBox hostComboBox = null;

    private JComboBox remoteAEComboBox = null;

    private JComboBox localAEComboBox = null;

    private FileSetTableModel model;

    private JLabel portLabel = null;

    private JComboBox portComboBox = null;

    private JLabel tlsLabel = null;

    private JCheckBox useTLSCheckBox = null;


    private static final class AEAddr {
        private static final Pattern OLD_ADDR_PATTERN = Pattern.compile("([^\\:]+)\\:([^\\:]+)\\:([^\\:]+)");
        private static final Pattern ADDR_PATTERN = Pattern.compile("([^#]+)#([^\\:]+)\\:([^\\:]+)\\:([^\\:]+)");

        private final String host;
        private final int port;
        private final String remoteAE;
        private final String localAE;
        private String string = null;

        public AEAddr(final String host, final int port, final String remoteAE, final String localAE) {
            this.host = host;
            this.port = port;
            this.remoteAE = remoteAE;
            this.localAE = localAE;
        }

        public AEAddr(final String desc) {
            final Matcher matcher = ADDR_PATTERN.matcher(desc);
            if (matcher.matches()) {
                this.localAE = matcher.group(1);
                this.host = matcher.group(2);
                this.port = Integer.parseInt(matcher.group(3));
                this.remoteAE = matcher.group(4);
            } else {
                final Matcher oldMatcher = OLD_ADDR_PATTERN.matcher(desc);
                if (oldMatcher.matches()) {
                    this.host = oldMatcher.group(1);
                    this.port = Integer.parseInt(oldMatcher.group(2));
                    this.remoteAE = oldMatcher.group(3);
                    this.localAE = DEFAULT_AE_TITLE;
                } else {
                    throw new IllegalArgumentException("Uninterpretable AE address entry " + desc);
                }
            }
        }

        @Override
        public String toString() {
            if (null == string) {
                final StringBuilder sb = new StringBuilder(localAE);
                sb.append("#").append(host);
                sb.append(":").append(port);
                sb.append(":").append(remoteAE);
                string = sb.toString();
            }
            return string;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(localAE, host, port, remoteAE);
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof AEAddr && this.toString().equals(o.toString());
        }
    }

    private static final class AEHistory extends Stack<AEAddr> {
        private static final long serialVersionUID = 1L;
        private static final String SEPARATOR = ",";

        AEHistory(final String hs) {
            super();
            if (null != hs) {
                final String[] h = hs.split(SEPARATOR);
                for (int i = h.length - 1; i >= 0; i--) {
                    try {
                        push(new AEAddr(h[i]));
                    } catch (IllegalArgumentException e) {
                        LoggerFactory.getLogger(AEHistory.class)
                        .warn("invalid AE history entry {}, skipping", h[i]);
                    }
                }
            }
        }

        AEAddr matchHost(final String host) {
            for (final AEAddr a : this) {
                if (a.host.equals(host)) {
                    return a;
                }
            }
            return null;
        }

        Vector<String> getHosts() {
            final Vector<String> hosts = new Vector<String>(size());
            for (final AEAddr ae : this) {
                hosts.add(ae.host);
            }
            return hosts;
        }

        Vector<String> getPorts() {
            final Vector<String> ports = new Vector<String>(size());
            for (final AEAddr ae : this) {
                ports.add(String.valueOf(ae.port));
            }
            return ports;
        }

        Vector<String> getRemoteTitles() {
            final Vector<String> aets = new Vector<String>(size());
            for (final AEAddr ae : this) {
                aets.add(ae.remoteAE);
            }
            return aets;
        }

        Vector<String> getLocalTitles() {
            final Vector<String> aets = new Vector<String>(size());
            for (final AEAddr a : this) {
                aets.add(a.localAE);
            }
            return aets;
        }

        @Override
        public AEAddr push(final AEAddr item) {
            while (this.contains(item)) {
                this.remove(item);
            }
            return super.push(item);
        }

        @Override
        public String toString() {
            return Joiner.on(SEPARATOR).join(this);
         }
    }

    private final Preferences prefs;
    private final AEHistory history;

    public CStoreDialog(final DicomBrowser browser, final FileSetTableModel model) {
        super(browser.getFrame());
        this.model = model;
        this.prefs = browser.getPrefs();

        logger.trace("loading SCP AE history from preferences");
        history = new AEHistory(prefs.get(AE_HISTORY, null));
        logger.trace("setting location");
        setLocationRelativeTo(browser);
        logger.trace("initializing");
        initialize();
        logger.trace("ready");
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setTitle(TITLE);
        this.setContentPane(getJContentPane());
        this.pack();
        this.setResizable(false);
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            portLabel = new JLabel();
            portLabel.setText("Port:");
            portLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            portLabel.setHorizontalTextPosition(SwingConstants.RIGHT);

            localAELabel = new JLabel();
            localAELabel.setText("Local AE title:");
            localAELabel.setHorizontalAlignment(SwingConstants.RIGHT);
            localAELabel.setHorizontalTextPosition(SwingConstants.RIGHT);

            remoteAELabel = new JLabel();
            remoteAELabel.setText("Remote AE title:");
            remoteAELabel.setHorizontalAlignment(SwingConstants.RIGHT);
            remoteAELabel.setHorizontalTextPosition(SwingConstants.RIGHT);

            hostLabel = new JLabel();
            hostLabel.setText("Remote host:");
            hostLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
            hostLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            tlsLabel = new JLabel();
            tlsLabel.setText("Use secure connection (TLS):");
            tlsLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
            tlsLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            jContentPane = new JPanel(new GridBagLayout());

            final Insets labelInsets = new Insets(4, 8, 0, 0);

            // Host label
            final GridBagConstraints hostLabelC9s = new GridBagConstraints();
            hostLabelC9s.gridy = 0;
            hostLabelC9s.gridx = 0;
            hostLabelC9s.anchor = GridBagConstraints.LINE_END;
            hostLabelC9s.insets = labelInsets;
            jContentPane.add(hostLabel, hostLabelC9s);

            // Host combo box
            final GridBagConstraints hostC9s = new GridBagConstraints();
            hostC9s.gridx = 1;
            hostC9s.gridy = 0;
            hostC9s.weightx = 1.0;
            hostC9s.anchor = GridBagConstraints.LINE_START;
            jContentPane.add(getHostComboBox(), hostC9s);

            // "Port:" label
            final GridBagConstraints portLabelC9s = new GridBagConstraints();
            portLabelC9s.gridy = 1;
            portLabelC9s.gridx = 0;
            portLabelC9s.insets = labelInsets;
            portLabelC9s.anchor = GridBagConstraints.LINE_END;
            jContentPane.add(portLabel, portLabelC9s);

            // Port combo box
            final GridBagConstraints portC9s = new GridBagConstraints();
            portC9s.gridy = 1;
            portC9s.gridx = 1;
            //	    portC9s.ipadx = 20;
            //	    portC9s.ipady = -1;
            portC9s.weightx = 1.0;
            //	    portC9s.insets = new Insets(8, 9, 7, 6);
            portC9s.anchor = GridBagConstraints.LINE_START;
            jContentPane.add(getPortComboBox(), portC9s);

            // Local AE title label
            final GridBagConstraints localAELabelC9s = new GridBagConstraints();
            localAELabelC9s.gridy = 3;
            localAELabelC9s.gridx = 0;
            localAELabelC9s.insets = labelInsets;
            localAELabelC9s.anchor = GridBagConstraints.LINE_END;
            jContentPane.add(localAELabel, localAELabelC9s);

            final GridBagConstraints localAEC9s = new GridBagConstraints();
            localAEC9s.gridy = 3;
            localAEC9s.gridx = 1;
            localAEC9s.weightx = 1;
            localAEC9s.anchor = GridBagConstraints.LINE_START;
            jContentPane.add(getLocalAEComboBox(), localAEC9s);

            final GridBagConstraints remoteAELabelC9s = new GridBagConstraints();
            remoteAELabelC9s.gridy = 2;
            remoteAELabelC9s.gridx = 0;
            remoteAELabelC9s.anchor = GridBagConstraints.LINE_END;
            remoteAELabelC9s.insets = labelInsets;
            jContentPane.add(remoteAELabel, remoteAELabelC9s);

            final GridBagConstraints remoteAEC9s = new GridBagConstraints();
            remoteAEC9s.gridy = 2;
            remoteAEC9s.gridx = 1;
            remoteAEC9s.weightx = 1;
            remoteAEC9s.anchor = GridBagConstraints.LINE_START;
            jContentPane.add(getRemoteAEComboBox(), remoteAEC9s);

            // Send which files selector
            final GridBagConstraints whichFilesC9s = new GridBagConstraints();
            whichFilesC9s.gridwidth = 2;
            whichFilesC9s.gridx = 0;
            whichFilesC9s.gridy = 4;
            whichFilesC9s.weightx = 1.0;
            jContentPane.add(getWhichFiles(), whichFilesC9s);

            final GridBagConstraints tlsC9s = new GridBagConstraints();
            tlsC9s.gridx = 0;
            tlsC9s.gridy = 5;
            tlsC9s.gridwidth = 2;

            final JPanel tlsPanel = new JPanel();
            tlsPanel.add(tlsLabel);
            tlsPanel.add(getUseTLSCheckBox());
            jContentPane.add(tlsPanel, tlsC9s);

            // Send button
            GridBagConstraints sendC9s = new GridBagConstraints();
            sendC9s.gridy = 6;
            sendC9s.gridx = 0;
            jContentPane.add(getSendButton(), sendC9s);


            // Cancel button
            GridBagConstraints cancelC9s = new GridBagConstraints();
            cancelC9s.gridy = 6;
            cancelC9s.gridx = 1;
            jContentPane.add(getCancelButton(), cancelC9s);
        }
        return jContentPane;
    }

    private JComboBox getWhichFiles() {
        if (whichFiles == null) {
            whichFiles = new JComboBox(whichOpts);    // this comes out too small on Windows
            whichFiles.setMinimumSize(new Dimension(120,0));
        }
        return whichFiles;
    }

     private JButton getSendButton() {
        if (sendButton == null) {
            sendButton = new JButton(SEND_BUTTON);
            sendButton.addActionListener(this);
        }
        return sendButton;
    }

    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton(CANCEL_BUTTON);
            cancelButton.addActionListener(this);
        }
        return cancelButton;
    }

     private JComboBox getHostComboBox() {
        if (hostComboBox == null) {
            hostComboBox = new JComboBox(history.getHosts());
            hostComboBox.setEditable(true);
            hostComboBox.addActionListener(this);
            hostComboBox.setMinimumSize(new Dimension(120,0));
        }
        return hostComboBox;
    }

    private JComboBox getRemoteAEComboBox() {
        if (remoteAEComboBox == null) {
            remoteAEComboBox = new JComboBox(history.getRemoteTitles());
            remoteAEComboBox.setEditable(true);
            remoteAEComboBox.setMinimumSize(new Dimension(120,0));
        }
        return remoteAEComboBox;
    }

    private JComboBox getLocalAEComboBox() {
        if (null == localAEComboBox) {
            localAEComboBox = new JComboBox(history.getLocalTitles());
            localAEComboBox.setEditable(true);
            localAEComboBox.setMinimumSize(new Dimension(120, 0));
        }
        return localAEComboBox;
    }

    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();
        if (source == hostComboBox) {
            final AEAddr a = history.matchHost((String)hostComboBox.getSelectedItem());
            if (a != null) {
                portComboBox.setSelectedItem(String.valueOf(a.port));
                remoteAEComboBox.setSelectedItem(a.remoteAE);
                localAEComboBox.setSelectedItem(a.localAE);
            }
        } else {
            final String command = e.getActionCommand();
            if (command.equals(SEND_BUTTON)) {
                final int port;
                try {
                    port = Integer.parseInt((String)portComboBox.getSelectedItem());
                    history.push(new AEAddr((String)hostComboBox.getSelectedItem(),
                            port,
                            (String)remoteAEComboBox.getSelectedItem(),
                            (String)localAEComboBox.getSelectedItem()));
                    prefs.put(AE_HISTORY, history.toString());
                    setVisible(false);
                    doSend();
                } catch (NumberFormatException e1) {
                    JOptionPane.showMessageDialog(this,
                            String.format("Invalid port number: %s", portComboBox.getSelectedItem()),
                            "Invalid port",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else if (command.equals(CANCEL_BUTTON)) {
                setVisible(false);        // never mind
            } else {
                throw new UnsupportedOperationException("Unimplemented action: " + command);
            }
        }
    }

    private void doSend() { 
        final InterfaceCounter wc = InterfaceCounter.getInstance();
        wc.register(this);	// don't exit program until we're done with this send
        model.send((String)hostComboBox.getSelectedItem(),
                (String)portComboBox.getSelectedItem(),
                (String)remoteAEComboBox.getSelectedItem(),
                useTLSCheckBox.isSelected(),
                (String)localAEComboBox.getSelectedItem(),
                whichFiles.getSelectedIndex() == ALL_FILES_INDEX);
        wc.unregister(this, true);
    }

    private JComboBox getPortComboBox() {
        if (portComboBox == null) {
            portComboBox = new JComboBox(history.getPorts());
            portComboBox.setMinimumSize(new Dimension(48,0));
            portComboBox.setEditable(true);
        }
        return portComboBox;
    }

    private JCheckBox getUseTLSCheckBox() {
        if (null == useTLSCheckBox) {
            useTLSCheckBox = new JCheckBox();
        }
        return useTLSCheckBox;
    }
}