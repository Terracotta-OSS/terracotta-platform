/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.xml.topology.config.xmlobjects;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the org.terracotta.dynamic_config.xml.topology.config.xmlobjects package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

  private final static QName _Cluster_QNAME = new QName("http://www.terracotta.org/config/cluster", "cluster");

  /**
   * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.terracotta.dynamic_config.xml.topology.config.xmlobjects
   */
  public ObjectFactory() {
  }

  /**
   * Create an instance of {@link TcCluster }
   */
  public TcCluster createTcCluster() {
    return new TcCluster();
  }

  /**
   * Create an instance of {@link TcStripe }
   */
  public TcStripe createTcStripe() {
    return new TcStripe();
  }

  /**
   * Create an instance of {@link TcNode }
   */
  public TcNode createTcNode() {
    return new TcNode();
  }

  /**
   * Create an instance of {@link TcServerConfig }
   */
  public TcServerConfig createTcServerConfig() {
    return new TcServerConfig();
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link TcCluster }{@code >}}
   */
  @XmlElementDecl(namespace = "http://www.terracotta.org/config/cluster", name = "cluster", substitutionHeadNamespace = "http://www.terracotta.org/config", substitutionHeadName = "config-content")
  public JAXBElement<TcCluster> createCluster(TcCluster value) {
    return new JAXBElement<TcCluster>(_Cluster_QNAME, TcCluster.class, null, value);
  }
}