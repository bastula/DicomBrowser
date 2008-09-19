/**
 * $Id: Sender.java,v 1.2 2007/04/03 04:32:23 karchie Exp $
 * Copyright (c) 2006 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.swing.ProgressMonitor;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkApplicationEntityBuilder;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NetworkConnectionBuilder;
import org.dcm4che2.net.TransferCapability;

import org.nrg.dcm.DicomSender;
import org.nrg.dcm.edit.Statement;

final class Sender extends Exporter {
	private static final String MAKE_CONN_MSG = "Making network connection";
//	private static final String BAD_CONN_MSG = "Unable to make network connection: %1$s";

	private int progress = 0;
	private final ProgressMonitor pm;
	private DicomSender sender;


	Sender(final String host, final String port, final boolean isTLS, final String aeTitle,
			final TransferCapability[] tcs, final Collection<File> files,
			final Statement statements, final ProgressMonitor pm) {
		super(statements, files);
		
		final NetworkConnection nc;
		if (isTLS) {
			nc = new NetworkConnectionBuilder()
				.setTls(NetworkConnectionBuilder.TlsType.AES)
				.setTlsNeedClientAuth(false)
				.build();
		} else {
			nc = new NetworkConnection();
		}

		final NetworkApplicationEntity localAE = new NetworkApplicationEntityBuilder()
			.setAETitle(aeTitle)
			.setTransferCapability(tcs)
			.setNetworkConnection(nc)
			.build();
		
		final NetworkApplicationEntity remoteAE = new NetworkApplicationEntityBuilder()
			.setNetworkConnection(new NetworkConnectionBuilder()
				.setHostname(host)
				.setPort(Integer.parseInt(port))
				.build())
			.build();
		
		this.sender = new DicomSender(localAE, remoteAE);

		this.pm = pm;
	}

	@Override
	void open() throws IOException {
		if (pm != null)
			pm.setNote(MAKE_CONN_MSG);
	}

	
	@Override
	protected void process(File f, DicomObject o) throws IOException,CancelException {
		if (pm != null)
			pm.setNote(f.getName());

		sender.send(o, getTransferSyntaxUID(o));

		if (pm != null) {
			if (pm.isCanceled()) {
				throw new CancelException();
			}
			pm.setProgress(++progress);
		}
	}

	@Override
	void close() {
		if (sender != null)
			sender.close();
		if (pm != null)
			pm.close();
	}
}