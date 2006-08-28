package org.codehaus.mojo.findbugs;

/* `Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;

import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.filter.FilterException;

/**
 * Generates a FindBugs report.
 * 
 * @goal findbugs
 * @description Generates a FindBugs Report.
 * @execute phase="compile"
 * 
 * @author <a href="mailto:ruettimac@mac.com">Cyrill Ruettimann</a>
 * @author <a href="mailto:d.pleiss@comundus.com">Detlef Pleiss</a>
 * @version $Id$
 */
public final class FindBugsMojo
    extends AbstractMavenReport
{

    /**
     * The name of the Plug-In.
     * 
     */
    private static final String PLUGIN_NAME = "findbugs";

    /**
     * The name of the property resource bundle (Filesystem).
     * 
     */
    private static final String BUNDLE_NAME = "findbugs";

    /**
     * The key to get the name of the Plug-In from the bundle.
     * 
     */
    private static final String NAME_KEY = "report.findbugs.name";

    /**
     * The key to get the description of the Plug-In from the bundle.
     * 
     */
    private static final String DESCRIPTION_KEY = "report.findbugs.description";

    /**
     * The key to get the source directory message of the Plug-In from the
     * bundle.
     * 
     */
    private static final String SOURCE_ROOT_KEY = "report.findbugs.sourceRoot";

    /**
     * The key to get the java source message of the Plug-In from the bundle.
     * 
     */
    private static final String JAVA_SOURCES_KEY = "report.findbugs.javasources";

    /**
     * The regex pattern to search for java class files.
     * 
     */
    private static final String JAVA_REGEX_PATTERN = "**/*.class";

    /**
     * The key to get the jxr-plugin artifactId from the bundle.
     * 
     */
    private static final String JXR_ARTIFACT_ID_KEY = "report.findbugs.jxrplugin.artifactid";

    /**
     * The name of the coreplugin.
     * 
     */
    private static final String FINDBUGS_COREPLUGIN = "report.findbugs.coreplugin";

    /**
     * Location where generated html will be created.
     * 
     * @parameter expression="${project.reporting.outputDirectory}/site"
     */
    private transient String outputDirectory;

    /**
     * Doxia Site Renderer.
     * 
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    private transient SiteRenderer siteRenderer;

    /**
     * Directory containing the class files for FindBugs to analyze.
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     */
    private transient File classFilesDirectory;

    /**
     * List of artifacts this plugin depends on.
     * Used for resolving the Findbugs coreplugin.
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private transient ArrayList pluginArtifacts;

    /**
     * The local repository, needed to download the coreplugin jar.
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private transient DefaultArtifactRepository localRepository;

    /**
     * Remote repositories which will be searched for the coreplugin jar.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    private List remoteArtifactRepositories;

    /**
     * Maven Project
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private transient MavenProject project;

    /**
     * Threshold of minimum bug severity to report. 
     * Valid values are High, Medium, Low and Exp (for experimental).
     *
     * @parameter
     */
    private transient String threshold;

    /**
     * Artifact resolver, needed to download the coreplugin jar.
     *
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * @required
     * @readonly
     */
    private ArtifactResolver artifactResolver;

    /**
     * File name of the include filter. Only bugs in matching the filters are reported.
     * 
     * @parameter
     */
    private transient String includeFilterFile;

    /**
     * File name of the exclude filter. Bugs matching the filters are not reported.
     * 
     * @parameter
     */
    private transient String excludeFilterFile;

    /**
     * Effort of the bug finders.
     * Valid values are Min, Default and Max.
     * 
     * @parameter
     */
    private transient String effort;

    /**
     * turn on Findbugs debugging
     *
     * @parameter default-value="false"
     * DP: not used yet
     private transient Boolean debug;
     */

    /**
     * The visitor list to run.
     * This is a comma-delimited list.
     *
     * @parameter
     */
    private transient String visitors;

    /**
     * The visitor list to omit.
     * This is a comma-delimited list.
     *
     * @parameter
     */
    private transient String omitVisitors;

    /**
     * The plugin list to include in the report.
     * This is a comma-delimited list.
     *
     * @parameter
     */
    private transient String pluginList;

    /**
     * Returns report output file name, without the extension.
     * 
     * Called by AbstractMavenReport.execute() for creating the sink.
     * 
     * @return name of the generated page
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return FindBugsMojo.PLUGIN_NAME;
    }

    /**
     * Returns the plugins name for the "generated reports" overview page and the menu.
     * 
     * @param pLocale
     *            the locale the report should be generated for
     * 
     * @return name of the report
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( final Locale pLocale )
    {
        final ResourceBundle bundle = FindBugsMojo.getBundle( pLocale );
        final String name = bundle.getString( FindBugsMojo.NAME_KEY );

        return name;
    }

    /**
     * Returns the plugins description for the "generated reports" overview page.
     * 
     * @param pLocale
     *            the locale the report should be generated for
     * 
     * @return description of the report
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( final Locale pLocale )
    {
        final ResourceBundle bundle = FindBugsMojo.getBundle( pLocale );
        final String description = bundle.getString( FindBugsMojo.DESCRIPTION_KEY );

        return description;
    }

    /**
     * Returns the report output directory.
     * 
     * Called by AbstractMavenReport.execute() for creating the sink.
     * 
     * @return full path to the directory where the files in the site get copied
     *         to
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return this.outputDirectory;
    }

    /**
     * Checks whether prerequisites for generating this report are given.
     * 
     * @return true if report can be generated, otherwise false
     * @see org.apache.maven.reporting.MavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        return this.classFilesDirectory.exists();
    }

    /**
     * Returns the doxia site renderer.
     * 
     * @return the doxia Renderer
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return this.siteRenderer;
    }

    /**
     * Executes the generation of the report.
     * 
     * Callback from Maven Site Plugin or from AbstractMavenReport.execute() =>
     * generate().
     * 
     * @param pLocale
     *            the locale the report should be generated for
     * @throws MavenReportException
     *             if anything goes wrong
     * @see org.apache.maven.reporting.AbstractMavenReport
     *      #executeReport(java.util.Locale)
     */
    protected void executeReport( final Locale pLocale )
        throws MavenReportException
    {
        FindBugs findBugs = null;
        this.debugSourceDirectory( pLocale, this.classFilesDirectory );
        try
        {
            findBugs = this.initialiseFindBugs( pLocale, this.getJavaSources( pLocale, this.classFilesDirectory ) );
        }
        catch ( final IOException pException )
        {
            throw new MavenReportException( "A java source file could not be added", pException );
        }
        catch ( final DependencyResolutionRequiredException pException )
        {
            throw new MavenReportException( "Failed executing FindBugs", pException );
        }
        catch ( final FilterException pException )
        {
            throw new MavenReportException( "Failed adding filters to FindBugs", pException );
        }
        catch ( final ArtifactNotFoundException pException )
        {
            throw new MavenReportException( "Did not find coreplugin", pException );
        }
        catch ( final ArtifactResolutionException pException )
        {
            throw new MavenReportException( "Failed to resolve coreplugin", pException );
        }
        // save the original out and err stream 
        final PrintStream tempOut = System.out;
        //final PrintStream tempErr = System.err;
        try
        {
            // redirect the streams 
            //final OutputStream pipedOut = new ByteArrayOutputStream();
            System.setOut( new VoidPrintStream() );
            //System.setErr( new PrintStream( pipedOut ) );
            findBugs.execute();
        }
        catch ( final IOException pException )
        {
            throw new MavenReportException( "Failed executing FindBugs", pException );
        }
        catch ( final InterruptedException pException )
        {
            throw new MavenReportException( "Failed executing FindBugs", pException );
        }
        catch ( final Exception pException )
        {
            throw new MavenReportException( "Failed executing FindBugs", pException );
        }
        finally
        {
            // restore to the old streams
            System.setOut( tempOut );
            //System.setErr( tempErr );
        }
    }

    /**
     * PrintStream that prints just nothing.
     * Used to suppress FindBugs System.out Messages.
     */
    private static class VoidPrintStream
        extends PrintStream
    {

        /**
         * contructor
         */
        public VoidPrintStream()
        {
            super( new PrintStream( System.out ) );
        }

        /**
         * @param s String to print
         */
        public void println( String s )
        {
            // do nothing
        }
    }

    /**
     * Initialise FindBugs.
     * 
     * @param pLocale
     *            The locale.
     * @param pSourceFiles
     *            The source files FindBugs should analyse.
     * @return An initialised FindBugs object.
     * @throws DependencyResolutionRequiredException 
     *              Exception that occurs when an artifact file is used, but has not been resolved.
     * @throws IOException If filter file could not be read.
     * @throws FilterException If filter file was invalid.
     * @throws ArtifactNotFoundException If the coreplugin could not be found.
     * @throws ArtifactResolutionException If the coreplugin could not be resolved.
     * 
     */
    protected FindBugs initialiseFindBugs( final Locale pLocale, final List pSourceFiles )
        throws DependencyResolutionRequiredException, IOException, FilterException, ArtifactNotFoundException,
        ArtifactResolutionException
    {
        final Sink sink = this.getSink();
        final ResourceBundle bundle = FindBugsMojo.getBundle( pLocale );
        final Log log = this.getLog();
        final EffortParameter effortParameter = this.getEffortParameter();
        final Reporter bugReporter = this.initialiseReporter( sink, bundle, log, effortParameter );
        final Project findBugsProject = new Project();
        this.addJavaSourcesToFindBugsProject( pSourceFiles, findBugsProject );
        this.addClasspathEntriesToFindBugsProject( findBugsProject );
        final FindBugs findBugs = new FindBugs( bugReporter, findBugsProject );

        this.addPluginsToFindBugs( pLocale );

        final UserPreferences preferences = UserPreferences.createDefaultUserPreferences();

        this.addVisitorsToFindBugs( preferences );

        findBugs.setUserPreferences( preferences );
        findBugs.setAnalysisFeatureSettings( effortParameter.getValue() );

        // TO DO fix output to allow Findbugs debugging to work
        //        this.setFindBugsDebug( findBugs );  
        this.addFiltersToFindBugs( findBugs );

        return findBugs;
    }

    /**
     * Initialises a reporter.
     * 
     * @param pSink
     *            The sink to write the report to.
     * @param pBundle
     *            The bundle to get messages from.
     * @param pLog
     *            The logger to write logs to.
     * @param pEffortParameter
     *            The effort to use.
     * @return An initialised reporter.
     * 
     */
    protected Reporter initialiseReporter( final Sink pSink, final ResourceBundle pBundle, final Log pLog,
                                           final EffortParameter pEffortParameter )
    {
        ThresholdParameter thresholdParameter = ThresholdParameter.DEFAULT;

        if ( this.threshold == null )
        {
            this.getLog().info( "  No threshold provided, using default threshold." );
        }
        else
        {
            if ( this.threshold.equals( ThresholdParameter.HIGH.getName() ) )
            {
                thresholdParameter = ThresholdParameter.HIGH;
                this.getLog().info( "  Using high threshold." );
            }
            else if ( this.threshold.equals( ThresholdParameter.NORMAL.getName() ) )
            {
                thresholdParameter = ThresholdParameter.NORMAL;
                this.getLog().info( "  Using normal threshold." );
            }
            else if ( this.threshold.equals( ThresholdParameter.LOW.getName() ) )
            {
                thresholdParameter = ThresholdParameter.LOW;
                this.getLog().info( "  Using low threshold." );
            }
            else if ( this.threshold.equals( ThresholdParameter.EXP.getName() ) )
            {
                thresholdParameter = ThresholdParameter.EXP;
                this.getLog().info( "  Using exp threshold." );
            }
            else if ( this.threshold.equals( ThresholdParameter.IGNORE.getName() ) )
            {
                thresholdParameter = ThresholdParameter.IGNORE;
                this.getLog().info( "  Using ignore threshold." );
            }
            else
            {
                this.getLog().info( "  Threshold not recognised, using default threshold" );
            }
        }

        final boolean isJXRPluginEnabled = this.isJXRPluginEnabled( pBundle );
        final Reporter bugReporter = new Reporter( pSink, pBundle, pLog, thresholdParameter, isJXRPluginEnabled,
                                                   pEffortParameter );
        bugReporter.setPriorityThreshold( thresholdParameter.getValue() );

        return bugReporter;
    }

    /**
     * Collects the java sources from the source roots.
     * 
     * @param pSourceDirectory
     *            The source directory to search for java sources.
     * @param pLocale
     *            The locale to print out the messages.
     * @return A list containing the java sources or an empty list if no java
     *         sources are found.
     * @throws IOException
     *             If there are problems searching for java sources.
     * 
     */
    protected List getJavaSources( final Locale pLocale, final File pSourceDirectory )
        throws IOException
    {
        final List sourceFiles = new ArrayList();

        if ( pSourceDirectory.exists() && pSourceDirectory.isDirectory() )
        {
            final List files = FileUtils.getFiles( pSourceDirectory, FindBugsMojo.JAVA_REGEX_PATTERN, null );
            sourceFiles.addAll( files );
        }

        this.debugJavaSources( pLocale, sourceFiles );

        return sourceFiles;
    }

    /**
     * Adds the source files to the find bugs project. The return value of the
     * method call <code>addFile</code> is omited, because we are not
     * interested if the java source is already added.
     * 
     * @param pSourceFiles
     *            The java sources (Type <code>java.io.File</code>) to add to
     *            the project.
     * @param pFindBugsProject
     *            The find bugs project to add the java source to.
     * 
     */
    protected void addJavaSourcesToFindBugsProject( final List pSourceFiles, final Project pFindBugsProject )
    {
        final Iterator iterator = pSourceFiles.iterator();
        while ( iterator.hasNext() )
        {
            final File currentSourceFile = (File) iterator.next();
            final String filePath = currentSourceFile.getAbsolutePath();
            pFindBugsProject.addFile( filePath );
        }
    }

    /**
     * Adds the dependend libraries of the project to the findbugs aux classpath.
     *
     * @param pFindBugsProject The find bugs project to add the aux classpath entries.
     * @throws DependencyResolutionRequiredException 
     *      Exception that occurs when an artifact file is used, but has not been resolved.
     * 
     */
    protected void addClasspathEntriesToFindBugsProject( final Project pFindBugsProject )
        throws DependencyResolutionRequiredException
    {
        final List entries = this.getProject().getCompileClasspathElements();
        final Iterator iterator = entries.iterator();
        while ( iterator.hasNext() )
        {
            final String currentEntry = (String) iterator.next();
            this.getLog().debug( "  Adding " + currentEntry + " to auxilary classpath" );
            pFindBugsProject.addAuxClasspathEntry( currentEntry );
        }
    }

    /**
     * Adds the specified filters of the project to the findbugs.
     *
     * @param pFindBugs The find bugs to add the filters.
     * @throws IOException If filter file could not be read.
     * @throws FilterException If filter file was invalid.
     * 
     */
    protected void addFiltersToFindBugs( final FindBugs pFindBugs )
        throws IOException, FilterException
    {
        if ( this.includeFilterFile != null )
        {
            if ( new File( this.includeFilterFile ).exists() )
            {
                pFindBugs.addFilter( this.includeFilterFile, true );
                this.getLog().info( "  Using bug include filter " + this.includeFilterFile );
            }
            else
            {
                this.getLog().info( "  No bug include filter " + this.includeFilterFile + " found" );
            }
        }
        else
        {
            this.getLog().info( "  No bug include filter." );
        }
        if ( this.excludeFilterFile != null )
        {
            if ( new File( this.excludeFilterFile ).exists() )
            {
                pFindBugs.addFilter( this.excludeFilterFile, false );
                this.getLog().info( "  Using bug exclude filter " + this.excludeFilterFile );
            }
            else
            {
                this.getLog().info( "  No bug exclude filter " + this.excludeFilterFile + " found" );
            }
        }
        else
        {
            this.getLog().info( "  No bug exclude filter." );
        }
    }

    /**
     * Adds the specified plugins to findbugs.
     * The coreplugin is always added first.
     * 
     * @param pLocale
     *            The locale to print out the messages. 
     *            Used here to get the nameof the coreplugin from the properties.
     * @throws ArtifactNotFoundException If the coreplugin could not be found.
     * @throws ArtifactResolutionException If the coreplugin could not be resolved.
     * 
     */
    protected void addPluginsToFindBugs( final Locale pLocale )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {

        final File corepluginpath = this.getCorePluginPath( pLocale );
        getLog().info( "  coreplugin Jar is located at " + corepluginpath.toString() );

        File[] plugins;

        if ( this.pluginList != null )
        {
            getLog().info( "  Adding Plugins " );
            final String[] pluginJars = this.pluginList.split( "," );

            plugins = new File[pluginJars.length + 1];

            for ( int i = 0; i < pluginJars.length; i++ )
            {
                String pluginFile = pluginJars[i].trim();

                if ( !pluginFile.endsWith( ".jar" ) )
                {
                    throw new IllegalArgumentException( "Plugin File is not a Jar file: " + pluginFile );
                }

                getLog().info( "  Adding Plugin: " + pluginFile );
                plugins[i + 1] = new File( pluginFile );

            }
        }
        else
        {
            plugins = new File[1];
        }

        getLog().info( "  Done Adding Plugins" );

        plugins[0] = corepluginpath;
        DetectorFactoryCollection.setPluginList( plugins );

    }

    /** 
     * Get the File reference for the Findbugs core plugin.
     *
     * @param pLocale
     *            The locale of the messages.
     * @return The File reference to the coreplugin JAR
     * @throws ArtifactNotFoundException If the coreplugin could not be found.
     * @throws ArtifactResolutionException If the coreplugin could not be resolved.
     * 
     */
    protected File getCorePluginPath( final Locale pLocale )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        for ( Iterator it = this.pluginArtifacts.iterator(); it.hasNext(); )
        {
            final Artifact artifact = (Artifact) it.next();
            if ( artifact.getArtifactId().equals( getCorePlugin( pLocale ) ) )
            {
                this.artifactResolver.resolve( artifact, this.remoteArtifactRepositories, this.localRepository );
                return artifact.getFile();
            }
        }
        return null;
    }

    /**
     * Retrieve the coreplugin module name
     * 
     * @param pLocale
     *            The locale to print out the messages.
     * @return corePluginName
     *            The coreplugin module name.
     * 
     */
    protected String getCorePlugin( final Locale pLocale )
    {
        final ResourceBundle bundle = getBundle( pLocale );
        final String corePluginName = bundle.getString( FINDBUGS_COREPLUGIN );

        return corePluginName;

    }

    /**
     * Adds the specified visitors to findbugs.
     *
     * @param preferences The find bugs UserPreferences.
     * 
     */
    protected void addVisitorsToFindBugs( final UserPreferences preferences )
    {
        // This is done in this order to make sure only one of vistors or omitVisitors options is run
        // This is consistent with the way the Findbugs commandline and Ant Tasks run
        if ( this.visitors != null || this.omitVisitors != null )
        {
            boolean enableVisitor = true;
            String[] visitorList;

            if ( this.omitVisitors != null )
            {
                enableVisitor = false;
                visitorList = this.omitVisitors.split( "," );
                this.getLog().info( "  Omitting visitors : " + this.omitVisitors );

            }
            else
            {
                visitorList = this.visitors.split( "," );
                this.getLog().info( "  Including visitors : " + this.visitors );
                preferences.enableAllDetectors( false );
            }

            for ( int i = 0; i < visitorList.length; i++ )
            {
                String visitorName = visitorList[i].trim();
                DetectorFactory factory = DetectorFactoryCollection.instance().getFactory( visitorName );
                if ( factory == null )
                {
                    throw new IllegalArgumentException( "Unknown detector: " + visitorName );
                }

                preferences.enableDetector( factory, enableVisitor );
            }
        }
    }

    /**
     * Sets the Debug Level
     *
     * @param pFindBugs The find bugs to add the filters.
     * DP: not used yet
     protected void setFindBugsDebug( final FindBugs pFindBugs )
     {
     System.setProperty( "findbugs.debug", debug.toString() );

     if ( debug.booleanValue() )
     {
     this.getLog().info( "  Debugging is On" );
     }
     else
     {
     this.getLog().info( "  Debugging is Off" );
     }
     }
     */

    /**
     * Returns the maven project.
     * 
     * @return the maven project
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return this.project;
    }

    /**
     * Returns the resource bundle for a specific locale.
     * 
     * @param pLocale
     *            The locale to get the bundle for.
     * @return A resource Bundle.
     * 
     */
    protected static ResourceBundle getBundle( final Locale pLocale )
    {
        final ClassLoader loader = FindBugsMojo.class.getClassLoader();
        final ResourceBundle bundle = ResourceBundle.getBundle( FindBugsMojo.BUNDLE_NAME, pLocale, loader );

        return bundle;
    }

    /**
     * Returns the effort parameter to use.
     * 
     * @return A valid effort parameter.
     * 
     */
    protected EffortParameter getEffortParameter()
    {
        EffortParameter effortParameter = EffortParameter.DEFAULT;

        if ( this.effort == null )
        {
            this.getLog().info( "  No effort provided, using default effort." );
        }
        else
        {
            if ( this.effort.equals( EffortParameter.MAX.getName() ) )
            {
                effortParameter = EffortParameter.MAX;
                this.getLog().info( "  Using maximum effort." );
            }
            else if ( this.effort.equals( EffortParameter.DEFAULT.getName() ) )
            {
                effortParameter = EffortParameter.DEFAULT;
                this.getLog().info( "  Using normal effort." );
            }
            else if ( this.effort.equals( EffortParameter.MIN.getName() ) )
            {
                effortParameter = EffortParameter.MIN;
                this.getLog().info( "  Using minimum effort." );
            }
            else
            {
                this.getLog().info( "  Effort not recognised, using default effort" );
            }
        }
        return effortParameter;
    }

    /**
     * Prints out the source roots to the logger with severity debug.
     * 
     * @param pLocale
     *            The locale to print out the messages.
     * @param pSourceDirectory
     *            The source directory to print.
     * 
     */
    protected void debugSourceDirectory( final Locale pLocale, final File pSourceDirectory )
    {
        final ResourceBundle bundle = FindBugsMojo.getBundle( pLocale );
        final String sourceRootMessage = bundle.getString( FindBugsMojo.SOURCE_ROOT_KEY );
        this.getLog().debug( "  " + sourceRootMessage );
        this.getLog().debug( "    " + pSourceDirectory.getAbsolutePath() );
    }

    /**
     * Lists absolute paths of java source files for denugging purposes. 
     * @param pLocale
     *            The locale to print out the messages.
     * @param pSourceFiles
     *              List of source files.
     */
    protected void debugJavaSources( final Locale pLocale, final List pSourceFiles )
    {
        final ResourceBundle bundle = FindBugsMojo.getBundle( pLocale );
        final String javaSourceMessage = bundle.getString( FindBugsMojo.JAVA_SOURCES_KEY );
        this.getLog().debug( "  " + javaSourceMessage );

        final Iterator iterator = pSourceFiles.iterator();
        while ( iterator.hasNext() )
        {
            final File currentFile = (File) iterator.next();
            this.getLog().debug( "    " + currentFile.getAbsolutePath() );
        }
    }

    /** 
     * Determines if the JXR-Plugin is included in the report section of the POM.
     *
     * @param pBundle The bundle to load the artifactIf of the jxr plugin.
     * @return True if the JXR-Plugin is included in the POM, false otherwise.
     * 
     */
    protected boolean isJXRPluginEnabled( final ResourceBundle pBundle )
    {
        boolean isEnabled = false;

        final String artifactId = pBundle.getString( FindBugsMojo.JXR_ARTIFACT_ID_KEY );

        final List reportPlugins = this.getProject().getReportPlugins();
        final Iterator iterator = reportPlugins.iterator();
        while ( iterator.hasNext() )
        {
            final ReportPlugin currentPlugin = (ReportPlugin) iterator.next();
            final String currentArtifactId = currentPlugin.getArtifactId();
            if ( artifactId.equals( currentArtifactId ) )
            {
                isEnabled = true;
            }
        }

        return isEnabled;
    }

}
