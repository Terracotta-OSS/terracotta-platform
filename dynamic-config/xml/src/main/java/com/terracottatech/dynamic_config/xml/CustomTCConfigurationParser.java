/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.Config;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;
import org.terracotta.config.Service;
import org.terracotta.config.TCConfigurationSetupException;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.config.service.ConfigValidator;
import org.terracotta.config.service.ExtendedConfigParser;
import org.terracotta.config.service.ServiceConfigParser;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * This is a copy of the TCConfigurationParser class in platform but this one is not doing any substitution
 */
public class CustomTCConfigurationParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(org.terracotta.config.TCConfigurationParser.class);
  private static final SchemaFactory XSD_SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
  private static final URL TERRACOTTA_XML_SCHEMA = org.terracotta.config.TCConfigurationParser.class.getResource("/terracotta.xsd");

  private static final Map<URI, ServiceConfigParser> serviceParsers = new HashMap<>();
  private static final Map<URI, ExtendedConfigParser> configParsers = new HashMap<>();

  private static TcConfiguration parseStream(InputStream in, String source, ClassLoader loader) throws IOException, SAXException {
    Collection<Source> schemaSources = new ArrayList<>();

    schemaSources.add(new StreamSource(TERRACOTTA_XML_SCHEMA.openStream()));

    for (ServiceConfigParser parser : loadServiceConfigurationParserClasses(loader)) {
      schemaSources.add(parser.getXmlSchema());
      serviceParsers.put(parser.getNamespace(), parser);
    }
    for (ExtendedConfigParser parser : loadConfigurationParserClasses(loader)) {
      schemaSources.add(parser.getXmlSchema());
      configParsers.put(parser.getNamespace(), parser);
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
      StringBuffer buf = new StringBuffer("Couldn't parse configuration file, there are " + parseErrors.size() + " error(s).\n");
      int i = 1;
      for (SAXParseException parseError : parseErrors) {
        buf.append(" [" + i + "] Line " + parseError.getLineNumber() + ", column " + parseError.getColumnNumber() + ": " + parseError.getMessage()
            + "\n");
        i++;
      }
      throw new TCConfigurationSetupException(buf.toString());
    }

    try {
      JAXBContext jc = JAXBContext.newInstance("org.terracotta.config", org.terracotta.config.TCConfigurationParser.class.getClassLoader());
      Unmarshaller u = jc.createUnmarshaller();

      TcConfig tcConfig = u.unmarshal(config, TcConfig.class).getValue();
      if (tcConfig.getServers() == null) {
        Servers servers = new Servers();
        tcConfig.setServers(servers);
      }

      if (tcConfig.getServers().getServer().isEmpty()) {
        tcConfig.getServers().getServer().add(new Server());
      }

      List<ServiceProviderConfiguration> serviceConfigurations = new ArrayList<>();
      List<Object> configObjects = new ArrayList<>();
      if (tcConfig.getPlugins() != null && tcConfig.getPlugins().getConfigOrService() != null) {
        //now parse the service configuration.
        for (Object plugin : tcConfig.getPlugins().getConfigOrService()) {
          if (plugin instanceof Service) {
            Element element = ((Service) plugin).getServiceContent();
            URI namespace = URI.create(element.getNamespaceURI());
            ServiceConfigParser parser = serviceParsers.get(namespace);
            if (parser == null) {
              throw new TCConfigurationSetupException("Can't find parser for service " + namespace);
            }
            ServiceProviderConfiguration serviceProviderConfiguration = parser.parse(element, source);
            serviceConfigurations.add(serviceProviderConfiguration);
          } else if (plugin instanceof Config) {
            Element element = ((Config) plugin).getConfigContent();
            URI namespace = URI.create(element.getNamespaceURI());
            ExtendedConfigParser parser = configParsers.get(namespace);
            if (parser == null) {
              throw new TCConfigurationSetupException("Can't find parser for config " + namespace);
            }
            Object co = parser.parse(element, source);
            configObjects.add(co);
          }
        }
      }

      return new TcConfiguration(tcConfig, source, configObjects, serviceConfigurations);
    } catch (JAXBException e) {
      throw new TCConfigurationSetupException(e);
    }
  }

  private static TcConfiguration convert(InputStream in, String path, ClassLoader loader) throws IOException, SAXException {
    byte[] data = new byte[in.available()];
    in.read(data);
    in.close();
    ByteArrayInputStream bais = new ByteArrayInputStream(data);

    return parseStream(bais, path, loader);
  }

  public static TcConfiguration parse(File f) throws IOException, SAXException {
    return parse(f, Thread.currentThread().getContextClassLoader());
  }

  public static TcConfiguration parse(File file, ClassLoader loader) throws IOException, SAXException {
    try (FileInputStream in = new FileInputStream(file)) {
      return convert(in, file.getParent(), loader);
    }
  }

  public static TcConfiguration parse(String xmlText) throws IOException, SAXException {
    return parse(xmlText, Thread.currentThread().getContextClassLoader());
  }

  public static TcConfiguration parse(String xmlText, ClassLoader loader) throws IOException, SAXException {
    return convert(new ByteArrayInputStream(xmlText.getBytes()), null, loader);
  }

  public static TcConfiguration parse(InputStream stream) throws IOException, SAXException {
    return parse(stream, Thread.currentThread().getContextClassLoader());
  }

  public static TcConfiguration parse(InputStream stream, ClassLoader loader) throws IOException, SAXException {
    return convert(stream, null, loader);
  }

  public static TcConfiguration parse(URL stream) throws IOException, SAXException {
    return parse(stream, Thread.currentThread().getContextClassLoader());
  }

  public static TcConfiguration parse(URL url, ClassLoader loader) throws IOException, SAXException {
    return convert(url.openStream(), url.getPath(), loader);
  }

  public static TcConfiguration parse(InputStream in, Collection<SAXParseException> errors, String source) throws IOException, SAXException {
    return parse(in, errors, source, Thread.currentThread().getContextClassLoader());
  }

  public static TcConfiguration parse(InputStream in, Collection<SAXParseException> errors, String source, ClassLoader loader) throws IOException, SAXException {
    return parseStream(in, source, loader);
  }

  public static ConfigValidator getValidator(URI namespace) {
    ServiceConfigParser parserObject = serviceParsers.get(namespace);
    if (parserObject != null) {
      return parserObject.getConfigValidator();
    }
    ExtendedConfigParser extendedConfigParser = configParsers.get(namespace);
    if (extendedConfigParser != null) {
      return extendedConfigParser.getConfigValidator();
    }
    return null;
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
