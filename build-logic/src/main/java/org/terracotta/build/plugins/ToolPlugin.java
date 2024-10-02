package org.terracotta.build.plugins;

import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import org.gradle.api.Action;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.resources.TextResource;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.owasp.dependencycheck.gradle.DependencyCheckPlugin;
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension;
import org.terracotta.build.plugins.CopyrightPlugin;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.file.Files.getFileAttributeView;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.attribute.PosixFilePermissions.fromString;
import static java.util.Collections.nCopies;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.terracotta.build.Utils.mapOf;
import static org.terracotta.build.plugins.packaging.PackageInternal.UNPACKAGED_JAVA_RUNTIME;

/**
 * Plugin to support generating Java-based tools with Windows and *nix launch scripts.
 * <p>
 * Minimal Example:
 * <pre>
 * tool {
 *   name = 'config-tool'
 *   mainClass = 'org.terracotta.dynamic_config.cli.config_tool.ConfigTool'
 * }
 * </pre>
 * This tool will generate a complete directory structure for the tool in question including start scripts and all
 * necessary Java libraries in the configured locations.
 * Maximal Example:
 * <pre>
 * tool {
 *   name = 'management-server'
 *   jar = bootJar
 *   classpath = files([])
 *   unixTemplate = resources.text.fromFile('src/tool/start.sh.template', 'UTF-8')
 *   windowsTemplate = resources.text.fromFile('src/tool/start.bat.template', 'UTF-8')
 *   extraContents {
 *     into('bin') {
 *       from project.files('src/tool/legacy/bin')
 *       eachFile {
 *         mode 0775
 *       }
 *     }
 *   }
 * }
 * </pre>
 */
public class ToolPlugin implements Plugin<Project> {

  public static final String TOOL_CATEGORY = "tool";

  private static final String DEFAULT_LIBRARY_PATH = "lib";
  private static final String DEFAULT_SCRIPT_PATH = "bin";

  private static final URL DEFAULT_UNIX_TEMPLATE = requireNonNull(ToolPlugin.class.getResource("/tool/start.sh.template"));
  private static final URL DEFAULT_WINDOWS_TEMPLATE = requireNonNull(ToolPlugin.class.getResource("/tool/start.bat.template"));

  @Override
  public void apply(Project project) {
    ToolExtension toolExtension = project.getExtensions().create(ToolExtension.class, "tool", ToolExtension.class);
    toolExtension.getBinDirectory().convention(DEFAULT_SCRIPT_PATH);
    toolExtension.getLibDirectory().convention(DEFAULT_LIBRARY_PATH);
    toolExtension.getUnixTemplate().convention(project.getResources().getText().fromUri(DEFAULT_UNIX_TEMPLATE));
    toolExtension.getWindowsTemplate().convention(project.getResources().getText().fromUri(DEFAULT_WINDOWS_TEMPLATE));

    Project rootProject = project.getRootProject();
    rootProject.getPlugins().withType(DependencyCheckPlugin.class).configureEach(depCheckPlugin -> {
      rootProject.getExtensions().configure(DependencyCheckExtension.class, dependencyCheck -> {
        dependencyCheck.getScanProjects().add(project.getPath());
      });
    });

    project.getPlugins().withType(JavaPlugin.class).configureEach(javaPlugin -> {
      project.getExtensions().configure(SourceSetContainer.class, sourceSets -> {
        sourceSets.configureEach(sourceSet -> {
          project.getConfigurations().named(sourceSet.getRuntimeClasspathConfigurationName()).configure(config -> {
            config.attributes(attributes -> {
              attributes.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, UNPACKAGED_JAVA_RUNTIME));
            });
          });
        });
      });

      toolExtension.getClasspath().convention(project.getConfigurations().named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
      toolExtension.getJar().convention(project.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class, t -> {
        Property<FileCollection> classpath = toolExtension.getClasspath();
        t.dependsOn(classpath);
        t.manifest(manifest -> manifest.attributes(mapOf(
                "Main-Class", toolExtension.getMainClass(),
                "Class-Path", classpath.flatMap(conf -> conf.getElements().map(e -> e.stream().map(element -> element.getAsFile().getName()).collect(joining(" "))))
        )));
      }));
    });

    Provider<CreateToolScripts> scriptsTask = project.getTasks().register("generateToolScripts", CreateToolScripts.class, t -> {
      t.getToolName().convention(toolExtension.getName());
      t.getOutputDir().convention(project.getLayout().getBuildDirectory().dir("scripts"));

      t.getUnixTemplate().convention(toolExtension.getUnixTemplate());
      t.getWindowsTemplate().convention(toolExtension.getWindowsTemplate());
      t.getJavaOptions().convention(toolExtension.getJavaOptions());
      t.getJavaArguments().convention(toolExtension.getJavaArguments());

      Provider<String> jarName = toolExtension.getJar().flatMap(jar -> jar.getArchiveFile().map(f -> f.getAsFile().getName()));
      Provider<Path> fromRootJarPath = toolExtension.getLibDirectory().zip(jarName, (path, name) -> Paths.get(path).resolve(name));
      t.getJarPath().convention(toolExtension.getBinDirectory().zip(fromRootJarPath, (bin, jar) -> stream(Paths.get(bin).relativize(jar).spliterator(), false).map(Path::toString).collect(toList())));
    });

    Provider<Sync> assembleTool = project.getTasks().register("assembleTool", Sync.class, sync -> {
      sync.setGroup(LifecycleBasePlugin.BUILD_GROUP);
      sync.into(project.getLayout().getBuildDirectory().dir("tool"));

      sync.into(toolExtension.getLibDirectory(), spec -> {
        spec.from(toolExtension.getJar());
        spec.from(toolExtension.getClasspath());
      });

      sync.into(toolExtension.getBinDirectory(), spec -> spec.from(scriptsTask));

      toolExtension.getExtras().configureEach(action -> sync.with(project.copySpec(action)));
    });

    project.getPlugins().withType(CopyrightPlugin.class).configureEach(plugin ->
            ((ExtensionAware) toolExtension).getExtensions().add("copyright", plugin.createCopyrightSet(project, "tool",
                    copyright -> copyright.check(project.getTasks().withType(CreateToolScripts.class))))
    );

    project.getConfigurations().register("tool", c -> {
      c.setCanBeConsumed(true);
      c.attributes(a -> a.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, TOOL_CATEGORY)));
    });
    project.getArtifacts().add("tool", assembleTool.map(Sync::getDestinationDir));
  }

  public abstract static class ToolExtension {

    public abstract Property<String> getName();

    public abstract Property<String> getMainClass();

    public abstract Property<String> getBinDirectory();

    public abstract Property<String> getLibDirectory();

    public abstract Property<TextResource> getUnixTemplate();

    public abstract Property<TextResource> getWindowsTemplate();

    public abstract Property<AbstractArchiveTask> getJar();

    public abstract Property<FileCollection> getClasspath();

    public abstract DomainObjectSet<Action<CopySpec>> getExtras();

    public void extraContents(Action<CopySpec> action) {
      getExtras().add(action);
    }

    public abstract ListProperty<Object> getJavaOptions();
    public abstract ListProperty<Object> getJavaArguments();

    public static PlatformSensitiveString env(String variable) {
      return os -> os.isWindows() ?  "%" + variable + "%" : "${" + variable + "}";
    }

    public static PlatformSensitiveString path(Object ... elements) {
      return os -> Stream.of(elements).map(o -> stringForPlatform(o, os)).collect(joining(os.isWindows() ? "\\" : "/"));
    }

    public static PlatformSensitiveString argument(Object ... elements) {
      return os -> {
        String argument = Stream.of(elements).map(o -> stringForPlatform(o, os)).collect(joining());
        if (os.isWindows()) {
          return "\"" + argument + "\"";
        } else {
          return argument;
        }
      };
    }
  }

  public abstract static class CreateToolScripts extends ConventionTask {

    @Input
    abstract public Property<String> getToolName();

    @Input
    abstract public ListProperty<String> getJarPath();

    @Input
    public abstract ListProperty<Object> getJavaOptions();

    @Input
    public abstract ListProperty<Object> getJavaArguments();

    @Internal
    abstract public DirectoryProperty getOutputDir();

    @Nested
    abstract public Property<TextResource> getUnixTemplate();

    @Nested
    abstract public Property<TextResource> getWindowsTemplate();

    @OutputFile
    public Provider<RegularFile> getUnixScript() {
      return getOutputDir().file(getToolName().map(name -> name + ".sh"));
    }

    @OutputFile
    public Provider<RegularFile> getWindowsScript() {
      return getOutputDir().file(getToolName().map( name -> name + ".bat"));
    }

    @Internal
    Template getUnixTemplateInstance() {
      return createTemplate(getUnixTemplate().get());
    }

    @Internal
    Template getWindowsTemplateInstance() {
      return createTemplate(getWindowsTemplate().get());
    }

    private final SimpleTemplateEngine engine = new SimpleTemplateEngine();

    private Template createTemplate(TextResource template) {
      try {
        return engine.createTemplate(template.asReader());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @TaskAction
    void generate() throws IOException {
      File unixScript = getUnixScript().get().getAsFile();
      try (Writer writer = newBufferedWriter(unixScript.toPath(), StandardCharsets.UTF_8)) {
        getUnixTemplateInstance().make(variablesFor(OperatingSystem.UNIX)).writeTo(writer);
      }

      /*
       * Windows batch scripts are interpreted in the OEM code page of the executing machine.
       * In order to minimize our chances of seeing issues here we constrain ourselves to US-ASCII only.
       */
      File windowsScript = getWindowsScript().get().getAsFile();
      try (Writer writer = newBufferedWriter(windowsScript.toPath(), StandardCharsets.US_ASCII)) {
        getWindowsTemplateInstance().make(variablesFor(OperatingSystem.WINDOWS)).writeTo(writer);
      }

      PosixFileAttributeView unixScriptAttrs = getFileAttributeView(unixScript.toPath(), PosixFileAttributeView.class);
      if (unixScriptAttrs != null) {
        unixScriptAttrs.setPermissions(fromString("rwxrwxr-x"));
      }
    }

    Map<String, String> variablesFor(OperatingSystem os) {
      List<String> libDirFromBin = getJarPath().get();

      int backTraversals = countLeading("..", libDirFromBin);

      List<String> libFromTop = libDirFromBin.subList(backTraversals, libDirFromBin.size());

      if (libFromTop.contains("..")) {
        throw new IllegalArgumentException("Unexpected back traversal in path: " + libDirFromBin);
      }

      return mapOf(String.class, String.class,
              "jar", ToolExtension.path(libFromTop.toArray()).toString(os),
              "tooldir_evaluation", backTraversals("TOOL_DIR", backTraversals, os),
              "java_opts", getJavaOptions().get().stream().map(o -> stringForPlatform(o, os)).collect(joining(" ")),
              "java_args", getJavaArguments().get().stream().map(o -> stringForPlatform(o, os)).collect(joining(" "))
      );
    }

    private static String backTraversals(String variable, int count, OperatingSystem os) {
      String backTraversal = ToolExtension.path(nCopies(count, "..").toArray()).toString(os);
      String variableSet;
      if (os.isWindows()) {
        variableSet = "pushd \"%%~dp0%2$s\"\r\n" + "set \"%1$s=%%CD%%\"\r\n" + "popd\r\n";
      } else {
        variableSet = "%1$s=\"$(cd \"$(dirname \"$0\")/%2$s\"; pwd)\"\n";
      }
      return String.format(Locale.ROOT, variableSet, variable, backTraversal);
    }

    private static <T> int countLeading(T lead, Iterable<T> elements) {
      int count = 0;
      for (T element : elements) {
        if (lead.equals(element)) {
          count++;
        } else {
          break;
        }
      }
      return count;
    }
  }

  private static String stringForPlatform(Object o, OperatingSystem os) {
    if (o instanceof PlatformSensitiveString) {
      return ((PlatformSensitiveString) o).toString(os);
    } else {
      return o.toString();
    }
  }

  interface PlatformSensitiveString extends Serializable {
    String toString(OperatingSystem os);
  }
}
