package org.terracotta.build.conventions;

import org.terracotta.build.plugins.JavaVersionPlugin;
import org.terracotta.build.plugins.PackagePlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyCollector;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.JvmTestSuitePlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.testing.base.TestingExtension;

/**
 * Convention plugin for the {@code java-base} plugin (a dependency of the {@code java} and {@code java-library} plugins).
 * This convention:
 * <ul>
 *     <li>applies the base convention</li>
 *     <li>applies the checkstyle convention</li>
 *     <li>applies the spotbugs convention</li>
 *     <li>augments dependency attribute schema to understand the 'unpackaged runtime' concept</li>
 *     <li>wires compile tasks to the {@code compileVersion} Gradle property</li>
 *     <li>configures the junit version used (by default) for test suites</li>
 *     <li>adds a set of baseline dependencies for test suites</li>
 *     <li>sets a default maximum heap sizde for test suites</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class JavaBaseConvention implements ConventionPlugin<Project, JavaBasePlugin> {

    @Override
    public void apply(Project project) {
        PluginContainer plugins = project.getPlugins();
        plugins.apply(JavaBasePlugin.class);
        plugins.apply(BaseConvention.class);
        plugins.apply(JavaVersionPlugin.class);
        plugins.apply(CheckstyleConvention.class);
        plugins.apply(SpotBugsConvention.class);

        JavaVersionPlugin.JavaVersions javaVersions = project.getExtensions().getByType(JavaVersionPlugin.JavaVersions.class);

        PackagePlugin.augmentAttributeSchema(project);

        project.getExtensions().configure(JavaPluginExtension.class, java -> {
            java.toolchain(toolchain -> toolchain.getLanguageVersion().convention(javaVersions.getCompileVersion()));
        });

        project.getPlugins().withType(JvmTestSuitePlugin.class).configureEach(plugin -> {
            project.getConfigurations().configureEach(config ->
                    config.getResolutionStrategy().dependencySubstitution(subs -> {
                            subs.substitute(subs.module("org.hamcrest:hamcrest-core:1.3"))
                                    .using(subs.module("org.hamcrest:hamcrest-core:" + project.property("hamcrestVersion")));
                            subs.substitute(subs.module("junit:junit:4.12"))
                                    .using(subs.module("junit:junit:" + project.property("junitVersion")));
                    })
            );

            project.getExtensions().configure(TestingExtension.class, testing -> {
                testing.getSuites().withType(JvmTestSuite.class).configureEach(testSuite -> {
                    testSuite.useJUnit(project.property("junitVersion").toString());

                    DependencyCollector implementation = testSuite.getDependencies().getImplementation();
                    implementation.add("org.hamcrest:hamcrest:" + project.property("hamcrestVersion"));
                    implementation.add("org.hamcrest:hamcrest-core:" + project.property("hamcrestVersion"));
                    implementation.add("org.hamcrest:hamcrest-library:" + project.property("hamcrestVersion"));
                    implementation.add("org.mockito:mockito-core:" + project.property("mockitoVersion"));

                    testSuite.getTargets().configureEach(target -> {
                        target.getTestTask().configure(test -> {
                            test.setMaxHeapSize("256m");

                            // Override default toolchain for tests
                            // acquire a provider that returns the launcher for the toolchain
                            test.getJavaLauncher().set(project.getExtensions().getByType(JavaToolchainService.class)
                                    .launcherFor(spec -> spec.getLanguageVersion().convention(javaVersions.getTestVersion())));
                            test.environment("JAVA_HOME", test.getJavaLauncher().get().getMetadata().getInstallationPath());
                        });
                    });
                });
            });
        });
    }

    @Override
    public Class<JavaBasePlugin> isConventionFor() {
        return JavaBasePlugin.class;
    }
}
