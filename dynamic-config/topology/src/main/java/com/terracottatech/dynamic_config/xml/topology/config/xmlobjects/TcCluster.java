/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml.topology.config.xmlobjects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for tc-cluster complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tc-cluster"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="name" type="{http://www.terracotta.org/config}non-blank-token"/&gt;
 *         &lt;sequence&gt;
 *           &lt;element name="stripe" type="{http://www.terracotta.org/config/cluster}tc-stripe" maxOccurs="unbounded"/&gt;
 *         &lt;/sequence&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="currentStripeId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tc-cluster", propOrder = {
    "name",
    "stripes"
})
public class TcCluster {

  @XmlElement(required = true)
  @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
  @XmlSchemaType(name = "token")
  protected String name;
  @XmlElement(required = true, name="stripe")
  protected List<TcStripe> stripes;
  @XmlAttribute(name = "currentStripeId", required = true)
  protected int currentStripeId;

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
   * Gets the value of the stripe property.
   *
   * <p>
   * This accessor method returns a reference to the live list,
   * not a snapshot. Therefore any modification you make to the
   * returned list will be present inside the JAXB object.
   * This is why there is not a <CODE>set</CODE> method for the stripe property.
   *
   * <p>
   * For example, to add a new item, do as follows:
   * <pre>
   *    getStripe().add(newItem);
   * </pre>
   *
   *
   * <p>
   * Objects of the following type(s) are allowed in the list
   * {@link TcStripe }
   */
  public List<TcStripe> getStripes() {
    if (stripes == null) {
      stripes = new ArrayList<TcStripe>();
    }
    return this.stripes;
  }

  /**
   * Gets the value of the currentStripeId property.
   */
  public int getCurrentStripeId() {
    return currentStripeId;
  }

  /**
   * Sets the value of the currentStripeId property.
   */
  public void setCurrentStripeId(int value) {
    this.currentStripeId = value;
  }

}