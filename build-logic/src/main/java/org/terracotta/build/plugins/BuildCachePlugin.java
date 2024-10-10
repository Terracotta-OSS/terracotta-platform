package org.terracotta.build.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.caching.http.HttpBuildCache;

/**
 * Build Cache Settings Plugin.
 * <p>
 * CI is allowed to push to the build cache.
 * Developer workstations can only make use of it.
 */
public class BuildCachePlugin implements Plugin<Settings> {
  @Override
  public void apply(Settings settings) {
    boolean isCiServer = System.getenv().containsKey("JOB_NAME");

    BuildCacheConfiguration buildCache = settings.getBuildCache();

    buildCache.local(local -> {
      local.setEnabled(!isCiServer);
    });
    buildCache.remote(HttpBuildCache.class, remote -> {
      remote.setEnabled(isCiServer);
      remote.setUrl(System.getProperty("cache.url", "http://tc-perf-g5-08.eur.ad.sag:5071/cache/"));
      remote.setAllowInsecureProtocol(true);
      remote.setPush(isCiServer || System.getenv().containsKey("cache.push"));
    });
  }
}
