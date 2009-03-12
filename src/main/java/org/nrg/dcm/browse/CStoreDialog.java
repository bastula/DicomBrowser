/**
 * Copyright (c) 2006-2009 Washington University
 */
package org.nrg.dcm.browse;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Stack;

import javax.swing.JPanel;
import javax.swing.JDialog;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;

import javax.swing.JCheckBox;
import javax.swing.SwingConstants;
import javax.swing.JComboBox;
import javax.swing.JButton;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;


/**
 * Dialog box for entering destination information to send DICOM files.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
final class CStoreDialog extends JDialog implements ActionListener {
  private static final long serialVersionUID = 1L;

  private final static String TITLE = "Send files to DICOM receiver";  // TODO: localize
  private final static String SEND_BUTTON = "Send";            // TODO: localize
  private final static String CANCEL_BUTTON = "Cancel";        // TODO: localize
  private final static String[] whichOpts = {"Send all files", "Send only selected files"};     // TODO: localize

  private final static String AE_HISTORY = "AE-history";  //  @jve:decl-index=0:

  private final static int ALL_FILES_INDEX = 0;

  private JPanel jContentPane = null;

  private JLabel hostLabel = null;

  private JLabel aeLabel = null;

  private JComboBox whichFiles = null;

  private JButton sendButton = null;

  private JButton cancelButton = null;

  private JComboBox hostComboBox = null;

  private JComboBox aeComboBox = null;

  private FileSetTableModel model;

  private JLabel portLabel = null;

  private JComboBox portComboBox = null;

  private JLabel tlsLabel = null;

  private JCheckBox useTLSCheckBox = null;


  private static final class AEAddr {
    final String host;
    final int port;
    final String title;

    public AEAddr(final String host, final int port, final String title) {
      this.host = host;
      this.port = port;
      this.title = title;
    }

    public AEAddr(final String desc) {
      final String[] broken = desc.split(":");
      this.host = broken[0];
      this.port = Integer.parseInt(broken[1]);
      this.title = broken[2];
    }

    @Override
    public String toString() {
      return String.format("%1$s:%2$d:%3$s", host, port, title);
    }

    @Override
    public int hashCode() {
      return 17 + host.hashCode() + port + title.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof AEAddr)) return false;
      final AEAddr a = (AEAddr)o;
      return host.equals(a.host) && port == a.port && title.equals(a.title);
    }
  }

  private static final class AEHistory extends Stack<AEAddr> {
    private static final long serialVersionUID = 1L;
    private static final String SEPARATOR = ",";

    AEHistory(final String hs) {
      super();
      if (hs != null) {
        final String[] h = hs.split(SEPARATOR);
        for (int i = h.length - 1; i >= 0; i--)
          push(new AEAddr(h[i]));
      }
    }

    AEAddr matchHost(final String host) {
      for (final AEAddr a : this) {
        if (a.host.equals(host))
          return a;
      }
      return null;
    }

    AEAddr matchTitle(final String title) {
      for (final AEAddr a : this) {
        if (a.title.equals(title))
          return a;
      }
      return null;
    }

    String[] getHosts() {
      final List<String> hosts = new LinkedList<String>();
      for (final AEAddr a : this)
        if (!hosts.contains(a.host))
          hosts.add(a.host);
      return hosts.toArray(new String[0]);
    }

    String[] getPorts() {
      final List<String> ports = new LinkedList<String>();
      for (final AEAddr a : this) {
        final String port = String.valueOf(a.port);
        if (!ports.contains(port))
          ports.add(port);
      }
      return ports.toArray(new String[0]);
    }

    String[] getTitles() {
      final List<String> aets = new LinkedList<String>();
      for (final AEAddr a : this)
        if (!aets.contains(a.title))
          aets.add(a.title);
      return aets.toArray(new String[0]);
    }

    @Override
    public AEAddr push(AEAddr item) {
      while (this.contains(item))
        this.remove(item);
      return super.push(item);
    }

    @Override
    public String toString() {
      final Iterator<AEAddr> ai = iterator();
      final StringBuilder sb = new StringBuilder(ai.hasNext() ? ai.next().toString() : null);
      while (ai.hasNext()) {
        sb.append(SEPARATOR);
        sb.append(ai.next().toString());
      }
      return sb.toString();
    }
  }

  private final AEHistory history;

  /**
   * @param owner
   */
  public CStoreDialog(DicomBrowser browser, FileSetTableModel model) {
    super(browser.getFrame());
    this.model = model;

    history = new AEHistory(DicomBrowser.prefs.get(AE_HISTORY, null));
    setLocationRelativeTo(browser);
    initialize();
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
      // TLS checkbox
      GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
      gridBagConstraints11.gridx = 3;
      gridBagConstraints11.gridy = 2;
      gridBagConstraints11.anchor = GridBagConstraints.LINE_START;

      // "Use secure connection (TLS):" label
      GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
      gridBagConstraints10.gridwidth = 3;
      gridBagConstraints10.gridx = 0;
      gridBagConstraints10.gridy = 2; 
      gridBagConstraints10.anchor = GridBagConstraints.LINE_END;

      // Port combo box
      GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
      gridBagConstraints9.gridx = 1;
      gridBagConstraints9.gridy = 1;
      gridBagConstraints9.ipadx = 20;
      gridBagConstraints9.ipady = -1;
      gridBagConstraints9.weightx = 1.0;
      gridBagConstraints9.insets = new Insets(8, 9, 7, 6);
      gridBagConstraints9.anchor = GridBagConstraints.LINE_START;

      // "Port:" label
      GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
      gridBagConstraints8.insets = new Insets(12, 60, 12, 9);
      gridBagConstraints8.gridy = 1;
      gridBagConstraints8.ipadx = 19;
      gridBagConstraints8.gridx = 0;
      gridBagConstraints8.anchor = GridBagConstraints.LINE_END;

      // AE title combo box
      GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
      gridBagConstraints7.gridx = 3;
      gridBagConstraints7.gridy = 1;
      gridBagConstraints7.ipadx = -39;
      gridBagConstraints7.weightx = 1.0;
      gridBagConstraints7.insets = new Insets(8, 3, 7, 11);
      gridBagConstraints7.anchor = GridBagConstraints.LINE_START;

      // Host combo box
      GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
      gridBagConstraints6.gridwidth = 3;
      gridBagConstraints6.gridx = 1;
      gridBagConstraints6.gridy = 0;
      gridBagConstraints6.ipadx = 121;
      gridBagConstraints6.ipady = -1;
      gridBagConstraints6.weightx = 1.0;
      gridBagConstraints6.insets = new Insets(15, 10, 8, 11);
      gridBagConstraints6.anchor = GridBagConstraints.LINE_START;

      // Cancel button
      GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
      gridBagConstraints5.insets = new Insets(7, 6, 19, 11);
      gridBagConstraints5.gridy = 3;
      gridBagConstraints5.ipadx = 1;
      gridBagConstraints5.gridx = 3;

      // Send button
      GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
      gridBagConstraints4.insets = new Insets(7, 7, 19, 3);
      gridBagConstraints4.gridy = 3;
      gridBagConstraints4.ipadx = 10;
      gridBagConstraints4.gridx = 2;

      // Send which files selector
      GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
      gridBagConstraints3.gridwidth = 2;
      gridBagConstraints3.gridx = 0;
      gridBagConstraints3.gridy = 3;
      gridBagConstraints3.ipadx = -6;
      gridBagConstraints3.ipady = 1;
      gridBagConstraints3.weightx = 1.0;
      gridBagConstraints3.insets = new Insets(7, 28, 19, 6);

      // "AE title:" label
      GridBagConstraints gridBagConstraints2 = new GridBagConstraints();     
      gridBagConstraints2.insets = new Insets(12, 18, 12, 8);
      gridBagConstraints2.gridy = 1;
      gridBagConstraints2.ipadx = 15;
      gridBagConstraints2.gridx = 2;
      gridBagConstraints2.anchor = GridBagConstraints.LINE_END;

      // "Remote host:" label
      GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
      gridBagConstraints1.insets = new Insets(20, 31, 12, 9);
      gridBagConstraints1.gridy = 0;
      gridBagConstraints1.gridx = 0;
      gridBagConstraints1.anchor = GridBagConstraints.LINE_END;

      portLabel = new JLabel();
      portLabel.setHorizontalAlignment(SwingConstants.RIGHT);
      portLabel.setText("Port:");

      aeLabel = new JLabel();
      aeLabel.setText("AE title:");
      aeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
      aeLabel.setHorizontalTextPosition(SwingConstants.RIGHT);

      hostLabel = new JLabel();
      hostLabel.setText("Remote host:");
      hostLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
      hostLabel.setHorizontalAlignment(SwingConstants.RIGHT);

      tlsLabel = new JLabel();
      tlsLabel.setText("Use secure connection (TLS):");
      tlsLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
      tlsLabel.setHorizontalAlignment(SwingConstants.RIGHT);

      jContentPane = new JPanel();
      jContentPane.setLayout(new GridBagLayout());
      jContentPane.add(hostLabel, gridBagConstraints1);
      jContentPane.add(aeLabel, gridBagConstraints2);
      jContentPane.add(getWhichFiles(), gridBagConstraints3);
      jContentPane.add(getSendButton(), gridBagConstraints4);
      jContentPane.add(getCancelButton(), gridBagConstraints5);
      jContentPane.add(getHostComboBox(), gridBagConstraints6);
      jContentPane.add(getAeComboBox(), gridBagConstraints7);
      jContentPane.add(portLabel, gridBagConstraints8);
      jContentPane.add(getPortComboBox(), gridBagConstraints9);
      jContentPane.add(tlsLabel, gridBagConstraints10);
      jContentPane.add(getUseTLSCheckBox(), gridBagConstraints11);
    }
    return jContentPane;
  }

  /**
   * This method initializes whichFiles	
   * 	
   * @return javax.swing.JComboBox	
   */
  private JComboBox getWhichFiles() {
    if (whichFiles == null) {
      whichFiles = new JComboBox(whichOpts);    // this comes out too small on Windows
      whichFiles.setMinimumSize(new Dimension(120,0));
    }
    return whichFiles;
  }

  /**
   * This method initializes sendButton	
   * 	
   * @return javax.swing.JButton	
   */
  private JButton getSendButton() {
    if (sendButton == null) {
      sendButton = new JButton(SEND_BUTTON);
      sendButton.addActionListener(this);
    }
    return sendButton;
  }

  /**
   * This method initializes cancelButton	
   * 	
   * @return javax.swing.JButton	
   */
  private JButton getCancelButton() {
    if (cancelButton == null) {
      cancelButton = new JButton(CANCEL_BUTTON);
      cancelButton.addActionListener(this);
    }
    return cancelButton;
  }

  /**
   * This method initializes hostComboBox	
   * 	
   * @return javax.swing.JComboBox	
   */
  private JComboBox getHostComboBox() {
    if (hostComboBox == null) {
      hostComboBox = new JComboBox(history.getHosts());
      hostComboBox.setEditable(true);
      hostComboBox.addActionListener(this);
      hostComboBox.setMinimumSize(new Dimension(120,0));
    }
    return hostComboBox;
  }

  /**
   * This method initializes aeComboBox	
   * 	
   * @return javax.swing.JComboBox	
   */
  private JComboBox getAeComboBox() {
    if (aeComboBox == null) {
      aeComboBox = new JComboBox(history.getTitles());
      aeComboBox.setEditable(true);
      aeComboBox.setMinimumSize(new Dimension(120,0));
    }
    return aeComboBox;
  }


  public void actionPerformed(ActionEvent e) {
    final Object source = e.getSource();
    if (source == hostComboBox) {
      final AEAddr a = history.matchHost((String)hostComboBox.getSelectedItem());
      if (a != null) {
        portComboBox.setSelectedItem(String.valueOf(a.port));
        aeComboBox.removeActionListener(this);
        aeComboBox.setSelectedItem(a.title);
        aeComboBox.addActionListener(this);
      }
    } else {
      final String command = e.getActionCommand();
      if (command.equals(SEND_BUTTON)) {
        setVisible(false);
        history.push(new AEAddr((String)hostComboBox.getSelectedItem(),
            Integer.parseInt((String)portComboBox.getSelectedItem()),
            (String)aeComboBox.getSelectedItem()));
        DicomBrowser.prefs.put(AE_HISTORY, history.toString());
        doSend();
      } else if (command.equals(CANCEL_BUTTON)) {
        setVisible(false);        // never mind
      } else {
        throw new RuntimeException("Unimplemented action: " + command);
      }
    }
  }

  private void doSend() { 
    final InterfaceCounter wc = InterfaceCounter.getInstance();
    wc.register(this);	// don't exit program until we're done with this send
    model.send((String)hostComboBox.getSelectedItem(),
        (String)portComboBox.getSelectedItem(),
        (String)aeComboBox.getSelectedItem(),
        useTLSCheckBox.isSelected(),
        whichFiles.getSelectedIndex() == ALL_FILES_INDEX);
    wc.unregister(this);
  }

  /**
   * This method initializes portComboBox	
   * 	
   * @return javax.swing.JComboBox	
   */
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

}  //  @jve:decl-index=0:visual-constraint="10,10"
