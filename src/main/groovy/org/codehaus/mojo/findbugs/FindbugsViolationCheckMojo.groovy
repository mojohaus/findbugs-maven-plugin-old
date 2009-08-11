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
 * Fail the build if there were any FindBugs violations in the source code.
 * An XML report is put out by default in the target directory with the errors.
 * To see more documentation about FindBugs' options, please see the
 * <a href="http://findbugs.sourceforge.net/manual/index.html">FindBugs Manual.</a>
 *
 *
 * @since 2.0
 * @goal check
 * @phase verify
 *
 * @requiresDependencyResolution compile
 * @requiresProject
 *
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id: FindbugsViolationCheckMojo.groovy gleclaire $
 */

class FindbugsViolationCheckMojo extends GroovyMojo implements FindBugsInfo {
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

    File outputDirectory

    /**
     * Turn on and off xml output of the Findbugs report.
     *
     * @parameter default-value="false"
     * @since 1.0.0
     */
    boolean xmlOutput

    /**
     * Specifies the directory where the xml output will be generated.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     * @since 1.0.0
     */
    File xmlOutputDirectory

    /**
     * Turn on and off findbugs native xml output of the Findbugs report.
     *
     * @parameter default-value="false"
     * @since 1.2.0
     */
    boolean findbugsXmlOutput

    /**
     * Specifies the directory where the findbugs native xml output will be generated.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     * @since 1.2.0
     */
    File findbugsXmlOutputDirectory


    /**
     * Doxia Site Renderer.
     *
     * @parameter expression="${component.org.apache.maven.doxia.siterenderer.Renderer}"
     * @required
     * @readonly
     */
    Renderer siteRenderer

    /**
     * Directory containing the class files for FindBugs to analyze.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     */
    File classFilesDirectory

    /**
     * Directory containing the test class files for FindBugs to analyze.
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     * @required
     */
    File testClassFilesDirectory

    /**
     * Location of the Xrefs to link to.
     *
     * @parameter default-value="${project.reporting.outputDirectory}/xref"
     */
    File xrefLocation

    /**
     * Location of the Test Xrefs to link to.
     *
     * @parameter default-value="${project.reporting.outputDirectory}/xref-test"
     */
    File xrefTestLocation

    /**
     * The directories containing the sources to be compiled.
     *
     * @parameter expression="${project.compileSourceRoots}"
     * @required
     * @readonly
     */
    List compileSourceRoots

    /**
     * The directories containing the test-sources to be compiled.
     *
     * @parameter expression="${project.testCompileSourceRoots}"
     * @required
     * @readonly
     * @since 2.0
     */
    List testSourceRoots

    /**
     * Run Findbugs on the tests.
     *
     * @parameter default-value="false"
     * @since 2.0
     */
    boolean includeTests

    /**
     * List of artifacts this plugin depends on. Used for resolving the Findbugs coreplugin.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    ArrayList pluginArtifacts

    /**
     * The local repository, needed to download the coreplugin jar.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    DefaultArtifactRepository localRepository

    /**
     * Remote repositories which will be searched for the coreplugin jar.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    List remoteArtifactRepositories

    /**
     * Maven Project
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    MavenProject project

    /**
     * Encoding used for xml files. Default value is UTF-8.
     *
     * @parameter default-value="UTF-8"
     * @readonly
     */
    String xmlEncoding

    /**
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used. <strong>Note:</strong> This parameter always overrides the
     * property <code>charset</code> from Checkstyle's <code>TreeWalker</code> module.
     *
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     * @since 2.2
     */
    String encoding

    /**
     * Threshold of minimum bug severity to report. Valid values are High, Default, Low, Ignore, and Exp (for experimental).
     *
     * @parameter
     */
    String threshold

    /**
     * Artifact resolver, needed to download the coreplugin jar.
     *
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * @required
     * @readonly
     */
    ArtifactResolver artifactResolver

    /**
     * File name of the include filter. Only bugs in matching the filters are reported.
     *
     * @parameter
     * @since 1.0-beta-1
     */
    String includeFilterFile

    /**
     * File name of the exclude filter. Bugs matching the filters are not reported.
     *
     * @parameter
     * @since 1.0-beta-1
     */
    String excludeFilterFile

    /**
     * Effort of the bug finders. Valid values are Min, Default and Max.
     *
     * @parameter
     * @since 1.0-beta-1
     */
    String effort

    /**
     * turn on Findbugs debugging
     *
     * @parameter default-value="false"
     */
    Boolean debug

    /**
     * Relaxed reporting mode. For many detectors, this option suppresses the heuristics used to avoid reporting false
     * positives.
     *
     * @parameter default-value="false"
     * @since 1.1
     */
    Boolean relaxed

    /**
     * The visitor list to run. This is a comma-delimited list.
     *
     * @parameter
     * @since 1.0-beta-1
     */
    String visitors

    /**
     * The visitor list to omit. This is a comma-delimited list.
     *
     * @parameter
     * @since 1.0-beta-1
     */
    String omitVisitors

    /**
     * The plugin list to include in the report. This is a comma-delimited list.
     *
     * @parameter
     * @since 1.0-beta-1
     */
    String pluginList

    /**
     * Restrict analysis to the given comma-separated list of classes and packages.
     *
     * @parameter
     * @since 1.1
     */
    String onlyAnalyze

    /**
     * The Flag letting us know if classes have been loaded already.
     *
     * @parameter
     * @readonly
     */
    static boolean pluginLoaded = false

    /**
     * Skip entire check.
     *
     * @parameter expression="${findbugs.skip}" default-value="false"
     * @since 1.1
     */
    boolean skip

    /**
     * @component
     * @required
     * @readonly
     * @since 2.0
     */
    ResourceManager resourceManager

    /**
     * SiteTool.
     *
     * @since 2.1-SNAPSHOT
     * @component role="org.apache.maven.doxia.tools.SiteTool"
     * @required
     * @readonly
     */
    protected SiteTool siteTool

    /**
     * Fail the build on an error.
     *
     * @parameter default-value="true"
     * @since 2.0
     */
    boolean failOnError

    /**
     * Maximum Java heap size in megabytes  (default=512).
     *
     * @parameter default-value="512"
     * @since 2.2
     */
    int maxHeap

    /**
     * Specifies the amount of time, in milliseconds, that FindBugs may run before
     *  it is assumed to be hung and is terminated.
     * The default is 600,000 milliseconds, which is ten minutes.
     *
     * @parameter default-value="600000"
     * @since 2.2
     */
    int timeout

    int bugCount
    int errorCount


    void execute() {
        Locale locale = Locale.getDefault()
        List sourceFiles

        log.info("Excecuting findbugs:check")

        if ( this.classFilesDirectory.exists() && this.classFilesDirectory.isDirectory() ) {
            sourceFiles = FileUtils.getFiles(classFilesDirectory, JAVA_REGEX_PATTERN, null)
        }

        if ( !skip && sourceFiles ) {

            // this goes

            log.info("Here goes...............Excecuting findbugs:check")

            if (!findbugsXmlOutputDirectory.exists()) {
                findbugsXmlOutputDirectory.mkdirs()
            }

            File outputFile = new File("${findbugsXmlOutputDirectory}/findbugsCheck.xml")

            executeFindbugs(locale, outputFile)

            println '**********************************'
            println "Checking Findbugs Native XML file"
            println '**********************************'

            def path = new XmlSlurper().parse(outputFile)

            def allNodes = path.depthFirst().collect { it }
            def bugCount = allNodes.findAll {it.name() == 'BugInstance'}.size()
            log.debug("BugInstance size is ${bugCount}")

            def errorCount = allNodes.findAll {it.name() == 'Error'}.size()
            log.debug("Error size is ${errorCount}")


            if ( (bugCount || errorCount) && failOnError ) {
                fail("failed with ${bugCount} bugs and ${errorCount} errors ")
            }

        }
        else {
            log.debug("Nothing for FindBugs to do here.")
        }
    }

    /**
     * Set up and run the Findbugs engine.
     *
     * @param locale
     *            the locale the report should be generated for
     *
     */
    public void executeFindbugs(Locale locale, File outputFile) {


        resourceManager.addSearchPath(FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath())
        resourceManager.addSearchPath("url", "")

        resourceManager.setOutputDirectory(new File(project.getBuild().getDirectory()))

        log.debug("resourceManager outputDirectory is " + resourceManager.outputDirectory)



        def auxClasspathElements = project.compileClasspathElements

        if ( debug ) {
            log.debug("  Plugin Artifacts to be added ->" + pluginArtifacts.toString())
        }

        log.debug("  Plugin Artifacts to be added ->" + pluginArtifacts.toString())

        log.debug("outputFile is " + outputFile.getAbsolutePath())
        log.debug("output Directory is " + findbugsXmlOutputDirectory.getAbsolutePath())

        def ant = new AntBuilder()

        ant.java(classname: "edu.umd.cs.findbugs.FindBugs2", fork: "true", failonerror: "${failOnError}", clonevm: "true", timeout: "${timeout}", maxmemory: "${maxHeap}m")
        {

            def effectiveEncoding = System.getProperty( "file.encoding", "UTF-8" )

            if ( encoding ) { effectiveEncoding = encoding }

            log.info("File Encoding is " + effectiveEncoding)

            sysproperty(key: "file.encoding" , value: effectiveEncoding)

            arg(value: "-xml:withMessages")

            arg(value: "-projectName")
            arg(value: "${project.name}")

            arg(value: "-output")
            arg(value: outputFile.getAbsolutePath())

            arg(value: getEffortParameter())
            arg(value: getThresholdParameter())

            //if ( debug ) arg(value: "-debug")
            arg(value: "-progress")

            if ( pluginList ) {
                arg(value: "-pluginList")
                arg(value: getPlugins())
            }


            if ( visitors ) {
                arg(value: "-visitors")
                arg(value: visitors)
            }

            if ( omitVisitors ) {
                arg(value: "-omitVisitors")
                arg(value: omitVisitors)
            }

            if ( relaxed ) {
                arg(value: "-relaxed")
            }


            if ( onlyAnalyze ) {
                arg(value: "-onlyAnalyze")
                arg(value: onlyAnalyze)
            }


            if ( includeFilterFile ) {
                arg(value: "-include")
                arg(value: getResourceFile(includeFilterFile))
            }

            if ( excludeFilterFile ) {
                arg(value: "-exclude")
                arg(value: getResourceFile(excludeFilterFile))
            }

            classpath()
            {

                auxClasspathElements.each() {auxClasspathElement ->
                    log.debug("  Trying to Add to AuxClasspath ->" + auxClasspathElement.toString())
                    pathelement(location: auxClasspathElement.toString())
                }

                pluginArtifacts.each() {pluginArtifact ->
                    if ( debug ) {
                        log.debug("  Trying to Add to pluginArtifact ->" + pluginArtifact.file.toString())
                    }

                    pathelement(location: pluginArtifact.file)
                }
            }

            log.debug("  Adding Source Directory: " + classFilesDirectory.getAbsolutePath())
            arg(value: classFilesDirectory.getAbsolutePath())


        }


        def path = new XmlSlurper().parse(outputFile)

        def allNodes = path.depthFirst().collect { it }

        bugCount = allNodes.findAll {it.name() == 'BugInstance'}.size()
        log.debug("BugInstance size is ${bugCount}")

        errorCount = allNodes.findAll {it.name() == 'Error'}.size()
        log.debug("Error size is ${errorCount}")

    }

    /**
     * Returns the threshold parameter to use.
     *
     * @return A valid threshold parameter.
     *
     */
    protected String getThresholdParameter() {

        String thresholdParameter

        switch ( threshold ) {
            case threshold = "High":
            thresholdParameter = "-high"; break

            case threshold = "Exp":
            thresholdParameter = "-experimental"; break

            case threshold = "Low":
            thresholdParameter = "-low"; break

            case threshold = "high":
            thresholdParameter = "-high"; break

            default:
            thresholdParameter = "-medium"; break
        }
        return thresholdParameter

    }

    /**
     * Returns the effort parameter to use.
     *
     * @return A valid effort parameter.
     *
     */
    protected String getEffortParameter() {
        String effortParameter

        switch ( effort ) {
            case effort = "Max":
            effortParameter = "max"; break

            case effort = "Min":
            effortParameter = "min"; break

            default:
            effortParameter = "default"; break
        }

        return "-effort:" + effortParameter
    }

    /**
     * Get the File reference for a File passed in as a string reference.
     *
     * @param resource
     *            The file for the resource manager to locate
     * @return The File of the resource
     *
     */
    protected File getResourceFile(String resource) {

        assert resource

        String location = resource

        if ( location.indexOf('/') != -1 ) {
            location = location.substring(location.lastIndexOf('/') + 1)
        }

        log.debug("location of include file is " + location)


        File resourceFile = resourceManager.getResourceAsFile(resource, location)

        println "location of configFile file is " + resourceFile

        return resourceFile

    }

    /**
     * Adds the specified plugins to findbugs. The coreplugin is always added first.
     *
     */
    protected String getPlugins() {
        URL[] pluginURL

        def urlPlugins = ""

        if ( pluginList ) {
            log.debug("  Adding Plugins ")
            String[] pluginJars = pluginList.split(",")

            pluginJars.each() {pluginJar ->
                def pluginFileName = pluginJar.trim()

                if ( !pluginFileName.endsWith(".jar") ) {
                    throw new IllegalArgumentException("Plugin File is not a Jar file: " + pluginFileName)
                }

                try {
                    log.debug("  Processing Plugin: " + pluginFileName.toString())

                    if ( urlPlugins ) {
                        urlPlugins = urlPlugins + "," + new File(pluginFileName.toString()).toURL().toString()
                    } else {
                        urlPlugins = new File(pluginFileName.toString()).toURL().toString()
                    }
                }
                catch (MalformedURLException exception) {
                    fail("The addin plugin has an invalid URL", exception)
                }
            }
        }


        return urlPlugins
    }


}
