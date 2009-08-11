package org.codehaus.mojo.findbugs

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.repository.DefaultArtifactRepository
import org.apache.maven.artifact.resolver.ArtifactResolver
import org.apache.maven.doxia.siterenderer.Renderer
import org.apache.maven.doxia.tools.SiteTool
import org.apache.maven.project.MavenProject
import org.codehaus.groovy.maven.mojo.GroovyMojo
import org.codehaus.plexus.resource.ResourceManager
import org.codehaus.plexus.resource.loader.FileResourceLoader
import org.codehaus.plexus.util.FileUtils

/**
 *
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id: FindbugsOptions.groovy gleclaire $
 */

interface FindbugsOptions {
    /**
     * The name of the Plug-In.
     *
     */
    public static final String PLUGIN_NAME = "findbugs"

    /**
     * The name of the property resource bundle (Filesystem).
     *
     */
    public static final String BUNDLE_NAME = "findbugs"

    /**
     * The key to get the name of the Plug-In from the bundle.
     *
     */
    public static final String NAME_KEY = "report.findbugs.name"

    /**
     * The key to get the description of the Plug-In from the bundle.
     *
     */
    public static final String DESCRIPTION_KEY = "report.findbugs.description"

    /**
     * The key to get the source directory message of the Plug-In from the bundle.
     *
     */
    public static final String SOURCE_ROOT_KEY = "report.findbugs.sourceRoot"

    /**
     * The key to get the source directory message of the Plug-In from the bundle.
     *
     */
    public static final String TEST_SOURCE_ROOT_KEY = "report.findbugs.testSourceRoot"

    /**
     * The key to get the java source message of the Plug-In from the bundle.
     *
     */
    public static final String JAVA_SOURCES_KEY = "report.findbugs.javasources"

    /**
     * The regex pattern to search for java class files.
     *
     */
    public static final String JAVA_REGEX_PATTERN = "**/*.class"

    /**
     * Location where generated html will be created.
     *
     * @parameter default-value="${project.reporting.outputDirectory}"
     * @required
     */

    public File outputDirectory

    /**
     * Turn on and off xml output of the Findbugs report.
     *
     * @parameter default-value="false"
     * @since 1.0.0
     */
    public boolean xmlOutput

    /**
     * Specifies the directory where the xml output will be generated.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     * @since 1.0.0
     */
    public File xmlOutputDirectory

    /**
     * Turn on and off findbugs native xml output of the Findbugs report.
     *
     * @parameter default-value="false"
     * @since 1.2.0
     */
    public boolean findbugsXmlOutput

    /**
     * Specifies the directory where the findbugs native xml output will be generated.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     * @since 1.2.0
     */
    public File findbugsXmlOutputDirectory


    /**
     * Doxia Site Renderer.
     *
     * @parameter expression="${component.org.apache.maven.doxia.siterenderer.Renderer}"
     * @required
     * @readonly
     */
    public Renderer siteRenderer

    /**
     * Directory containing the class files for FindBugs to analyze.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     */
    public File classFilesDirectory

    /**
     * Directory containing the test class files for FindBugs to analyze.
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     * @required
     */
    public File testClassFilesDirectory

    /**
     * Location of the Xrefs to link to.
     *
     * @parameter default-value="${project.reporting.outputDirectory}/xref"
     */
    public File xrefLocation

    /**
     * Location of the Test Xrefs to link to.
     *
     * @parameter default-value="${project.reporting.outputDirectory}/xref-test"
     */
    public File xrefTestLocation

    /**
     * The directories containing the sources to be compiled.
     *
     * @parameter expression="${project.compileSourceRoots}"
     * @required
     * @readonly
     */
    public List compileSourceRoots

    /**
     * The directories containing the test-sources to be compiled.
     *
     * @parameter expression="${project.testCompileSourceRoots}"
     * @required
     * @readonly
     * @since 2.0
     */
    public List testSourceRoots

    /**
     * Run Findbugs on the tests.
     *
     * @parameter default-value="false"
     * @since 2.0
     */
    public boolean includeTests

    /**
     * List of artifacts this plugin depends on. Used for resolving the Findbugs coreplugin.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    public ArrayList pluginArtifacts

    /**
     * The local repository, needed to download the coreplugin jar.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    public DefaultArtifactRepository localRepository

    /**
     * Remote repositories which will be searched for the coreplugin jar.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    public List remoteArtifactRepositories

    /**
     * Maven Project
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    public MavenProject project

    /**
     * Encoding used for xml files. Default value is UTF-8.
     *
     * @parameter default-value="UTF-8"
     * @readonly
     */
    public String xmlEncoding

    /**
     * Threshold of minimum bug severity to report. Valid values are High, Default, Low, Ignore, and Exp (for experimental).
     *
     * @parameter
     */
    public String threshold

    /**
     * Artifact resolver, needed to download the coreplugin jar.
     *
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * @required
     * @readonly
     */
    public ArtifactResolver artifactResolver

    /**
     * File name of the include filter. Only bugs in matching the filters are reported.
     *
     * @parameter
     * @since 1.0-beta-1
     */
    public String includeFilterFile

    /**
     * File name of the exclude filter. Bugs matching the filters are not reported.
     *
     * @parameter
     * @since 1.0-beta-1
     */
    public String excludeFilterFile

    /**
     * Effort of the bug finders. Valid values are Min, Default and Max.
     *
     * @parameter
     * @since 1.0-beta-1
     */
    public String effort

    /**
     * turn on Findbugs debugging
     *
     * @parameter default-value="false"
     */
    public Boolean debug

    /**
     * Relaxed reporting mode. For many detectors, this option suppresses the heuristics used to avoid reporting false
     * positives.
     *
     * @parameter default-value="false"
     * @since 1.1
     */
    public Boolean relaxed

    /**
     * The visitor list to run. This is a comma-delimited list.
     *
     * @parameter
     * @since 1.0-beta-1
     */
    public String visitors

    /**
     * The visitor list to omit. This is a comma-delimited list.
     *
     * @parameter
     * @since 1.0-beta-1
     */
    public String omitVisitors

    /**
     * The plugin list to include in the report. This is a comma-delimited list.
     *
     * @parameter
     * @since 1.0-beta-1
     */
    public String pluginList

    /**
     * Restrict analysis to the given comma-separated list of classes and packages.
     *
     * @parameter
     * @since 1.1
     */
    public String onlyAnalyze

    /**
     * The Flag letting us know if classes have been loaded already.
     *
     * @parameter
     * @readonly
     */
    public static boolean pluginLoaded = false

    /**
     * Skip entire check.
     *
     * @parameter expression="${findbugs.skip}" default-value="false"
     * @since 1.1
     */
    public boolean skip

    /**
     * @component
     * @required
     * @readonly
     * @since 2.0
     */
    public ResourceManager resourceManager

    /**
     * SiteTool.
     *
     * @since 2.1-SNAPSHOT
     * @component role="org.apache.maven.doxia.tools.SiteTool"
     * @required
     * @readonly
     */
    public SiteTool siteTool


    /**
     * Fail the build on an error.
     *
     * @parameter default-value="true"
     * @since 2.0
     */
    public boolean failOnError

    public int bugCount
    public int errorCount
    
}
