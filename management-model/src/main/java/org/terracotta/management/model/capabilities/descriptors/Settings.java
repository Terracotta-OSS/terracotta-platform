package org.terracotta.management.model.capabilities.descriptors;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Mathieu Carbou
 */
public class Settings extends AbstractMap<String, Object> implements Descriptor, Serializable {

  private static final long serialVersionUID = 1;

  private final Map<String, Object> map = new LinkedHashMap<String, Object>();

  public Settings() {
  }

  public Settings(Map<String, String> map) {
    this.map.putAll(map);
  }

  public Settings(Settings settings) {
    this.map.putAll(settings.map);
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return map.entrySet();
  }

  public <T> T get(String key, Class<T> type) {
    return type.cast(map.get(key));
  }

  public <T> T getOrDefault(String key, Class<T> type, T def) {
    T o = get(key, type);
    return o == null ? def : o;
  }

  public String getString(String key) {
    return get(key, String.class);
  }

  public String getStringOrDefault(String key, String def) {
    return getOrDefault(key, String.class, def);
  }

  public Number getNumber(String key) {
    return get(key, Number.class);
  }

  public Number getNumberOrDefault(String key, Number def) {
    return getOrDefault(key, Number.class, def);
  }

  public boolean getBool(String key) {
    return get(key, Boolean.class);
  }

  public boolean getBoolOrDefault(String key, boolean def) {
    return getOrDefault(key, Boolean.class, def);
  }

  public Settings set(String key, Settings settings) {
    map.put(key, settings);
    return this;
  }

  public Settings set(String key, String value) {
    map.put(key, value);
    return this;
  }

  public Settings set(String key, Number value) {
    map.put(key, value);
    return this;
  }

  public Settings set(String key, boolean value) {
    map.put(key, value);
    return this;
  }

  public Settings set(String key, Enum<?> value) {
    return set(key, value == null ? null : value.name());
  }

  public Settings set(String key, Class<?> value) {
    return set(key, value == null ? null : value.getName());
  }

  public <T> Settings with(String key, T object, Builder<T> builder) {
    Settings child = new Settings();
    map.put(key, child);
    builder.build(child, object);
    return this;
  }

  public <T> Settings withEach(String containerKey, Collection<T> list, Builder<T> builder) {
    Settings container = new Settings();
    map.put(containerKey, container);
    int i = 0;
    for (T o : list) {
      Settings child = new Settings();
      container.set(String.valueOf(i++), child);
      builder.build(child, o);
    }
    return this;
  }

  public <V> Settings withEach(String containerKey, Map<String, V> map, Builder<V> builder) {
    Settings container = new Settings();
    this.map.put(containerKey, container);
    for (Map.Entry<String, V> entry : map.entrySet()) {
      Settings child = new Settings();
      container.set(entry.getKey(), child);
      builder.build(child, entry.getValue());
    }
    return this;
  }

  public interface Builder<T> {
    void build(Settings settings, T object);
  }

}
