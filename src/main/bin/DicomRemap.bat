@ECHO OFF
set LIB=%~dp0..\lib
"%JAVA_HOME%\bin\java" -cp "%LIB%/DicomBrowser-1.5-SNAPSHOT.jar;%LIB%/slf4j-api-1.5.0.jar;%LIB%/slf4j-nop-1.5.0.jar;%LIB%/DicomEdit-1.2-SNAPSHOT.jar;%LIB%/dcm4che-core-2.0.22.jar;%LIB%/dcm4che-net-2.0.22.jar;%LIB%/json-20080701.jar;%LIB%/antlr-runtime-3.2.jar;%LIB%/stringtemplate-3.2.jar;%LIB%/antlr-2.7.7.jar;%LIB%/ij-1.41o.jar;%LIB%/ostermillerutils-1.07.jar;%LIB%/dom4j-1.6.1.jar;%LIB%/DicomDB-browser-0.1-SNAPSHOT.jar;%LIB%/DicomUtils-1.0.jar;%LIB%/ExtAttr-1.5.jar;%LIB%/commons-cli-1.1.jar;%LIB%/commons-lang-2.4.jar;%LIB%/hsqldb-1.8.0.10.jar;%LIB%/xml-apis-1.0.b2.jar" org.nrg.dcm.edit.CSVRemapper %*

