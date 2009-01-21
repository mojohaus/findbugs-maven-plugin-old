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


import edu.umd.cs.findbugs.DetectorFactory
import edu.umd.cs.findbugs.DetectorFactoryCollection
import edu.umd.cs.findbugs.FindBugs2
import edu.umd.cs.findbugs.Project
import edu.umd.cs.findbugs.TextUIBugReporter
import edu.umd.cs.findbugs.XMLBugReporter

import edu.umd.cs.findbugs.config.UserPreferences
import edu.umd.cs.findbugs.filter.FilterException

import org.apache.maven.artifact.DependencyResolutionRequiredException
import org.apache.maven.artifact.resolver.ArtifactNotFoundException
import org.apache.maven.artifact.resolver.ArtifactResolutionException

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

class FindbugsViolationCheckMojo extends GroovyMojo
{

  /**
   * The name of the property resource bundle (Filesystem).
   *
   */
  static final String BUNDLE_NAME = "findbugs"

  /**
   * The name of the coreplugin.
   *
   */
  static final String FINDBUGS_COREPLUGIN = "report.findbugs.coreplugin"

  /**
   * The regex pattern to search for java class files.
   *
   */
  static final String JAVA_REGEX_PATTERN = "**/*.class"


  /**
   * Directory containing the class files for FindBugs to analyze.
   *
   * @parameter default-value="${project.build.outputDirectory}"
   * @required
   */
  File classFilesDirectory

  /**
   * turn on Findbugs debugging
   *
   * @parameter default-value="false"
   */
  Boolean debug

  /**
   * The visitor list to run. This is a comma-delimited list.
   *
   * @parameter
   *
   */
  String visitors

  /**
   * The visitor list to omit. This is a comma-delimited list.
   *
   * @parameter
   *
   */
  String omitVisitors

  /**
   * List of artifacts this plugin depends on. Used for resolving the Findbugs coreplugin.
   *
   * @parameter expression="${plugin.artifacts}"
   * @required
   * @readonly
   */
  ArrayList pluginArtifacts

  /**
   * Effort of the bug finders. Valid values are Min, Default and Max.
   *
   * @parameter default-value="Default"
   *
   */
  String effort

  /**
   * Specifies the directory where the findbugs xml output will be generated.
   *
   * @parameter default-value="${project.build.directory}"
   * @required
   *
   */
  File findbugsCheckOutputDirectory

  /**
   * Restrict analysis to the given comma-separated list of classes and packages.
   *
   * @parameter
   *
   */
  String onlyAnalyze

  /**
   * The plugin list to include in the report. This is a comma-delimited list.
   *
   * @parameter
   *
   */
  String pluginList

  /**
   * The Flag letting us know if classes have been loaded already.
   *
   * @parameter
   * @readonly
   */
  static boolean pluginLoaded = false

  /**
   * Maven Project
   *
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  MavenProject project

  /**
   * Relaxed reporting mode. For many detectors, this option suppresses the heuristics used to avoid reporting false
   * positives.
   *
   * @parameter default-value="false"
   *
   */
  Boolean relaxed

  /**
   * Skip entire check.
   *
   * @parameter expression="${skip}" default-value="false"
   *
   */
  boolean skip

  /**
   * Fail the build on an error.
   *
   * @parameter default-value="true"
   * @since 2.0
   */
  boolean failOnError

  /**
   * Threshold of minimum bug severity to report. Valid values are High, Default, Low, Ignore, and Exp (for experimental).
   *
   * @parameter default-value="Default"
   */
  String threshold

  /**
   * File name of the include filter. Only bugs in matching the filters are reported.
   *
   * @parameter
   *
   */
  String includeFilterFile

  /**
   * File name of the exclude filter. Bugs matching the filters are not reported.
   *
   * @parameter
   *
   */
  String excludeFilterFile

  /**
   * Resource bundle for a specific locale.
   *
   * @parameter
   * @readonly
   *
   */
  ResourceBundle bundle

  /**
   * @component
   * @required
   * @readonly
   *
   */
  ResourceManager resourceManager

    
  void execute()
  {
    Locale locale = Locale.getDefault()
    List sourceFiles

    log.info("Excecuting findbugs:check")

    if ( this.classFilesDirectory.exists() && this.classFilesDirectory.isDirectory() )
    {
      sourceFiles = FileUtils.getFiles(classFilesDirectory, FindbugsViolationCheckMojo.JAVA_REGEX_PATTERN, null)
    }

    if ( !skip && sourceFiles )
    {
      bundle = ResourceBundle.getBundle( BUNDLE_NAME, locale )

      FindBugs2 findBugs = null

      try
      {
        EffortParameter effortParameter = getEffortParameter()

        TextUIBugReporter textUiBugReporter

        Project findBugsProject = new Project()
        findBugsProject.projectName = "${project.name}"

        //  Adds the source files to the find bugs project.
        sourceFiles.each() {sourceFile ->
          String filePath = sourceFile.getAbsolutePath()
          findBugsProject.addFile(filePath)
        }


        addClasspathEntriesToFindBugsProject(findBugsProject)

        findBugs = new FindBugs2()

        findBugs.setProject(findBugsProject)

        log.info("  Using FindBugs Version: " + edu.umd.cs.findbugs.Version.RELEASE)

        XMLBugReporter xmlBugReporter = new XMLBugReporter(findBugsProject)
        xmlBugReporter.setAddMessages(true)
        textUiBugReporter = xmlBugReporter
        textUiBugReporter.setOutputStream(new PrintStream(new FileOutputStream("${findbugsCheckOutputDirectory}/findbugsCheck.xml"), true, "UTF-8"))
        textUiBugReporter.setPriorityThreshold(this.getThresholdParameter().getValue())

        findBugs.setBugReporter(textUiBugReporter)


        if ( !pluginLoaded )
        {
          addPluginsToFindBugs( )
          pluginLoaded = true
        }

        UserPreferences preferences = UserPreferences.createDefaultUserPreferences()

        addVisitorsToFindBugs(preferences)

        findBugs.setRelaxedReportingMode(relaxed.booleanValue())
        findBugs.setUserPreferences(preferences)
        findBugs.setAnalysisFeatureSettings(effortParameter.getValue())
        findBugs.setDetectorFactoryCollection(DetectorFactoryCollection.rawInstance())

        setFindBugsDebug(findBugs)
        addFiltersToFindBugs(findBugs)
        addClassScreenerToFindBugs(findBugs)

      }
      catch (IOException exception)
      {
        fail("A java source file could not be added", exception)
      }
      catch (DependencyResolutionRequiredException exception)
      {
        fail("Failed executing FindBugs", exception)
      }
      catch (FilterException exception)
      {
        fail("Failed adding filters to FindBugs", exception)
      }
      catch (ArtifactNotFoundException exception)
      {
        fail("Did not find coreplugin", exception)
      }
      catch (ArtifactResolutionException exception)
      {
        fail("Failed to resolve coreplugin", exception)
      }

      try
      {
        findBugs.execute()
      }
      catch (IOException exception)
      {
        fail("Failed executing FindBugs", exception)
      }
      catch (InterruptedException exception)
      {
        fail("Failed executing FindBugs", exception)
      }
      catch (Exception exception)
      {
        fail("Failed executing FindBugs", exception)
      }

      def bugCount = findBugs.getBugCount()
      def errorCount = findBugs.getErrorCount()

      if (( bugCount || errorCount ) && failOnError )
      {
        fail("failed with ${bugCount} bugs and ${errorCount} errors ")
      }

    }
    else
    {
      log.info( "Nothing for FindBugs to do here." )
    }
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
   * Adds the specified plugins to findbugs. The coreplugin is always added first.
   *
   */
  protected void addPluginsToFindBugs( )
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

      if ( omitVisitors != null )
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
        if ( !factory )
        {
          throw new IllegalArgumentException( "Unknown detector: " + visitorName )
        }
        preferences.enableDetector( factory, enableVisitor )
      }
    }
  }

  /**
   * Sets the Debug Level
   *
   * @param findBugs
   *            The find bugs to add debug level information.
   *
   */
  protected void setFindBugsDebug( FindBugs2 findBugs )
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
   * Adds the specified filters of the project to the findbugs.
   *
   * @param findBugs
   *            The find bugs to add the filters.
   *
   */
  protected void addFiltersToFindBugs( FindBugs2 findBugs )
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
   */
  protected void addClassScreenerToFindBugs( FindBugs2 findBugs )
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
          log.debug(
                                        " classScreener.addAllowedPrefix "
            + ( stringTokenItem.substring( 0, stringTokenItem.length() - 1 ) ) )
        }
        else if ( stringTokenItem.endsWith( ".*" ) )
        {
          classScreener.addAllowedPackage( stringTokenItem.substring( 0, stringTokenItem.length() - 1 ) )
          log.debug(
                                        " classScreener.addAllowedPackage "
            + ( stringTokenItem.substring( 0, stringTokenItem.length() - 1 ) ) )
        }
        else
        {
          classScreener.addAllowedClass( stringTokenItem )
          log.debug( " classScreener.addAllowedClass " + stringTokenItem )
        }
      }
      findBugs.setClassScreener( classScreener )
    }
    log.debug( "  Done Adding Class Screeners" )
  }
}
