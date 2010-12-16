/**
 * Copyright (c) 2009,2010 Washington University
 */
package org.nrg.dcm.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.zip.GZIPOutputStream;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomOutputStream;
import org.nrg.dcm.edit.DicomUtils;
import org.nrg.dcm.edit.DicomUtils.DicomObjectException;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public abstract class AbstractFileExporter implements DicomObjectExporter {
	private final String aeTitle;

	AbstractFileExporter(final String aeTitle) {
		this.aeTitle = aeTitle;
	}

	protected abstract File map(File source);

	private static boolean hasFileMetaInformation(final DicomObject o) {
		return o.contains(Tag.MediaStorageSOPClassUID) && o.contains(Tag.MediaStorageSOPInstanceUID);
	}

	/**
	 * Returns the Transfer Syntax UID for the given DicomObject
	 * @param o DicomObject
	 * @return Transfer Syntax UID
	 */
	protected static final String getTransferSyntaxUID(final DicomObject o) {
		final String ts = o.getString(Tag.TransferSyntaxUID);
		// Default TS UID is Implicit VR LE (PS 3.5, Section 10)
		return ts == null ? UID.ImplicitVRLittleEndian : ts;
	}

	/*
	 * (non-Javadoc)
	 * @see org.nrg.dcm.io.DicomObjectExporter#close()
	 */
	public final void close() {}

	/* (non-Javadoc)
	 * @see org.nrg.dcm.io.DicomObjectExporter#export(org.dcm4che2.data.DicomObject, java.io.File)
	 */
	public final void export(final DicomObject o, final File source)
	throws DicomObjectException,IOException,MalformedURLException,URISyntaxException {
		final DicomObject dcmo = null == o ? DicomUtils.read(source) : o;
		final DicomObject fmi;
		if (hasFileMetaInformation(dcmo)) {
			fmi = dcmo.fileMetaInfo();
		} else {
			assert null != o;
			// Part 10 header ends after group 0002, but read() checks SOP Class UID
			// to ensure file validity so we need to read at least that far.
			final DicomObject original = DicomUtils.read(source, Tag.SOPClassUID);
			fmi = original.fileMetaInfo();
		}
		final String tsuid = getTransferSyntaxUID(fmi);

		fmi.putString(Tag.SourceApplicationEntityTitle, VR.AE, aeTitle);

		final File dest = map(source);
		dest.getParentFile().mkdirs();
		IOException ioexception = null;
		final FileOutputStream fos = new FileOutputStream(dest);
		try {
			final OutputStream os;
			if (dest.getName().endsWith(".gz")) {
				os = new GZIPOutputStream(fos);
			} else {
				os = new BufferedOutputStream(fos);
			}
			try {
				final DicomOutputStream dos = new DicomOutputStream(os);
				try {
					dos.writeFileMetaInformation(fmi);
					dos.writeDataset(o, tsuid);
				} catch (IOException e) {
					throw ioexception = e;
				} finally {
					try {
						dos.close();
					} catch (IOException e) {
						throw ioexception = null == ioexception ? e : ioexception;
					}
				}
			} finally {
				try {
					os.close();
				} catch (IOException e) {
					throw ioexception = null == ioexception ? e : ioexception;
				}
			}
		} finally {
			try {
				fos.close();
			} catch (IOException e) {
				throw null == ioexception ? e : ioexception;
			}
		}
	}
}
