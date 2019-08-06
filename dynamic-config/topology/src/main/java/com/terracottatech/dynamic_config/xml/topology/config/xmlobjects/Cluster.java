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
 * <p>Java class for cluster complex type.
 * <p>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;complexType name="cluster"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="name" type="{http://www.terracotta.org/config}non-blank-token"/&gt;
 *         &lt;sequence&gt;
 *           &lt;element name="stripe" type="{http://www.terracotta.org/config/cluster}stripe" maxOccurs="unbounded"/&gt;
 *         &lt;/sequence&gt;
 *         &lt;sequence&gt;
 *         &lt;/sequence&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cluster", propOrder = {
    "name",
    "stripes"
})
public class Cluster {

  @XmlAttribute(name = "currentStripeId", required = true)
  protected Integer currentStripeId;
  @XmlElement(required = true)
  @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
  @XmlSchemaType(name = "token")
  protected String name;
  @XmlElement(required = true, name="stripe")
  protected List<Stripe> stripes;

  /**
   * Gets the value of the name property.
   *
   * @return name for the {@code Cluster}
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the value of the name property.
   *
   * @param value is a {@link String }
   */
  public void setName(String value) {
    this.name = value;
  }

  public Integer getCurrentStripeId() {
    return currentStripeId;
  }

  public void setCurrentStripeId(Integer currentStripeId) {
    this.currentStripeId = currentStripeId;
  }

  /**
   * Gets the value of the stripe property.
   * <p>
   * <p>
   * This accessor method returns a reference to the live list,
   * not a snapshot. Therefore any modification you make to the
   * returned list will be present inside the JAXB object.
   * This is why there is not a <CODE>set</CODE> method for the stripe property.
   * <p>
   * <p>
   * For example, to add a new item, do as follows:
   * <pre>
   *    getStripe().add(newItem);
   * </pre>
   * <p>
   * <p>
   * <p>
   * Objects of the following type(s) are allowed in the list
   * {@link Stripe }
   */
  public List<Stripe> getStripes() {
    if (stripes == null) {
      stripes = new ArrayList<>();
    }
    return this.stripes;
  }

}