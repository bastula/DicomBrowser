/**
 * Copyright (c) 2009 Washington University
 */
package org.nrg.dcm.io;

import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkApplicationEntityBuilder;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NetworkConnectionBuilder;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.NetworkConnectionBuilder.TlsType;
import org.nrg.dcm.DicomSender;


/**
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 *
 */
public class CStoreExporter implements DicomObjectExporter {
    private static final String PROTOCOL_TLS = "TLS";

    private final static TrustManager[] yesManagers = new TrustManager[] {
      new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] chain, String authType) { /* always accept */ }
        public void checkServerTrusted(X509Certificate[] chain, String authType) { /* always accept */ }
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
      }};

    private final DicomSender sender;

    public CStoreExporter(final DicomSender sender) {
	this.sender = sender;
    }
    
    public CStoreExporter(final String host, final String port,
	    final boolean isTLS, final String remAETitle, final String locAETitle,
	    final TransferCapability[] tcs) {
	this(buildSender(host, port, remAETitle, locAETitle, tcs, isTLS ? TlsType.AES : null, false, yesManagers));
    }

    private static DicomSender buildSender(final String remHost, final String remPort,
	    final String remAETitle, final String locAETitle,
	    final TransferCapability[] tcs, final TlsType tlsType,
	    final boolean needsClientAuth, final TrustManager[] trustManagers) {
	final NetworkConnection lnc;
	if (null != tlsType) {
	    lnc = new NetworkConnectionBuilder()
	    .setTls(tlsType)
	    .setTlsNeedClientAuth(needsClientAuth)
	    .build();
	} else {
	    lnc = new NetworkConnection();
	}

	final NetworkApplicationEntity localAE = new NetworkApplicationEntityBuilder()
	.setAETitle(locAETitle)
	.setTransferCapability(tcs)
	.setNetworkConnection(lnc)
	.build();

	final NetworkConnectionBuilder rncb = new NetworkConnectionBuilder();
	rncb.setHostname(remHost).setPort(Integer.parseInt(remPort));
	if (null != tlsType) {
	    rncb.setTls(tlsType).setTlsNeedClientAuth(needsClientAuth);
	}

	final NetworkApplicationEntity remoteAE = new NetworkApplicationEntityBuilder()
	.setNetworkConnection(rncb.build())
	.setAETitle(remAETitle)
	.build();

	final DicomSender sender = new DicomSender(localAE, remoteAE);
	if (null != tlsType) {
	    try {
		final SSLContext context = SSLContext.getInstance(PROTOCOL_TLS);
		context.init(null, trustManagers, null);
		sender.setSSLContext(context);
	    } catch (NoSuchAlgorithmException e) {
		throw new RuntimeException(e);	// programming error
	    } catch (KeyManagementException e) {
		throw new RuntimeException(e);
	    }
	}
	return sender;
    }

    
    /* (non-Javadoc)
     * @see org.nrg.dcm.io.DicomObjectExporter#export(org.dcm4che2.data.DicomObject, java.io.File)
     */
    public void export(final DicomObject o, final File source) throws Exception {
	sender.send(o, IOUtils.getTransferSyntaxUID(o));
    }
}
