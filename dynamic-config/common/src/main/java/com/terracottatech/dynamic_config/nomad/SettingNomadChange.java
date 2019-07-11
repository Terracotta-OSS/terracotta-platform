/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terracottatech.dynamic_config.ConfigChangeHandler.Type;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Nomad change that supports any dynamic config change (see Cluster-tool.adoc)
 *
 * @author Mathieu Carbou
 */
public class SettingNomadChange extends FilteredNomadChange {

  public enum Cmd {SET, UNSET}

  private final Cmd cmd;
  private final Type configType;
  private final String name;
  private final String value;

  @JsonCreator
  private SettingNomadChange(@JsonProperty("applicability") Applicability applicability,
                             @JsonProperty("cmd") Cmd cmd,
                             @JsonProperty("configType") Type configType,
                             @JsonProperty("name") String name,
                             @JsonProperty("value") String value) {
    super(applicability);
    this.cmd = requireNonNull(cmd);
    this.configType = requireNonNull(configType);
    this.name = requireNonNull(name);
    this.value = value;
  }

  @Override
  public String getSummary() {
    return cmd == Cmd.SET ?
        ("set " + configType + "." + name + "=" + value) :
        ("unset " + configType + "." + name);
  }

  @JsonIgnore
  public String getChange() {
    return cmd == Cmd.SET ? name + "=" + value : name;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public Type getConfigType() {
    return configType;
  }

  public Cmd getCmd() {
    return cmd;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SettingNomadChange)) return false;
    if (!super.equals(o)) return false;
    SettingNomadChange that = (SettingNomadChange) o;
    return getCmd() == that.getCmd() &&
        getName().equals(that.getName()) &&
        Objects.equals(getValue(), that.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getCmd(), getName(), getValue());
  }

  @Override
  public String toString() {
    return "SettingNomadChange{" +
        "cmd=" + cmd +
        ", configType=" + configType +
        ", name='" + name + '\'' +
        ", value='" + value + '\'' +
        ", applicability=" + applicability +
        '}';
  }

  public static SettingNomadChange set(Applicability applicability, Type type, String name, String value) {
    return new SettingNomadChange(applicability, Cmd.SET, type, name, value);
  }

  public static SettingNomadChange unset(Applicability applicability, Type type, String name) {
    return new SettingNomadChange(applicability, Cmd.UNSET, type, name, null);
  }

}
