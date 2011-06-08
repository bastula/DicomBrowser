/**
 * Copyright (c) 2006-2010 Washington University
 */
package org.nrg.dcm.browse;

import java.util.Properties;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
final class AboutDialog extends JDialog {
  final static private long serialVersionUID = 1L;
  final static private int MARGIN = 2;
  final static private int SPACE = 8;
  final static String TITLE;
  final static String VERSION;
  static {
	  final Properties props = new Properties();
	  final ClassLoader cl = AboutDialog.class.getClassLoader();
	  try {
		  props.load(cl.getResourceAsStream("META-INF/application.properties"));
	  } catch (Exception e) {
		  System.err.println("Unable to load properties: " + e.getMessage());
	  }
	  TITLE = props.getProperty("application.name");
	  VERSION = props.getProperty("application.version");
  }

  AboutDialog(final Frame owner, final String title) {
    super(owner, title, true);
    setLocationRelativeTo(owner);

    final Container pane = this.getContentPane();
    setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
    
    final Border margin = new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN);
    final Border spaceMargin = new EmptyBorder(SPACE+MARGIN, MARGIN, MARGIN, MARGIN);
    
    final JLabel name = new JLabel(TITLE);
    name.setBorder(margin);
    name.setFont(new Font("SansSerif", Font.PLAIN, 18));
    name.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(name);
    
    final JLabel version = new JLabel("version " + VERSION);
    version.setBorder(margin);
    version.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(version);
    
    final JLabel wucopy = new JLabel("Copyright \u00A9 2006-2010 Washington University");
    wucopy.setBorder(margin);
    wucopy.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(wucopy);
    
    final JLabel devel = new JLabel("Developed by the Neuroinformatics Research Group");
    devel.setBorder(margin);
    devel.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(devel);
    
    final JLabel email = new JLabel("Questions, comments, or bugs?  Mail nrg-tech@nrg.wustl.edu");
    email.setBorder(margin);
    email.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(email);
    
    final JLabel imagej = new JLabel("Image viewer: ImageJ by Wayne Rasband");
    imagej.setBorder(spaceMargin);
    imagej.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(imagej);
    
    final JLabel imagejURL = new JLabel("http://rsb.info.nih.gov/ij/");
    imagejURL.setBorder(margin);
    imagejURL.setAlignmentX(Component.CENTER_ALIGNMENT);
    add(imagejURL);
    
    this.pack();
    setResizable(false);
  }
}
