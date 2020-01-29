/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.BindPort;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;
import org.terracotta.config.TCConfigDefaults;
import org.terracotta.config.TCConfigurationSetupException;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.config.service.ExtendedConfigParser;
import org.terracotta.config.service.ServiceConfigParser;
import org.terracotta.config.util.ParameterSubstitutor;
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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

import static java.lang.System.lineSeparator;

/**
 * This is a copy of the TCConfigurationParser class in platform but this one is not doing any substitution
 */
public class CustomTCConfigurationParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(org.terracotta.config.TCConfigurationParser.class);
  private static final SchemaFactory XSD_SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
  private static final URL TERRACOTTA_XML_SCHEMA = org.terracotta.config.TCConfigurationParser.class.getResource("/terracotta.xsd");
  private static final String WILDCARD_IP = "0.0.0.0";
  private static final int MIN_PORTNUMBER = 0x0FFF;
  private static final int MAX_PORTNUMBER = 0xFFFF;

  private static TcConfiguration parseStream(InputStream in, String source) throws IOException, SAXException {
    Collection<Source> schemaSources = new ArrayList<>();

    schemaSources.add(new StreamSource(TERRACOTTA_XML_SCHEMA.openStream()));

    for (ServiceConfigParser parser : loadServiceConfigurationParserClasses()) {
      schemaSources.add(parser.getXmlSchema());
    }
    for (ExtendedConfigParser parser : loadConfigurationParserClasses()) {
      schemaSources.add(parser.getXmlSchema());
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
      StringBuilder buf = new StringBuilder("Couldn't parse configuration file, there are " + parseErrors.size() + " error(s)." + lineSeparator());
      int i = 1;
      for (SAXParseException parseError : parseErrors) {
        buf.append(" [").append(i).append("] Line ").append(parseError.getLineNumber()).append(", column ")
            .append(parseError.getColumnNumber()).append(": ").append(parseError.getMessage()).append(lineSeparator());
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

      applyPlatformDefaults(tcConfig);

      return new TcConfiguration(tcConfig, source, Collections.emptyList(), Collections.emptyList());
    } catch (JAXBException e) {
      throw new TCConfigurationSetupException(e);
    }
  }

  public static void applyPlatformDefaults(TcConfig tcConfig) {
    for (Server server : tcConfig.getServers().getServer()) {
      setDefaultBind(server);
      initializeTsaPort(server);
      initializeTsaGroupPort(server);
      initializeNameAndHost(server);
    }
  }

  private static void initializeNameAndHost(Server server) {
    if (server.getHost() == null || server.getHost().trim().length() == 0) {
      if (server.getName() == null) {
        server.setHost("%i");
      } else {
        server.setHost(server.getName());
      }
    }
    if (server.getName() == null || server.getName().trim().length() == 0) {
      int tsaPort = server.getTsaPort().getValue();
      server.setName(server.getHost() + (tsaPort > 0 ? ":" + tsaPort : ""));
    }
  }

  private static void initializeTsaGroupPort(Server server) {
    if (server.getTsaGroupPort() == null) {
      BindPort l2GrpPort = new BindPort();
      server.setTsaGroupPort(l2GrpPort);
      int tempGroupPort = server.getTsaPort().getValue() + TCConfigDefaults.GROUPPORT_OFFSET_FROM_TSAPORT;
      int defaultGroupPort = ((tempGroupPort <= MAX_PORTNUMBER) ? (tempGroupPort) : (tempGroupPort % MAX_PORTNUMBER) + MIN_PORTNUMBER);
      l2GrpPort.setValue(defaultGroupPort);
      l2GrpPort.setBind(server.getBind());
    } else if (server.getTsaGroupPort().getBind() == null) {
      server.getTsaGroupPort().setBind(server.getBind());
    }
  }

  private static void setDefaultBind(Server s) {
    if (s.getBind() == null || s.getBind().trim().length() == 0) {
      s.setBind(WILDCARD_IP);
    }
    s.setBind(ParameterSubstitutor.substitute(s.getBind()));
  }

  private static void initializeTsaPort(Server server) {
    if (server.getTsaPort() == null) {
      BindPort tsaPort = new BindPort();
      tsaPort.setValue(TCConfigDefaults.TSA_PORT);
      server.setTsaPort(tsaPort);
    }
    if (server.getTsaPort().getBind() == null) {
      server.getTsaPort().setBind(server.getBind());
    }
  }

  private static TcConfiguration convert(InputStream in, String path) throws IOException, SAXException {
    byte[] data = new byte[in.available()];
    in.read(data);
    in.close();
    ByteArrayInputStream bais = new ByteArrayInputStream(data);

    return parseStream(bais, path);
  }

  public static TcConfiguration parse(File file) throws IOException, SAXException {
    try (FileInputStream in = new FileInputStream(file)) {
      return convert(in, file.getParent());
    }
  }

  public static TcConfiguration parse(String xmlText) throws IOException, SAXException {
    return convert(new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8)), null);
  }

  public static TcConfiguration parse(InputStream stream) throws IOException, SAXException {
    return convert(stream, null);
  }

  public static TcConfiguration parse(URL url) throws IOException, SAXException {
    return convert(url.openStream(), url.getPath());
  }

  public static TcConfiguration parse(InputStream in, Collection<SAXParseException> errors, String source) throws IOException, SAXException {
    return parseStream(in, source);
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

  private static ServiceLoader<ServiceConfigParser> loadServiceConfigurationParserClasses() {
    return ServiceLoader.load(ServiceConfigParser.class, CustomTCConfigurationParser.class.getClassLoader());
  }


  private static ServiceLoader<ExtendedConfigParser> loadConfigurationParserClasses() {
    return ServiceLoader.load(ExtendedConfigParser.class, CustomTCConfigurationParser.class.getClassLoader());
  }
}
