/**
 * $Id: UIDRequester.java,v 1.3 2008/04/02 22:21:05 karchie Exp $
 * Copyright (c) 2008 Washington University
 */
package org.nrg.dcm.edit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;


/**
 * Requests a UID from a remote web service using JSON encoding.
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 */
class UIDRequester {
  private final static String UID_KEY = "UID";
  
  private final String uid;
  
  public UIDRequester(final URL server) throws IOException {
    final HttpURLConnection connection;
    try {
      connection = (HttpURLConnection)server.openConnection();
    } catch (ClassCastException e) {
      throw new IOException("unable to make HTTP/HTTPS connection to URL " + server);
    }
    if (HttpURLConnection.HTTP_OK != connection.getResponseCode()) {
      throw new IOException("request failed: " + connection.getResponseCode()
	  + " " + connection.getResponseMessage());
    }

    final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    final StringBuilder text = new StringBuilder();
    for (String line = reader.readLine(); null != line; line = reader.readLine()) {
      text.append(line);
    }
    reader.close();
    connection.disconnect();

    final JSONObject jso = JSONObject.fromObject(text.toString());
    final String o = jso.getString(UID_KEY);
    System.out.println("retrieved UID root: " + o);
    uid = isUID(o) ? o : null;	// TODO: should throw exception?
  }

  public String getUID() { return uid; }

  
  private final static Pattern VALID_UID_PATTERN = Pattern.compile("(0|([1-9][0-9]*))(\\.(0|([1-9][0-9]*)))*");
  private final static int UID_MIN_LEN = 1;
  private final static int UID_MAX_LEN = 64;

  public static boolean isUID(final String uid) {
    if (null == uid) return false;
    final int len = uid.length();
    if (len < UID_MIN_LEN || UID_MAX_LEN < len) return false;
    return VALID_UID_PATTERN.matcher(uid).matches();
  }
  

  /**
   * @param args
   */
  public static void main(String[] args) throws IOException {
    for (final String server : args) {
      final URL url = new URL(server);
      System.out.println("new root from " + server + " : " + new UIDRequester(url).getUID());
    }
  }

}
