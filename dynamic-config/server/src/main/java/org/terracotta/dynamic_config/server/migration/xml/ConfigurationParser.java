/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.migration.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.TCConfigurationSetupException;
import org.terracotta.config.service.ExtendedConfigParser;
import org.terracotta.config.service.ServiceConfigParser;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static java.lang.System.lineSeparator;
import static org.terracotta.config.TCConfigurationParser.TERRACOTTA_XML_SCHEMA;

public class ConfigurationParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationParser.class);
  private static final SchemaFactory XSD_SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

  private static final Map<URI, ServiceConfigParser> serviceParsers = new HashMap<>();
  private static final Map<URI, ExtendedConfigParser> configParsers = new HashMap<>();

  private static Element getRootOfXml(InputStream in, ClassLoader loader) throws IOException, SAXException {
    Collection<Source> schemaSources = new ArrayList<>();

    schemaSources.add(new StreamSource(TERRACOTTA_XML_SCHEMA.openStream()));

    if (serviceParsers.isEmpty()) {
      for (ServiceConfigParser parser : loadServiceConfigurationParserClasses(loader)) {
        serviceParsers.put(parser.getNamespace(), parser);
      }
    }

    if (configParsers.isEmpty()) {
      for (ExtendedConfigParser parser : loadConfigurationParserClasses(loader)) {
        configParsers.put(parser.getNamespace(), parser);
      }
    }

    for (Map.Entry<URI, ExtendedConfigParser> entry : configParsers.entrySet()) {
      schemaSources.add(entry.getValue().getXmlSchema());
    }

    for (Map.Entry<URI, ServiceConfigParser> entry : serviceParsers.entrySet()) {
      schemaSources.add(entry.getValue().getXmlSchema());
    }

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setIgnoringComments(true);
    factory.setIgnoringElementContentWhitespace(true);
    factory.setSchema(XSD_SCHEMA_FACTORY.newSchema(schemaSources.toArray(new Source[schemaSources.size()])));

    final DocumentBuilder domBuilder;
    try {
      domBuilder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new AssertionError(e);
    }
    CollectingErrorHandler errorHandler = new CollectingErrorHandler();
    domBuilder.setErrorHandler(errorHandler);
    final Element config = domBuilder.parse(in).getDocumentElement();

    Collection<SAXParseException> parseErrors = errorHandler.getErrors();
    if (parseErrors.size() != 0) {
      StringBuffer buf = new StringBuffer("Couldn't parse configuration file, there are " + parseErrors.size() + " error(s)." + lineSeparator());
      int i = 1;
      for (SAXParseException parseError : parseErrors) {
        buf.append(" [" + i + "] Line " + parseError.getLineNumber() + ", column " + parseError.getColumnNumber() + ": " + parseError
            .getMessage()
            + lineSeparator());
        i++;
      }
      throw new TCConfigurationSetupException(buf.toString());
    }
    return config;
  }

  public static Element getRoot(File file, ClassLoader loader) throws IOException, SAXException {
    try (FileInputStream in = new FileInputStream(file)) {
      return getRootOfXml(in, loader);
    }
  }

  private static class CollectingErrorHandler implements ErrorHandler {

    private final List<SAXParseException> errors = new ArrayList<>();

    @Override
    public void error(SAXParseException exception) throws SAXException {
      errors.add(exception);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      errors.add(exception);
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
      LOGGER.warn(exception.getLocalizedMessage());
    }

    public Collection<SAXParseException> getErrors() {
      return Collections.unmodifiableList(errors);
    }
  }

  private static ServiceLoader<ServiceConfigParser> loadServiceConfigurationParserClasses(ClassLoader loader) {
    return ServiceLoader.load(ServiceConfigParser.class, loader);
  }


  private static ServiceLoader<ExtendedConfigParser> loadConfigurationParserClasses(ClassLoader loader) {
    return ServiceLoader.load(ExtendedConfigParser.class, loader);
  }
}
