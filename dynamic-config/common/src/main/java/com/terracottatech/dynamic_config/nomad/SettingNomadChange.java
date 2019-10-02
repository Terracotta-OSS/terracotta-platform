/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terracottatech.dynamic_config.model.Operation;
import com.terracottatech.dynamic_config.model.Setting;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Nomad change that supports any dynamic config change (see Cluster-tool.adoc)
 *
 * @author Mathieu Carbou
 */
public class SettingNomadChange extends FilteredNomadChange {

  private final Operation operation;
  private final Setting setting;
  private final String name;
  private final String value;

  @JsonCreator
  private SettingNomadChange(@JsonProperty(value = "applicability", required = true) Applicability applicability,
                             @JsonProperty(value = "operation", required = true) Operation operation,
                             @JsonProperty(value = "setting", required = true) Setting setting,
                             @JsonProperty(value = "name") String name,
                             @JsonProperty(value = "value") String value) {
    super(applicability);
    this.operation = requireNonNull(operation);
    this.setting = requireNonNull(setting);
    this.name = name;
    this.value = value;
  }

  @Override
  public String getSummary() {
    return operation == Operation.SET ?
        name == null ? (operation + " " + setting + "=" + value) : (operation + " " + setting + "." + name + "=" + value) :
        name == null ? (operation + " " + setting) : (operation + " " + setting + "." + name);
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public Setting getSetting() {
    return setting;
  }

  public Operation getOperation() {
    return operation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SettingNomadChange)) return false;
    if (!super.equals(o)) return false;
    SettingNomadChange that = (SettingNomadChange) o;
    return getOperation() == that.getOperation() &&
        getSetting() == that.getSetting() &&
        Objects.equals(getName(), that.getName()) &&
        Objects.equals(getValue(), that.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getOperation(), getSetting(), getName(), getValue());
  }

  @Override
  public String toString() {
    return "SettingNomadChange{" +
        "operation=" + operation +
        ", setting=" + setting +
        ", name='" + name + '\'' +
        ", value='" + value + '\'' +
        ", applicability=" + applicability +
        '}';
  }

  public static SettingNomadChange set(Applicability applicability, Setting type, String name, String value) {
    return new SettingNomadChange(applicability, Operation.SET, type, name, value);
  }

  public static SettingNomadChange unset(Applicability applicability, Setting type, String name) {
    return new SettingNomadChange(applicability, Operation.UNSET, type, name, null);
  }

  public static SettingNomadChange set(Applicability applicability, Setting type, String value) {
    return new SettingNomadChange(applicability, Operation.SET, type, null, value);
  }

  public static SettingNomadChange unset(Applicability applicability, Setting type) {
    return new SettingNomadChange(applicability, Operation.UNSET, type, null, null);
  }

}
