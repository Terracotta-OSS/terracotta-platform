package org.terracotta.build.conventions;

import com.github.spotbugs.snom.SpotBugsPlugin;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.terracotta.build.Utils;
import org.terracotta.build.plugins.CopyrightPlugin;
import org.terracotta.build.plugins.VoltronPlugin;
import org.terracotta.build.plugins.buildinfo.BuildInfoPlugin;
import org.terracotta.build.plugins.buildinfo.StructuredVersion;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.gradle.util.internal.ConfigureUtil.configureUsing;

/**
 * Convention plugin for the {@code base} plugin.
 * This convention must be applied to every non-empty project, this is enforced
 * by the root project {@code build.gradle}.
 * This convention:
 * <ul>
 * <li>applies the copyright plugin</li>
 * <li>applies the blacklist convention</li>
 * <li>installs and propagates a structured version object to facilitate
 * programmatic version querying</li>
 * <li>requires transitive dependency convergence for all configurations</li>
 * <li>constrains terracotta-utilities-tools and terracotta-utilities-test-tools
 * to their locally requested versions</li>
 * <li>adds and configures the Maven Central, Terracotta, Terracotta Nexus and
 * BAS Artifactory repositories</li>
 * <li>modify transitive slf4j dependencies to conform to the slf4j known
 * project compatibility rules</li>
 * <li>adopt the less collision prone artifact naming convention of
 * {@code "${project.group}.${project.name}"}</li>
 * <li>mark all Java sources as UTF-8, and enable all javac linter warnings,
 * along with warnings as errors</li>
 * <li>configures jar manifests with default attributes</li>
 * <li>adds license to the root of all jars</li>
 * <li>registers an {@code xmlConfig} closure as {@code DependencyHandler}
 * extension</li>
 * </ul>
 */
public class BaseConvention implements ConventionPlugin<Project, BasePlugin> {

  private static final Map<Class<? extends Plugin>, Collection<Class<? extends Plugin>>> KNOWN_CONVENTIONS = new ConcurrentHashMap<>();

  static {
    for (ConventionPlugin<?, ?> conventionPlugin : ServiceLoader.load(ConventionPlugin.class,
        BaseConvention.class.getClassLoader())) {
      KNOWN_CONVENTIONS.computeIfAbsent(conventionPlugin.isConventionFor(), k -> new ArrayList<>())
          .add(conventionPlugin.getClass().asSubclass(Plugin.class));
    }

    /*
     * The CopyrightPlugin is implemented using the CheckstylePlugin.
     * This means we must treat the CopyrightPlugin as a convention for the
     * CheckstylePlugin and all of its super types.
     */
    KNOWN_CONVENTIONS.entrySet().stream().filter(e -> e.getKey().isAssignableFrom(CheckstylePlugin.class))
        .forEach(e -> e.getValue().add(CopyrightPlugin.class));
  }

  @Override
  public Class<BasePlugin> isConventionFor() {
    return BasePlugin.class;
  }

  @Override
  public void apply(Project project) {
    PluginContainer plugins = project.getPlugins();
    plugins.apply(BasePlugin.class);
    plugins.apply(CopyrightPlugin.class);

    plugins.apply(BlacklistConvention.class);
    plugins.apply(BuildInfoPlugin.class);

    setupResolutionStrategy(project);
    setupRepositories(project.getRepositories());
    correctDependencyMetadata(project.getDependencies());
    setupArtifactNamingConvention(project);
    enforceJavaSourceRules(project.getTasks());
    setupJavaArchiveDefaults(project);
    setupDefaultExtensions(project);

    project.getExtensions().configure(CheckstyleExtension.class, checkstyle ->
            checkstyle.getConfigProperties().put("project_config", project.file("config/checkstyle").getAbsolutePath()));
  }

  private static void setupDefaultExtensions(Project project) {
    DependencyHandler dependencies = project.getDependencies();
    dependencies.getExtensions().add("xmlConfig", new Closure<Dependency>(dependencies, dependencies) {

      ModuleDependency doCall(Object notation) {
        Dependency xmlConfigDependency = ((DependencyHandler) getOwner()).create(notation);
        if (xmlConfigDependency instanceof ModuleDependency) {
          return ((ModuleDependency) xmlConfigDependency).capabilities(capabilities -> capabilities
              .requireCapability(VoltronPlugin.xmlConfigFeatureCapability((ModuleDependency) xmlConfigDependency)));
        } else {
          throw new IllegalArgumentException("Unsupported dependency type: " + xmlConfigDependency);
        }
      }

      ModuleDependency doCall(Object notation, Action<? super ModuleDependency> configureAction) {
        ModuleDependency xmlConfigDependency = doCall(notation);
        configureAction.execute(xmlConfigDependency);
        return xmlConfigDependency;
      }

      ModuleDependency doCall(Object notation, Closure configureClosure) {
        return doCall(notation, configureUsing(configureClosure));
      }
    });
  }

  private static void setupJavaArchiveDefaults(Project project) {
    project.getTasks().withType(Jar.class).configureEach(jar -> {
      jar.manifest(manifest -> manifest.attributes(Utils.mapOf(
          "provider", "gradle",
          "Implementation-Title", project.getName(),
          "Implementation-Version", project.getVersion(),
          "Implementation-Vendor-Id", project.getGroup(),
          "Built-By", System.getProperty("user.name"),
          "Built-JDK", System.getProperty("java.version"))));
      // Add license to all jars
      jar.from(project.getRootProject().file("LICENSE"));
    });
  }

  private static void setupArtifactNamingConvention(Project project) {
    project.getPlugins().withType(BasePlugin.class).configureEach(base -> {
      /*
       * Gradle's default convention here is too prone to collisions.
       * I think this approach is immune since it's leaning on a
       * combination that Gradle itself needs to be unique.
       */
      project.getExtensions().configure(BasePluginExtension.class, basePluginExtension -> {
        basePluginExtension.getArchivesName()
            .convention(project.provider(() -> project.getGroup() + "." + project.getName()));
      });
    });
  }

  private static void enforceJavaSourceRules(TaskContainer tasks) {
    tasks.withType(JavaCompile.class).configureEach(compile -> {
      compile.getOptions().setEncoding(StandardCharsets.UTF_8.name());
      compile.getOptions().getCompilerArgs().addAll(asList("-Xlint:all", "-Werror"));
    });
    tasks.withType(Javadoc.class)
        .configureEach(javadoc -> javadoc.getOptions().setEncoding(StandardCharsets.UTF_8.name()));
  }

  private static void setupResolutionStrategy(Project project) {
    ConfigurationContainer configurations = project.getConfigurations();

    /*
     * Spotbugs has a version conflict in its own transitive dependency set.
     * It's easier to just not configure this for SpotBugs than it is to jump
     * through the necessary hoops to exclude things. That and the exact issue
     * changes with every new SpotBugs version.
     */
    configurations.matching(config -> !SpotBugsPlugin.CONFIG_NAME.equals(config.getName()))
        .configureEach(config -> config.getResolutionStrategy().failOnVersionConflict());
  }

  private static void setupRepositories(RepositoryHandler repositories) {
    repositories.mavenCentral();
    repositories.maven(terracotta -> {
      terracotta.setUrl(URI.create("https://repo.terracotta.org/maven2"));
      terracotta.content(c -> {
        c.includeGroupByRegex("org\\.terracotta(?:\\..+)?");
      });
    });
  }

  private static void correctDependencyMetadata(DependencyHandler dependencies) {
    dependencies.components(components -> {
      components.all(details -> {
        /*
         * Adjust <i>transitive</i> dependencies on SLF4J to use a range based on the
         * declared version of SLF4J extending
         * to the "end" of that major.minor series.
         */
        details.allVariants(variantMetadata -> {
          variantMetadata.withDependencies(directDependencyMetadata -> {
            directDependencyMetadata.forEach(dependency -> {
              if (dependency.getGroup().equals("org.slf4j") && dependency.getName().equals("slf4j-api")) {
                dependency.version(versionConstraint -> {
                  /*
                   * Parse a "normal form" required version number (major.minor[.patch]) retaining
                   * only the
                   * major.minor portion and form a Gradle bounded range
                   * [required,major.minor.9999).
                   * Failure to parse results in the version being left as-is.
                   */
                  String requiredVersion = versionConstraint.getRequiredVersion();
                  Matcher matcher = MAJOR_MINOR_PATTERN.matcher(requiredVersion);
                  if (matcher.matches()) {
                    versionConstraint.require(
                        String.format("[%s, %s.%s.9999)", requiredVersion, matcher.group(1), matcher.group(2)));
                  }
                });
              }
            });
          });
        });
      });
    });
  }

  /** Matches major.minor[.patch]. */
  private static final Pattern MAJOR_MINOR_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.\\d+)?");
}
