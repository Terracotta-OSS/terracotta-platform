package org.terracotta.build.conventions;

import org.gradle.api.Plugin;

/**
 * Marks a plugin as a 'convention plugin' for another plugin.
 * <p>
 * The {@link BaseConvention} will then prevent the use of plugin {@code P} unless this plugin is also applied.

 * @param <T> type of the targeted object
 * @param <P> plugin type this is a convention for
 */
public interface ConventionPlugin<T, P extends Plugin<? extends T>> extends Plugin<T> {

  Class<P> isConventionFor();

}