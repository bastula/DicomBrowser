/**
 * $Id: ViewSlicesAction.java,v 1.10 2008/01/10 15:22:12 karchie Exp $
 * Copyright (c) 2007 Washington University
 */
package org.nrg.dcm.browse;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import java.awt.event.ActionEvent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;

import org.dcm4che2.data.Tag;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;

import org.nrg.dcm.DirectoryRecord;


/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
final class ViewSlicesAction extends AbstractAction {
  private static final long serialVersionUID = 1L;

  private final Collection<TreePath> selection;

  public ViewSlicesAction(final String name, final Collection<TreePath> selection) {
    super(name);
    this.selection = selection;
  }

  private final static class Ungz {
    private Ungz() {}	// no instantiation
    private final static Map<File,File> map = new HashMap<File,File>();
    final static String GZIP_SUFFIX = ".gz";

    public static File get(final File f) throws IOException {
      if (f.getName().endsWith(GZIP_SUFFIX)) {
	synchronized(map) {
	  if (map.containsKey(f) && map.get(f).exists())
	    return map.get(f);
	  else {
	    FileOutputStream fout = null;
	    BufferedOutputStream bout = null;
	    FileInputStream fin = null;
	    GZIPInputStream gzin = null;

	    final int BUFSIZE = 2048;
	    final byte[] buf = new byte[BUFSIZE];

	    try {
	      final File unzip = File.createTempFile(f.getName().replace(".gz", ""), ".tmp");
	      unzip.deleteOnExit();

	      fout = new FileOutputStream(unzip);
	      bout = new BufferedOutputStream(fout);
	      fin = new FileInputStream(f);
	      gzin = new GZIPInputStream(fin);

	      int count;
	      while ((count = gzin.read(buf)) != -1)
		bout.write(buf, 0, count);

	      map.put(f, unzip);
	      return unzip;
	    } finally {
	      if (gzin != null) gzin.close();
	      if (fin != null) fin.close();
	      if (bout != null) bout.close();
	      if (fout != null) fout.close();
	    }
	  }
	}
      } else
	return f;
    }
  }


  private static class ImageFinder implements Runnable {
    private final Collection<TreePath> selection;

    ImageFinder(final Collection<TreePath> selection) {
      this.selection = new HashSet<TreePath>(selection);
    }

    /**
     * Determine what files are selected, organized by Series
     * @return Map from Series DirectoryRecord to Collection of DICOM files
     */
    public final Map<DirectoryRecord,Collection<File>> getSeriesImages() {
      final Map<DirectoryRecord,Collection<File>> images = new HashMap<DirectoryRecord,Collection<File>>();

      for (final TreePath tp : selection) {
	final Queue<DirectoryRecord> records = new LinkedList<DirectoryRecord>();
	records.add((DirectoryRecord)tp.getLastPathComponent());
	while (records.peek() != null) {
	  final DirectoryRecord dr = records.poll();
	  final String rfpath = dr.getValue(Tag.ReferencedFileID);
	  if (rfpath != null) {
	    final DirectoryRecord series = dr.getUpper();
	    assert series.getValue(Tag.SeriesNumber) != null;
	    if (!images.containsKey(series))
	      images.put(series, new LinkedList<File>());
	    images.get(series).add(new File(rfpath));
	  } else
	    records.addAll(dr.getLower());
	}
      }
      return images;
    }

      // TODO: no out-of-memory check
     private void showImages(final String label, final Collection<File> files) throws IOException {
       final Iterator<File> fi = files.iterator();
       if (!fi.hasNext())
	 return;	// no images to view

       final ImageJ ij = IJ.getInstance();
       if (null == ij || ij.quitting()) {	// initialize IJ and make a window
	 new ImageJ(null, ImageJ.EMBEDDED).exitWhenQuitting(false);
       }

       final File first = Ungz.get(fi.next());
       int progress = 0;
       IJ.showStatus("Loading " + first.getName());	// TODO: localize
       IJ.showProgress(progress, files.size());

       final ImagePlus firstImage = new ImagePlus(first.getPath());
       if (null == firstImage)
	 throw new IOException("ImageJ failed to open DICOM file " + first);

       final ImageStack stack = firstImage.getStack();

       while (fi.hasNext()) {
	 final File file = Ungz.get(fi.next());
	 IJ.showStatus("Loading " + file.getName());	// TODO: localize
	 IJ.showProgress(++progress, files.size());
	 final ImagePlus im = new ImagePlus(file.getPath());
	 if (null == im)
	   throw new IOException("ImageJ failed to open DICOM file " + file);
	 final ImageStack oneImage = im.getStack();
	 assert oneImage.getSize() == 1;
	 stack.addSlice(file.getName(), oneImage.getProcessor(1));	// index is 1-offset
       }

       final ImagePlus image = new ImagePlus(label, stack);
       image.show();
       IJ.showProgress(++progress);
       assert progress == files.size();
     }


     public void run() {
       for (final Map.Entry<DirectoryRecord,Collection<File>> me : getSeriesImages().entrySet()) try {
	 showImages(me.getKey().toString(), me.getValue());
       } catch (final IOException ioe) {
	 SwingUtilities.invokeLater(new Runnable() {
	   public void run() {
	     JOptionPane.showMessageDialog(null, ioe.getMessage(),
		 "Error loading image", JOptionPane.ERROR_MESSAGE);
	   }
	 });
       }
     }
  }


  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(final ActionEvent e) {
    new Thread(new ImageFinder(new LinkedHashSet<TreePath>(selection))).start();
  }
}
