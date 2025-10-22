/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.build.conventions;

import aQute.bnd.gradle.BundleTaskExtension;
import aQute.bnd.gradle.BndBuilderPlugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.bundling.Jar;

import java.util.Collections;

/**
 * Convention plugin for automatically adding Export-Package instructions to OSGi bundles.
 */
public class OsgiPackageConvention implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(BndBuilderPlugin.class);
        project.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class, task -> {
            task.getExtensions().configure(BundleTaskExtension.class, bundle -> {
                // Add Export-Package instructions with wildcards for all root packages
                bundle.bnd(Collections.singletonMap("-exportcontents", "*"));
            });
		});
    }
}
