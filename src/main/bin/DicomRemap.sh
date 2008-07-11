#!/bin/sh
# $Id$
# Copyright (c) 2008 Washington University
# Author: Kevin A. Archie <karchie@npg.wustl.edu>
JAVA_HOME="${JAVA_HOME:-@java.home@}"
CLASSPATH="@app.lib@/DicomBrowser-1.3-SNAPSHOT.jar:@app.lib@/dcm4che-core-2.0.11.jar:@app.lib@/dcm4che-net-2.0.11.jar:@app.lib@/dom4j-1.6.1.jar:@app.lib@/ostermillerutils-1.07.jar:@app.lib@/xml-apis-1.0.b2.jar:@app.lib@/ij-1.38x.jar:@app.lib@/slf4j-api-1.2.jar:@app.lib@/slf4j-log4j12-1.2.jar:@app.lib@/log4j-1.2.14.jar:@app.lib@/hsqldb-1.8.0.7.jar:@app.lib@/cup_runtime-11a.jar:@app.lib@/json-lib-2.2.1-jdk15.jar:@app.lib@/commons-beanutils-1.7.0.jar:@app.lib@/commons-cli-1.1.jar:@app.lib@/commons-collections-3.2.jar:@app.lib@/commons-lang-2.3.jar:@app.lib@/commons-logging-1.1.jar:@app.lib@/ezmorph-1.0.4.jar:@app.lib@/ExtAttr-1.3-SNAPSHOT.jar:@app.lib@/DicomDB-1.1-SNAPSHOT.jar"
"${JAVA_HOME}/bin/java" -cp "$CLASSPATH" org.nrg.dcm.edit.CSVRemapper $*
