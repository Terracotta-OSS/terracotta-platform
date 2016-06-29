package org.terracotta.offheapresource;

import com.tc.classloader.CommonComponent;

import java.util.Set;

/**
 * Represents a collection of {@link OffHeapResource} instances
 */
@CommonComponent
public interface OffHeapResources {

  Set<String> getAllIdentifiers();

}
