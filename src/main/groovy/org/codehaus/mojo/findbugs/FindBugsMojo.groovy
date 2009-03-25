                                                                                           
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

import org.apache.maven.artifact.DependencyResolutionRequiredException
import org.apache.maven.artifact.repository.DefaultArtifactRepository
import org.apache.maven.artifact.resolver.ArtifactNotFoundException
import org.apache.maven.artifact.resolver.ArtifactResolutionException
import org.apache.maven.artifact.resolver.ArtifactResolver
import org.apache.maven.doxia.sink.Sink
import org.apache.maven.doxia.siterenderer.Renderer
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import org.apache.maven.reporting.AbstractMavenReport

import org.codehaus.plexus.resource.ResourceManager
import org.codehaus.plexus.resource.loader.FileResourceLoader
import org.codehaus.plexus.util.FileUtils

import edu.umd.cs.findbugs.BugReporter
import edu.umd.cs.findbugs.ClassScreener
import edu.umd.cs.findbugs.DetectorFactory
import edu.umd.cs.findbugs.DetectorFactoryCollection
import edu.umd.cs.findbugs.Project
import edu.umd.cs.findbugs.TextUIBugReporter
import edu.umd.cs.findbugs.XMLBugReporter

import edu.umd.cs.findbugs.config.UserPreferences
import edu.umd.cs.findbugs.filter.FilterException

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
class FindBugsMojo extends AbstractMavenReport
{

  /**
   * The name of the Plug-In.
   *
   */
  static final String PLUGIN_NAME = "findbugs"

  /**
   * The name of the property resource bundle (Filesystem).
   *
   */
  static final String BUNDLE_NAME = "findbugs"

  /**
   * The key to get the name of the Plug-In from the bundle.
   *
   */
  static final String NAME_KEY = "report.findbugs.name"

  /**
   * The key to get the description of the Plug-In from the bundle.
   *
   */
  static final String DESCRIPTION_KEY = "report.findbugs.description"

  /**
   * The key to get the source directory message of the Plug-In from the bundle.
   *
   */
  static final String SOURCE_ROOT_KEY = "report.findbugs.sourceRoot"

  /**
   * The key to get the source directory message of the Plug-In from the bundle.
   *
   */
  static final String TEST_SOURCE_ROOT_KEY = "report.findbugs.testSourceRoot"

  /**
   * The key to get the java source message of the Plug-In from the bundle.
   *
   */
  static final String JAVA_SOURCES_KEY = "report.findbugs.javasources"

  /**
   * The regex pattern to search for java class files.
   *
   */
  static final String JAVA_REGEX_PATTERN = "**/*.class"

  /**
   * The key to get the jxr-plugin artifactId from the bundle.
   *
   */
  static final String JXR_ARTIFACT_ID_KEY = "report.findbugs.jxrplugin.artifactid"

  /**
   * The name of the coreplugin.
   *
   */
  //    static final String FINDBUGS_COREPLUGIN = "report.findbugs.coreplugin"

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
   * Set whether textual messages should be added to any generated XML of the Findbugs native XML report.
   *
   * @parameter default-value="false"
   * @since 1.2.0
   */
  boolean findbugsXmlWithMessages

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
   * The Base FindBugs reporter Class for reports.
   *
   * @parameter
   * @readonly
   */
  BugReporter bugReporter

  /**
   * Restrict analysis to the given comma-separated list of classes and packages.
   *
   * @parameter
   * @since 1.1
   */
  String onlyAnalyze

  /**
   * The Base FindBugs reporter Class for reports.
   *
   * @parameter
   * @readonly
   */
  ClassScreener classScreener = new ClassScreener()

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
   * Checks whether prerequisites for generating this report are given.
   *
   * @return true if report can be generated, otherwise false
   * @see org.apache.maven.reporting.MavenReport#canGenerateReport()
   */
  boolean canGenerateReport()
  {

    if ( !skip  && classFilesDirectory.exists() )
    {
      def canGenerate = false

      classFilesDirectory.eachFileRecurse {
        if (it.name.contains('.class'))
        canGenerate = true
      }

      return canGenerate
    }
    else
    {
      return false
    }
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
  String getDescription( Locale locale )
  {
    return FindBugsMojo.getBundle( locale ).getString( FindBugsMojo.DESCRIPTION_KEY )

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
  String getName( Locale locale )
  {
    return FindBugsMojo.getBundle( locale ).getString( FindBugsMojo.NAME_KEY )
  }

  /**
   * Returns report output file name, without the extension.
   *
   * Called by AbstractMavenReport.execute() for creating the sink.
   *
   * @return name of the generated page
   * @see org.apache.maven.reporting.MavenReport#getOutputName()
   */
  String getOutputName()
  {
    return FindBugsMojo.PLUGIN_NAME
  }

  /**
   * Adds the dependend libraries of the project to the findbugs aux classpath.
   *
   * @param findBugsProject
   *            The find bugs project to add the aux classpath entries.
   *
   */
  protected void addClasspathEntriesToFindBugsProject( Project findBugsProject )
  {
    def auxClasspathElements = project.compileClasspathElements

    auxClasspathElements.each() { auxClasspathElement ->
      if ( log.isDebugEnabled() )
      {
        log.debug( "  Trying to Add to AuxClasspath ->" + auxClasspathElements.toString() )
      }
      findBugsProject.addAuxClasspathEntry( (String) auxClasspathElement.toString() )
    }

    if ( log.isDebugEnabled() )
    {
      def findbugsAuxClasspath = findBugsProject.auxClasspathEntryList

      findbugsAuxClasspath.each(){ findbugsAuxClasspathEntry ->
        log.debug( "  Added to AuxClasspath ->" + findbugsAuxClasspathEntry.toString() )
      }
    }

  }

  /**
   * Adds the specified filters of the project to the findbugs.
   *
   * @param findBugs
   *            The find bugs to add the filters.
   *
   */
  protected void addFiltersToFindBugs( FindBugs2Proxy findBugs )
  {
    File destFile
    String fileName
            
    if ( includeFilterFile )
    {
      try
      {
        log.debug( "  Searching  includeFilterFile.....")
        destFile = resourceManager.getResourceAsFile( includeFilterFile )

        log.debug( "  Done Searching includeFilterFile with resource Manager....."+ destFile.getAbsoluteFile())
      }
      catch (MalformedURLException me)
      {
        destFile = new File( includeFilterFile )
      }

      log.info( "  Using bug include filter " + destFile.toString())
      findBugs.addFilter( destFile.toString() , true )
    }
    else
    {
      log.info( "  No bug include filter." )
    }

    if ( excludeFilterFile )
    {
      try
      {
        log.debug( "  Searching  excludeFilterFile.....")
        destFile = resourceManager.getResourceAsFile( excludeFilterFile )

        log.debug( "  Done Searching excludeFilterFile with resource Manager....."+ destFile.getAbsoluteFile())
      }
      catch (MalformedURLException me)
      {
        destFile = new File( excludeFilterFile )
      }

      log.info( "  Using bug exclude filter " + destFile.toString())
      findBugs.addFilter( destFile.toString() , false )
    }
    else
    {
      log.info( "  No bug exclude filter." )
    }
  }

  /**
   * Adds the specified plugins to findbugs. The coreplugin is always added first.
   *
   * @param locale
   *            The locale to print out the messages. Used here to get the nameof the coreplugin from the properties.
   *
   */
  protected void addClassScreenerToFindBugs( FindBugs2Proxy findBugs )
  {
    if ( onlyAnalyze )
    {
      log.debug( "  Adding ClassScreener " )
      // The argument is a comma-separated list of classes and packages
      // to select to analyze. (If a list item ends with ".*",
      // it specifies a package, otherwise it's a class.)
      StringTokenizer stringToken = new StringTokenizer( onlyAnalyze, "," )
      while ( stringToken.hasMoreTokens() )
      {
        String stringTokenItem = stringToken.nextToken()
        if ( stringTokenItem.endsWith( ".-" ) )
        {
          classScreener.addAllowedPrefix( stringTokenItem.substring( 0, stringTokenItem.length() - 1 ) )
          log.info(
                                        " classScreener.addAllowedPrefix "
            + ( stringTokenItem.substring( 0, stringTokenItem.length() - 1 ) ) )
        }
        else if ( stringTokenItem.endsWith( ".*" ) )
        {
          classScreener.addAllowedPackage( stringTokenItem.substring( 0, stringTokenItem.length() - 1 ) )
          log.info(
                                        " classScreener.addAllowedPackage "
            + ( stringTokenItem.substring( 0, stringTokenItem.length() - 1 ) ) )
        }
        else
        {
          classScreener.addAllowedClass( stringTokenItem )
          log.info( " classScreener.addAllowedClass " + stringTokenItem )
        }
      }
      findBugs.setClassScreener( classScreener )
    }
    log.debug( "  Done Adding Class Screeners" )
  }

  /**
   * Adds the specified plugins to findbugs. The coreplugin is always added first.
   *
   * @param locale
   *            The locale to print out the messages. Used here to get the nameof the coreplugin from the properties.
   *
   */
  protected void addPluginsToFindBugs( Locale locale )
  {
    //        def corepluginpath
    URL[] pluginURL
    def plugins = []


    if ( pluginList )
    {
      log.info( "  Adding Plugins " )
      String[] pluginJars = pluginList.split( "," )

      pluginJars.each() { pluginJar ->
        def pluginFileName = pluginJar.trim()

        if ( !pluginFileName.endsWith( ".jar" ) )
        {
          throw new IllegalArgumentException( "Plugin File is not a Jar file: " + pluginFileName )
        }

        try
        {
          log.info( "  Processing Plugin: " + pluginFileName.toString() )
          plugins << new File( pluginFileName.toString() ).toURL()
        }
        catch ( MalformedURLException exception )
        {
          fail( "The addin plugin has an invalid URL", exception )
        }
      }
    }
 
    pluginURL = plugins.toArray()
    DetectorFactoryCollection.rawInstance().setPluginList( pluginURL  )

    log.debug( "  Done Adding Plugins" )
  }

  /**
   * Adds the specified visitors to findbugs.
   *
   * @param preferences
   *            The find bugs UserPreferences.
   *
   */
  protected void addVisitorsToFindBugs( UserPreferences preferences )
  {
    /*
     * This is done in this order to make sure only one of vistors or omitVisitors options is run. This is
     * consistent with the way the Findbugs commandline and Ant Tasks run.
     */
    if ( visitors || omitVisitors )
    {
      boolean enableVisitor = true
      String[] visitorList

      //            if ( omitVisitors != null )
      //            if ( omitVisitors != null )
      if ( omitVisitors )
      {
        enableVisitor = false
        visitorList = omitVisitors.split( "," )
        log.debug( "  Omitting visitors : " + omitVisitors )
      }
      else
      {
        visitorList = visitors.split( "," )
        log.debug( "  Including visitors : " + visitors )
        preferences.enableAllDetectors( false )
      }

      visitorList.each() { visitorListItem ->
        def visitorName = visitorListItem.trim()
        DetectorFactory factory = DetectorFactoryCollection.instance().getFactory( visitorName )
        //                if ( factory == null )
        if ( !factory )
        {
          throw new IllegalArgumentException( "Unknown detector: " + visitorName )
        }
        preferences.enableDetector( factory, enableVisitor )
      }
    }
  }

  /**
   * Lists absolute paths of java source files for debugging purposes.
   *
   * @param locale
   *            The locale to print out the messages.
   * @param sourceFiles
   *            List of source files.
   */
  protected void debugJavaSources( Locale locale, List sourceFiles )
  {
    def bundle = FindBugsMojo.getBundle( locale )
    def javaSourceMessage = bundle.getString( FindBugsMojo.JAVA_SOURCES_KEY )
    log.debug( "  " + javaSourceMessage )

    sourceFiles.each()  { sourceFile ->
      log.debug( "    " + sourceFile.getAbsolutePath() )
    }
  }

  /**
   * Executes the generation of the report.
   *
   * Callback from Maven Site Plugin or from AbstractMavenReport.execute() => generate().
   *
   * @param locale
   *            the locale the report should be generated for
   * @see org.apache.maven.reporting.AbstractMavenReport #executeReport(java.util.Locale)
   */
  protected void executeReport( Locale locale )
  {


    resourceManager.addSearchPath( FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath() )
    resourceManager.addSearchPath( "url", "" )

    resourceManager.setOutputDirectory( new File( project.getBuild().getDirectory() ) )

    log.debug("resourceManager outputDirectory is " + resourceManager.outputDirectory )


    if ( !skip )
    {
      ResourceBundle bundle = FindBugsMojo.getBundle( locale )

      FindBugs2Proxy findBugs = null

      log.info( "  " + bundle.getString( FindBugsMojo.SOURCE_ROOT_KEY ) )
      log.info( "    " + classFilesDirectory.getAbsolutePath() )

      log.info( "  " + bundle.getString( FindBugsMojo.TEST_SOURCE_ROOT_KEY ) )
      log.info( "    " + testClassFilesDirectory.getAbsolutePath() )

      if ( !canGenerateReport() )
      {
        log.info( "Output class directory doesn't exist. Skipping findbugs." )
        return
      }

      try
      {
        findBugs = initialiseFindBugs( locale, getJavaSources( locale ) )
      }
      catch ( IOException exception )
      {
        fail( "A java source file could not be added", exception )
      }
      catch ( DependencyResolutionRequiredException exception )
      {
        fail( "Failed executing FindBugs", exception )
      }
      catch ( FilterException exception )
      {
        fail( "Failed adding filters to FindBugs", exception )
      }
      catch ( ArtifactNotFoundException exception )
      {
        fail( "Did not find coreplugin", exception )
      }
      catch ( ArtifactResolutionException exception )
      {
        fail( "Failed to resolve coreplugin", exception )
      }

      try
      {
        findBugs.execute()
      }
      catch ( IOException exception )
      {
        fail( "Failed executing FindBugs", exception )
      }
      catch ( InterruptedException exception )
      {
        fail( "Failed executing FindBugs", exception )
      }
      catch ( Exception exception )
      {
        fail( "Failed executing FindBugs", exception )
      }
    }
  }


  /**
   * Get the File reference for the Findbugs core plugin.
   *
   * @param locale
   *            The locale of the messages.
   * @return The File reference to the coreplugin JAR
   *
   */
  protected File getCorePluginPath( Locale locale )
  {
    def corePluginPath = pluginArtifacts.find(){artifact ->
      artifact.getArtifactId() == getCorePlugin( locale )
    }
    return corePluginPath.file
  }

  /**
   * Returns the effort parameter to use.
   *
   * @return A valid effort parameter.
   *
   */
  protected EffortParameter getEffortParameter()
  {
    EffortParameter effortParameter = EffortParameter.DEFAULT

    if ( !effort )
    {
      log.info( "  No effort provided, using default effort." )
    }
    else
    {
      if ( effort.equals( EffortParameter.MAX.getName() ) )
      {
        effortParameter = EffortParameter.MAX
        log.info( "  Using maximum effort." )
      }
      else if ( effort.equals( EffortParameter.DEFAULT.getName() ) )
      {
        effortParameter = EffortParameter.DEFAULT
        log.info( "  Using normal effort." )
      }
      else if ( effort.equals( EffortParameter.MIN.getName() ) )
      {
        effortParameter = EffortParameter.MIN
        log.info( "  Using minimum effort." )
      }
      else
      {
        log.info( "  Effort not recognised, using default effort" )
      }
    }
    return effortParameter
  }

  /**
   * Collects the java sources from the source roots.
   *
   * @param pSourceDirectory
   *            The source directory to search for java sources.
   * @param locale
   *            The locale to print out the messages.
   * @return A list containing the java sources or an empty list if no java sources are found.
   *
   */
  protected List getJavaSources( Locale locale )
  {
    List sourceFiles = new ArrayList()

    if ( classFilesDirectory.exists() && classFilesDirectory.isDirectory() )
    {
      List files = FileUtils.getFiles( classFilesDirectory, FindBugsMojo.JAVA_REGEX_PATTERN, null )
      sourceFiles.addAll( files )
    }

    if ( testClassFilesDirectory.exists() && testClassFilesDirectory.isDirectory() && includeTests )
    {
      List files = FileUtils.getFiles( testClassFilesDirectory, FindBugsMojo.JAVA_REGEX_PATTERN, null )
      sourceFiles.addAll( files )
    }


    if (log.isDebugEnabled()) {
      debugJavaSources( locale, sourceFiles )
    }
    
    return sourceFiles
  }

  /**
   * Returns the report output directory.
   *
   * Called by AbstractMavenReport.execute() for creating the sink.
   *
   * @return full path to the directory where the files in the site get copied to
   * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
   */
  protected String getOutputDirectory()
  {
    return outputDirectory.getAbsolutePath()
  }

  /**
   * Returns the threshold parameter to use.
   *
   * @return A valid threshold parameter.
   *
   */
  protected ThresholdParameter getThresholdParameter()
  {

    ThresholdParameter thresholdParameter = ThresholdParameter.DEFAULT

    if ( !threshold )
    {
      log.info( "  No threshold provided, using default threshold." )
    }
    else
    {
      if ( threshold.equals( ThresholdParameter.HIGH.getName() ) )
      {
        thresholdParameter = ThresholdParameter.HIGH
        log.info( "  Using high threshold." )
      }
      else if ( threshold.equals( ThresholdParameter.NORMAL.getName() ) )
      {
        thresholdParameter = ThresholdParameter.NORMAL
        log.info( "  Using normal threshold." )
      }
      else if ( threshold.equals( ThresholdParameter.LOW.getName() ) )
      {
        thresholdParameter = ThresholdParameter.LOW
        log.info( "  Using low threshold." )
      }
      else if ( threshold.equals( ThresholdParameter.EXP.getName() ) )
      {
        thresholdParameter = ThresholdParameter.EXP
        log.info( "  Using exp threshold." )
      }
      else if ( threshold.equals( ThresholdParameter.IGNORE.getName() ) )
      {
        thresholdParameter = ThresholdParameter.IGNORE
        log.info( "  Using ignore threshold." )
      }
      else
      {
        log.info( "  Threshold not recognised, using default threshold" )
      }
    }

    return thresholdParameter

  }

  /**
   * Initialise FindBugs.
   *
   * @param locale
   *            The locale.
   * @param sourceFiles
   *            The source files FindBugs should analyse.
   * @return An initialised FindBugs object.
   *
   */
  protected FindBugs2Proxy initialiseFindBugs( Locale locale, List sourceFiles )
  {
    Sink sink = getSink()
    ResourceBundle bundle = FindBugsMojo.getBundle( locale )
    Log log = getLog()
    EffortParameter effortParameter = getEffortParameter()

    TextUIBugReporter textUiBugReporter

    Project findBugsProject = new Project()
    findBugsProject.projectName = "${project.name}"


    //  Adds the source files to the find bugs project.
    sourceFiles.each() { sourceFile ->
      String filePath = sourceFile.getAbsolutePath()
      findBugsProject.addFile( filePath )
    }

    addClasspathEntriesToFindBugsProject( findBugsProject )
    compileSourceRoots.each() { compileSourceRoot ->
      log.info( "  Adding Source Directory: ${compileSourceRoot}" )
      findBugsProject.addSourceDir(compileSourceRoot)
    }

    FindBugs2Proxy findBugs = new FindBugs2Proxy()

    findBugs.setProject( findBugsProject )


    findBugs.initializeProxyReporter( this.getThresholdParameter().getValue() )


    log.info( "  Using FindBugs Version: " + edu.umd.cs.findbugs.Version.RELEASE )

    bugReporter = initialiseReporter( sink, bundle, log, effortParameter )
    bugReporter.setPriorityThreshold( this.getThresholdParameter().getValue() )
    findBugs.setBugReporter( bugReporter )

    if ( findbugsXmlOutput )
    {
      XMLBugReporter xmlBugReporter = new XMLBugReporter( findBugsProject )
      xmlBugReporter.setAddMessages( findbugsXmlWithMessages )
      textUiBugReporter = xmlBugReporter
      textUiBugReporter.setOutputStream( new PrintStream( new FileOutputStream( "${findbugsXmlOutputDirectory}/findbugsXml.xml" ), true, "${xmlEncoding}"  ) )
      textUiBugReporter.setPriorityThreshold( this.getThresholdParameter().getValue() )

      bugReporter = textUiBugReporter
      findBugs.setBugReporter( bugReporter )
    }

    if ( xmlOutput )
    {
      log.info( "  Using the xdoc format" )

      if ( !xmlOutputDirectory.exists() )
      {
        if ( !xmlOutputDirectory.mkdirs() )
        {
          fail( "Cannot create xml output directory" )
        }
      }

      XDocsReporter xDocsReporter = new XDocsReporter( this.getProject() )
      xDocsReporter.setOutputWriter( new OutputStreamWriter( new FileOutputStream( new File( "${xmlOutputDirectory}/findbugs.xml" ) ), "UTF-8" ) )
      xDocsReporter.setResourceBundle( bundle )
      xDocsReporter.setLog( log )
      xDocsReporter.setEffort( getEffortParameter() )
      xDocsReporter.threshold = getThresholdParameter()
      xDocsReporter.setPriorityThreshold( this.getThresholdParameter().getValue() )   //TODO: combine the two in XDocsReporter
      findBugs.setBugReporter( xDocsReporter )
    }

    if ( !pluginLoaded && pluginList )
    {
      addPluginsToFindBugs( locale )
      pluginLoaded = true
    }

    UserPreferences preferences = UserPreferences.createDefaultUserPreferences()

    addVisitorsToFindBugs( preferences )

    findBugs.setRelaxedReportingMode( relaxed.booleanValue() )
    findBugs.setUserPreferences( preferences )
    findBugs.setAnalysisFeatureSettings( effortParameter.getValue() )
    findBugs.setDetectorFactoryCollection( DetectorFactoryCollection.rawInstance() )

    setFindBugsDebug( findBugs )
    addFiltersToFindBugs( findBugs )
    addClassScreenerToFindBugs( findBugs )

    return findBugs
  }

  /**
   * Initialises a reporter.
   *
   * @param sink
   *            The sink to write the report to.
   * @param bundle
   *            The bundle to get messages from.
   * @param log
   *            The logger to write logs to.
   * @param effortParameter
   *            The effort to use.
   * @return An initialised reporter.
   *
   */
  protected Reporter initialiseReporter( Sink sink, ResourceBundle bundle, Log log,
    EffortParameter effortParameter )
  {
    ThresholdParameter thresholdParameter = getThresholdParameter()

    boolean isJXRPluginEnabled = isJXRPluginEnabled( bundle )

    Reporter bugReporter = new Reporter( sink, bundle, log, thresholdParameter, isJXRPluginEnabled, effortParameter )
    bugReporter.setPriorityThreshold( thresholdParameter.getValue() )

    if (isJXRPluginEnabled ){
      bugReporter.setCompileSourceRoots( this.compileSourceRoots )
      bugReporter.setTestSourceRoots( this.testSourceRoots )
      bugReporter.setXrefLocation( this.xrefLocation )
      bugReporter.setXrefTestLocation( this.xrefTestLocation )
      bugReporter.setIncludeTests( this.includeTests )
      bugReporter.setOutputDirectory( new File( getOutputDirectory() ))
    }


    return bugReporter
  }

  /**
   * Determines if the JXR-Plugin is included in the report section of the POM.
   *
   * @param bundle
   *            The bundle to load the artifactIf of the jxr plugin.
   * @return True if the JXR-Plugin is included in the POM, false otherwise.
   *
   */
  protected boolean isJXRPluginEnabled( ResourceBundle bundle )
  {
    boolean isEnabled = false

    String artifactId = bundle.getString( FindBugsMojo.JXR_ARTIFACT_ID_KEY )

    List reportPlugins = getProject().getReportPlugins()
        
    reportPlugins.each() { reportPlugin ->
      if ( artifactId.equals( reportPlugin.getArtifactId() ) )
      {
        isEnabled = true
      }
    }

    return isEnabled
  }

  /**
   * Sets the Debug Level
   *
   * @param findBugs
   *            The find bugs to add debug level information.
   *
   */
  protected void setFindBugsDebug( FindBugs2Proxy findBugs )
  {
    System.setProperty( "findbugs.classpath.debug", debug.toString() )
    System.setProperty( "findbugs.debug", debug.toString() )
    System.setProperty( "findbugs.verbose", debug.toString() )
    System.setProperty( "findbugs.debug.missingclasses", debug.toString() )
       
    if ( debug.booleanValue() )
    {
      log.info( "  Debugging is On" )
    }
    else
    {
      log.info( "  Debugging is Off" )
    }
  }

  /**
   * Returns the resource bundle for a specific locale.
   *
   * @param locale
   *            The locale to get the bundle for.
   * @return A resource Bundle.
   *
   */
  protected static ResourceBundle getBundle( Locale locale )
  {
    ClassLoader loader = FindBugsMojo.class.getClassLoader()
    ResourceBundle bundle = ResourceBundle.getBundle( FindBugsMojo.BUNDLE_NAME, locale, loader )

    return bundle
  }

  protected def fail(msg)
  {
    assert msg

    if (msg instanceof Throwable) {
      fail(msg.message, msg)
    }
    throw new MojoExecutionException("$msg")
  }

  protected def fail(msg, Throwable cause)
  {
    assert msg
    assert cause

    throw new MojoExecutionException("$msg", cause)
  }

  protected MavenProject getProject()
  {
    return this.project
  }

  protected Renderer getSiteRenderer()
  {
    return this.siteRenderer
  }
}
