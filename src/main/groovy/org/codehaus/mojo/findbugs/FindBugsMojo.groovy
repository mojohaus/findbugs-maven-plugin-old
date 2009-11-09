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
import org.codehaus.doxia.site.renderer.SiteRenderer
import org.apache.maven.doxia.tools.SiteTool
import org.apache.maven.project.MavenProject
import org.apache.maven.reporting.AbstractMavenReport
import org.codehaus.plexus.resource.ResourceManager
import org.codehaus.plexus.resource.loader.FileResourceLoader
import groovy.xml.StreamingMarkupBuilder

/**
 * Generates a FindBugs Report when the site plugin is run.
 * The HTML report is generated for site commands only.
 * To see more documentation about FindBugs' options, please see the
 * <a href="http://findbugs.sourceforge.net/manual/index.html">FindBugs Manual.</a>
 *
 * @goal findbugs
 * @execute phase="compile"
 * @requiresDependencyResolution compile
 * @requiresProject
 *
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id$
 */
class FindBugsMojo extends AbstractMavenReport implements FindBugsInfo {

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
     * @deprecated
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
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    SiteRenderer siteRenderer

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
     * is not set, the platform default encoding is used.
     *
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     * @since 2.2
     */
    String sourceEncoding

    /**
     * The file encoding to use when creating the HTML reports. If the property <code>project.reporting.outputEncoding</code>
     * is not set, the platform default encoding is used.
     *
     * @parameter expression="${outputEncoding}" default-value="${project.reporting.outputEncoding}"
     * @since 2.2
     */
    String outputEncoding

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

    ResourceBundle bundle

    private static final RESPONSE_FILE = "findbugs.response"
    private static final EOL = "\n"

    /**
     * Checks whether prerequisites for generating this report are given.
     *
     * @return true if report can be generated, otherwise false
     * @see org.apache.maven.reporting.MavenReport#canGenerateReport()
     */
    boolean canGenerateReport() {

        def canGenerate = false

        log.debug("Inside canGenerateReport.....")

        if ( !skip && classFilesDirectory.exists() ) {

            classFilesDirectory.eachFileRecurse {
                if ( it.name.contains('.class') )
                canGenerate = true
            }
        }

        return canGenerate
    }

    /**
     * Returns the plugins description for the "generated reports" overview page.
     *
     * @param locale
     *            the locale the report should be generated for
     *
     * @return description of the report
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    String getDescription(Locale locale) {
        return getBundle(locale).getString(DESCRIPTION_KEY)

    }

    /**
     * Returns the plugins name for the "generated reports" overview page and the menu.
     *
     * @param locale
     *            the locale the report should be generated for
     *
     * @return name of the report
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    String getName(Locale locale) {
        return getBundle(locale).getString(NAME_KEY)
    }

    /**
     * Returns report output file name, without the extension.
     *
     * Called by AbstractMavenReport.execute() for creating the sink.
     *
     * @return name of the generated page
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    String getOutputName() {
        return PLUGIN_NAME
    }


    /**
     * Executes the generation of the report.
     *
     * Callback from Maven Site Plugin.
     *
     * @param locale he wanted locale to generate the report, could be null.
     *
     * @see org.apache.maven.reporting.MavenReport #executeReport(java.util.Locale)
     */
    void executeReport(Locale locale) {

        if ( canGenerateReport() ) {


            log.debug("****** Executing FindBugsMojo *******")


            resourceManager.addSearchPath(FileResourceLoader.ID, this.project.getFile().getParentFile().getAbsolutePath())
            resourceManager.addSearchPath("url", "")

            resourceManager.setOutputDirectory(new File(this.project.getBuild().getDirectory()))


            log.debug("report Output Directory is " + getReportOutputDirectory())
            log.debug("Output Directory is " + outputDirectory)
            log.debug("Classes Directory is " + classFilesDirectory)

            log.debug("resourceManager outputDirectory is " + resourceManager.outputDirectory)


            log.debug("  Plugin Artifacts to be added ->" + pluginArtifacts.toString())

            if (!findbugsXmlOutputDirectory.exists()) {
                if ( !findbugsXmlOutputDirectory.mkdirs() ) {
                    fail("Cannot create xml output directory")
                }
            }

            File outputFile = new File("${findbugsXmlOutputDirectory}/findbugsXml.xml")

            log.debug("XML outputFile is " + outputFile.getAbsolutePath())
            log.debug("XML output Directory is " + findbugsXmlOutputDirectory.getAbsolutePath())

            executeFindbugs(locale, outputFile)

            if (!outputDirectory.exists()) {
                if ( !outputDirectory.mkdirs() ) {
                    fail("Cannot create html output directory")
                }
            }

            log.debug("Generating Findbugs HTML")


            FindbugsReportGenerator generator = new FindbugsReportGenerator( getSink(), getBundle(locale), this.project.getBasedir(), siteTool)

            boolean isJxrPluginEnabled = isJxrPluginEnabled()

            generator.setIsJXRReportEnabled(isJxrPluginEnabled)

            if ( isJxrPluginEnabled ) {
                generator.setCompileSourceRoots(this.compileSourceRoots)
                generator.setTestSourceRoots(this.testSourceRoots)
                generator.setXrefLocation(this.xrefLocation)
                generator.setXrefTestLocation(this.xrefTestLocation)
                generator.setIncludeTests(this.includeTests)
            }


            generator.setLog(log)

            generator.setThreshold(threshold)

            generator.setEffort(effort)

            generator.setFindbugsResults(new XmlSlurper().parse(outputFile))


            generator.setOutputDirectory(new File(outputDirectory.getAbsolutePath()))

            generator.generateReport()




            if ( xmlOutput ) {
                log.debug("  Using the xdoc format")

                if ( !xmlOutputDirectory.exists() ) {
                    if ( !xmlOutputDirectory.mkdirs() ) {
                        fail("Cannot create xdoc output directory")
                    }
                }

                XDocsReporter xDocsReporter = new XDocsReporter(getBundle(locale), log, threshold, effort, outputEncoding )
                xDocsReporter.setOutputWriter(new OutputStreamWriter(new FileOutputStream(new File("${xmlOutputDirectory}/findbugs.xml")), outputEncoding))
                xDocsReporter.setFindbugsResults(new XmlSlurper().parse(outputFile))
                xDocsReporter.setCompileSourceRoots(this.compileSourceRoots)

                xDocsReporter.generateReport()
            }
        }

    }

    /**
     * Returns the report output directory.
     *
     * Called by AbstractMavenReport.execute() for creating the sink.
     *
     * @return full path to the directory where the files in the site get copied to
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory() {
        return outputDirectory.getAbsolutePath()
    }

    /**
     * Return the project.
     *
     * @return the project.
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject() {
        return this.project
    }

    /**
     * Return the Sire Renderer.
     *
     * @return the Render used for generating the site.
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer() {
        return this.siteRenderer
    }

    /**
     * Determines if the JXR-Plugin is included in the report section of the POM.
     *
     * @param bundle
     *            The bundle to load the artifactIf of the jxr plugin.
     * @return True if the JXR-Plugin is included in the POM, false otherwise.
     *
     */
    protected boolean isJxrPluginEnabled() {
        boolean isEnabled = false


        List reportPlugins = getProject().getReportPlugins()

        reportPlugins.each() {reportPlugin ->
            if ( "maven-jxr-plugin".equals(reportPlugin.getArtifactId()) || "jxr-maven-plugin".equals(reportPlugin.getArtifactId()) ) {
                isEnabled = true
            }
        }
        return isEnabled
    }


    ResourceBundle getBundle(locale) {

        if ( bundle == null ) {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale)
        }
        return bundle
    }

    /**
     * Set up and run the Findbugs engine.
     *
     * @param locale
     *            the locale the report should be generated for
     *
     */
    public void executeFindbugs(Locale locale, File outputFile) {

        File tempFile = new File("${project.build.directory}/findbugsTemp.xml")

        File responseFile = new File("${project.build.directory}/" + RESPONSE_FILE)

        if (!outputEncoding) { outputEncoding = "UTF-8"}

        log.debug("****** Executing FindBugsMojo *******")

        resourceManager.addSearchPath(FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath())
        resourceManager.addSearchPath("url", "")

        resourceManager.setOutputDirectory(new File(project.getBuild().getDirectory()))

        log.debug("resourceManager outputDirectory is " + resourceManager.outputDirectory)



        def auxClasspathElements = project.compileClasspathElements

        log.info("  Plugin Artifacts to be added ->" + pluginArtifacts.toString())

        log.debug("outputFile is " + outputFile.getAbsolutePath())
        log.debug("output Directory is " + findbugsXmlOutputDirectory.getAbsolutePath())

        log.debug("Temp File is " + tempFile.getAbsolutePath())

        def ant = new AntBuilder()

        ant.java(classname: "edu.umd.cs.findbugs.FindBugs2", fork: "true", failonerror: "false", clonevm: "true", timeout: "${timeout}", maxmemory: "${maxHeap}m")
        {

            def effectiveEncoding = System.getProperty( "file.encoding", "UTF-8" )

            if ( sourceEncoding ) { effectiveEncoding = sourceEncoding }

            log.debug("File Encoding is " + effectiveEncoding)

            sysproperty(key: "file.encoding" , value: effectiveEncoding)
            
            arg(value: "-xml:withMessages")

            arg(value: getEffortParameter())
            arg(value: getThresholdParameter())

            if ( debug ) {
                arg(value: "-progress")
            }

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


            arg(value: "-output")
            arg(value: tempFile.getAbsolutePath())

            def auxClasspath = ""

            pluginArtifacts.each() {pluginArtifact ->
                log.debug("  Adding to AuxClasspath ->" + pluginArtifact.file.toString())

                auxClasspath += pluginArtifact.file.toString() + ((pluginArtifact == pluginArtifacts[pluginArtifacts.size() - 1]) ? "" : File.pathSeparator)
            }

            if (auxClasspathElements) {

                log.debug("  AuxClasspath Elements ->" + auxClasspathElements)


                def auxClasspathList = auxClasspathElements.findAll{project.build.outputDirectory != it.toString()}

                if (auxClasspathList.size() > 0) {

                    auxClasspath += File.pathSeparator

                    log.debug("  Last AuxClasspath is ->" + auxClasspathList[auxClasspathList.size() - 1] )

                    auxClasspathList.each() {auxClasspathElement ->

                        log.debug("  Adding to AuxClasspath ->" + auxClasspathElement.toString())

                        auxClasspath += auxClasspathElement.toString() + ((auxClasspathElement == auxClasspathList[auxClasspathList.size() - 1]) ? "" : File.pathSeparator)
                    }


                }

            }

            log.info("  AuxClasspath is ->" + auxClasspath)
            arg(value: "-auxclasspath")
            arg(value: "@" + responseFile.absolutePath)

            responseFile << auxClasspath + EOL

            classpath()
            {


                pluginArtifacts.each() {pluginArtifact ->
                    log.debug("  Adding to pluginArtifact ->" + pluginArtifact.file.toString())

                    pathelement(location: pluginArtifact.file)
                }
            }

            log.debug("  Adding Source Directory: " + classFilesDirectory.getAbsolutePath())
            arg(value: classFilesDirectory.getAbsolutePath())


        }


        def path = new XmlSlurper().parse(tempFile)

        def allNodes = path.depthFirst().collect { it }

        bugCount = allNodes.findAll {it.name() == 'BugInstance'}.size()
        log.debug("BugInstance size is ${bugCount}")

        errorCount = allNodes.findAll {it.name() == 'Error'}.size()
        log.debug("Error size is ${errorCount}")



        def xmlProject = path.Project

        compileSourceRoots.each() { compileSourceRoot ->
            xmlProject.appendNode {
                SrcDir(compileSourceRoot)
            }
        }

        path.FindbugsResults.FindBugsSummary.'total_bugs' = bugCount   // Fixes visitor problem

        xmlProject.appendNode {
            WrkDir(project.build.directory)
        }

        def xmlBuilder = new StreamingMarkupBuilder()

        if (outputFile.exists()) outputFile.write "\n"
        
        outputFile << xmlBuilder.bind{ mkp.yield path }

        tempFile.delete()

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

        String location = null
        String artifact = resource

        if ( resource.indexOf('/') != -1 ) {
            artifact = resource.substring(resource.lastIndexOf('/') + 1)
        }

        if ( resource.indexOf('/') != -1 ) {
            location = resource.substring(0, resource.lastIndexOf('/'))
        }

        log.debug("resource is " + resource)
        log.debug("location is " + location)
        log.debug("artifact is " + artifact)

        File resourceFile = resourceManager.getResourceAsFile(resource, artifact)

        log.debug("location of configFile file is " + resourceFile)

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
    

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#setReportOutputDirectory(java.io.File)
     */
    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        super.setReportOutputDirectory( reportOutputDirectory )
        this.outputDirectory = reportOutputDirectory
    }

}
