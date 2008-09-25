/**
 * $Id: Sender.java,v 1.2 2007/04/03 04:32:23 karchie Exp $
 * Copyright (c) 2006 Washington University
 */
package org.nrg.dcm.browse;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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
	private static final String AE_TITLE = "DicomBrowser";
	private static final String PROTOCOL_TLS = "TLS";

	
	/**
	 * Why do you always have to be so suspicious?
	 */
	private final static TrustManager[] yesManagers = new TrustManager[] {
		new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] chain, String authType) { /* yes, sir! */ }
			public void checkServerTrusted(X509Certificate[] chain, String authType) { /* will do! */ }
			public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
		}};


	private int progress = 0;
	private final ProgressMonitor pm;
	private DicomSender sender;

	Sender(final String host, final String port, final boolean isTLS, final String aeTitle,
			final TransferCapability[] tcs, final Collection<File> files,
			final Statement statements, final ProgressMonitor pm) {
		super(statements, files);

		final NetworkConnection lnc;
		if (isTLS) {
			lnc = new NetworkConnectionBuilder()
			.setTls(NetworkConnectionBuilder.TlsType.AES)
			.setTlsNeedClientAuth(false)
			.build();
		} else {
			lnc = new NetworkConnection();
		}

		final NetworkApplicationEntity localAE = new NetworkApplicationEntityBuilder()
		.setAETitle(AE_TITLE)
		.setTransferCapability(tcs)
		.setNetworkConnection(lnc)
		.build();

		final NetworkConnectionBuilder rncb = new NetworkConnectionBuilder();
		rncb.setHostname(host).setPort(Integer.parseInt(port));
		if (isTLS) {
			rncb.setTls(NetworkConnectionBuilder.TlsType.AES).setTlsNeedClientAuth(false);
		}

		final NetworkApplicationEntity remoteAE = new NetworkApplicationEntityBuilder()
		.setNetworkConnection(rncb.build())
		.setAETitle(aeTitle)
		.build();

		this.sender = new DicomSender(localAE, remoteAE);
		if (isTLS) {
			try {
				final SSLContext context = SSLContext.getInstance(PROTOCOL_TLS);
				context.init(null, yesManagers, null);
				this.sender.setSSLContext(context);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);	// programming error
			} catch (KeyManagementException e) {
				throw new RuntimeException(e);
			}
		}

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