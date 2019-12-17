/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml.topology.config.xmlobjects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for tc-node complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tc-node"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="name" type="{http://www.terracotta.org/config}non-blank-token"/&gt;
 *         &lt;element name="publicHostname" type="{http://www.terracotta.org/config}host" minOccurs="0"/&gt;
 *         &lt;element name="publicPort" type="{http://www.terracotta.org/config}port" minOccurs="0"/&gt;
 *         &lt;element name="server-config" type="{http://www.terracotta.org/config/cluster}tc-server-config"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tc-node", namespace = "http://www.terracotta.org/config/cluster", propOrder = {
    "name",
    "publicHostname",
    "publicPort",
    "serverConfig"
})
public class TcNode {

  @XmlElement(required = true)
  @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
  @XmlSchemaType(name = "token")
  protected String name;
  protected String publicHostname;
  @XmlSchemaType(name = "nonNegativeInteger")
  protected Integer publicPort;
  @XmlElement(name = "server-config", required = true)
  protected TcServerConfig serverConfig;

  /**
   * Gets the value of the name property.
   *
   * @return possible object is
   * {@link String }
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the value of the name property.
   *
   * @param value allowed object is
   *              {@link String }
   */
  public void setName(String value) {
    this.name = value;
  }

  /**
   * Gets the value of the publicHostname property.
   *
   * @return possible object is
   * {@link String }
   */
  public String getPublicHostname() {
    return publicHostname;
  }

  /**
   * Sets the value of the publicHostname property.
   *
   * @param value allowed object is
   *              {@link String }
   */
  public void setPublicHostname(String value) {
    this.publicHostname = value;
  }

  /**
   * Gets the value of the publicPort property.
   *
   * @return possible object is
   * {@link Integer }
   */
  public Integer getPublicPort() {
    return publicPort;
  }

  /**
   * Sets the value of the publicPort property.
   *
   * @param value allowed object is
   *              {@link Integer }
   */
  public void setPublicPort(Integer value) {
    this.publicPort = value;
  }

  /**
   * Gets the value of the serverConfig property.
   *
   * @return possible object is
   * {@link TcServerConfig }
   */
  public TcServerConfig getServerConfig() {
    return serverConfig;
  }

  /**
   * Sets the value of the serverConfig property.
   *
   * @param value allowed object is
   *              {@link TcServerConfig }
   */
  public void setServerConfig(TcServerConfig value) {
    this.serverConfig = value;
  }

}