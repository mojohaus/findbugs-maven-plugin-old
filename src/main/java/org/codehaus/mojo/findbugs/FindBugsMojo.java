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
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import org.codehaus.plexus.util.FileUtils;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ClassScreener;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.filter.FilterException;

/**
 * Generates a FindBugs report.
 * 
 * @goal findbugs
 * @description Generates a FindBugs Report.
 * @execute phase="compile"
 * @requiresDependencyResolution compile
 * @requiresProject
 * 
 * @author <a href="mailto:ruettimac@mac.com">Cyrill Ruettimann</a>
 * @author <a href="mailto:d.pleiss@comundus.com">Detlef Pleiss</a>
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id$
 */
public final class FindBugsMojo extends AbstractMavenReport
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
     * The key to get the source directory message of the Plug-In from the bundle.
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
     * @parameter default-value="${project.reporting.outputDirectory}"
     * @required
     */

    private String outputDirectory;

    /**
     * Turn on and off xml output of the Findbugs report.
     * 
     * @parameter default-value="false"
     */
    private boolean xmlOutput;

    /**
     * Specifies the directory where the xml output will be generated.
     * 
     * @parameter default-value="${project.build.directory}"
     * @required
     */
    private File xmlOutputDirectory;

    /**
     * Doxia Site Renderer.
     * 
     * @parameter expression="${component.org.apache.maven.doxia.siterenderer.Renderer}"
     * @required
     * @readonly
     */
    private Renderer siteRenderer;

    /**
     * Directory containing the class files for FindBugs to analyze.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     */
    private File classFilesDirectory;

    /**
     * List of artifacts this plugin depends on. Used for resolving the Findbugs coreplugin.
     * 
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private ArrayList pluginArtifacts;

    /**
     * The local repository, needed to download the coreplugin jar.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private DefaultArtifactRepository localRepository;

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
    private MavenProject project;

    /**
     * Threshold of minimum bug severity to report. Valid values are High, Medium, Low and Exp (for experimental).
     * 
     * @parameter
     */
    private String threshold;

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
    private String includeFilterFile;

    /**
     * File name of the exclude filter. Bugs matching the filters are not reported.
     * 
     * @parameter
     */
    private String excludeFilterFile;

    /**
     * Effort of the bug finders. Valid values are Min, Default and Max.
     * 
     * @parameter
     */
    private String effort;

    /**
     * turn on Findbugs debugging
     * 
     * @parameter default-value="false"
     */
    private Boolean debug;

    /**
     * Relaxed reporting mode. For many detectors, this option suppresses the heuristics used to avoid reporting false
     * positives.
     * 
     * @parameter default-value="false"
     */
    private Boolean relaxed;

    /**
     * The visitor list to run. This is a comma-delimited list.
     * 
     * @parameter
     */
    private String visitors;

    /**
     * The visitor list to omit. This is a comma-delimited list.
     * 
     * @parameter
     */
    private String omitVisitors;

    /**
     * The plugin list to include in the report. This is a comma-delimited list.
     * 
     * @parameter
     */
    private String pluginList;

    /**
     * The Base FindBugs reporter Class for reports.
     * 
     * @parameter
     * @readonly
     */
    private BugReporter bugReporter;

    /**
     * Restrict analysis to find bugs to given comma-separated list of classes and packages.
     * 
     * @parameter
     */
    private String onlyAnalyze;

    /**
     * The Base FindBugs reporter Class for reports.
     * 
     * @parameter
     * @readonly
     */
    private ClassScreener classScreener = new ClassScreener();

    /**
     * The Flag letting us know if classes have been loaded already.
     * 
     * @parameter
     * @readonly
     */
    private static boolean pluginLoaded = false;

    /**
     * Skip entire check.
     * 
     * @parameter expression="${findbugs.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * Checks whether prerequisites for generating this report are given.
     * 
     * @return true if report can be generated, otherwise false
     * @see org.apache.maven.reporting.MavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        if ( !skip )
        {
            return this.classFilesDirectory.exists();
        }
        else
        {
            return false;
        }
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
     * Adds the dependend libraries of the project to the findbugs aux classpath.
     * 
     * @param findBugsProject
     *            The find bugs project to add the aux classpath entries.
     * @throws DependencyResolutionRequiredException
     *             Exception that occurs when an artifact file is used, but has not been resolved.
     * 
     */
    protected void addClasspathEntriesToFindBugsProject( final Project findBugsProject )
        throws DependencyResolutionRequiredException
    {
        List auxClasspathElements = this.getProject().getCompileClasspathElements();

        for ( int i = 0; i < auxClasspathElements.size(); ++i )
        {
            if ( this.getLog().isDebugEnabled() )
            {
                this.getLog().debug( "  Trying to Add to AuxClasspath ->" + auxClasspathElements.get( i ).toString() );
            }

            findBugsProject.addAuxClasspathEntry( (String) auxClasspathElements.get( i ).toString() );
        }

        if ( this.getLog().isDebugEnabled() )
        {
            List findbugsAuxClasspath = findBugsProject.getAuxClasspathEntryList();

            for ( int j = 0; j < findbugsAuxClasspath.size(); ++j )
            {
                this.getLog().debug( "  Added to AuxClasspath ->" + findbugsAuxClasspath.get( j ).toString() );
            }
        }

    }

    /**
     * Adds the specified filters of the project to the findbugs.
     * 
     * @param findBugs
     *            The find bugs to add the filters.
     * @throws IOException
     *             If filter file could not be read.
     * @throws FilterException
     *             If filter file was invalid.
     * 
     */
    protected void addFiltersToFindBugs( final FindBugs2 findBugs ) throws IOException, FilterException
    {
        if ( this.includeFilterFile != null )
        {
            if ( new File( this.includeFilterFile ).exists() )
            {
                findBugs.addFilter( this.includeFilterFile, true );
                this.getLog().debug( "  Using bug include filter " + this.includeFilterFile );
            }
            else
            {
                this.getLog().debug( "  No bug include filter " + this.includeFilterFile + " found" );
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
                findBugs.addFilter( this.excludeFilterFile, false );
                this.getLog().debug( "  Using bug exclude filter " + this.excludeFilterFile );
            }
            else
            {
                this.getLog().debug( "  No bug exclude filter " + this.excludeFilterFile + " found" );
            }
        }
        else
        {
            this.getLog().debug( "  No bug exclude filter." );
        }
    }

    /**
     * Adds the source files to the find bugs project. The return value of the method call <code>addFile</code> is
     * omited, because we are not interested if the java source is already added.
     * 
     * @param pSourceFiles
     *            The java sources (Type <code>java.io.File</code>) to add to the project.
     * @param findBugsProject
     *            The find bugs project to add the java source to.
     * 
     */
    protected void addJavaSourcesToFindBugsProject( final List pSourceFiles, final Project findBugsProject )
    {
        final Iterator iterator = pSourceFiles.iterator();
        while ( iterator.hasNext() )
        {
            final File currentSourceFile = (File) iterator.next();
            final String filePath = currentSourceFile.getAbsolutePath();
            findBugsProject.addFile( filePath );
        }
    }

    /**
     * Adds the specified plugins to findbugs. The coreplugin is always added first.
     * 
     * @param pLocale
     *            The locale to print out the messages. Used here to get the nameof the coreplugin from the properties.
     * @throws ArtifactNotFoundException
     *             If the coreplugin could not be found.
     * @throws ArtifactResolutionException
     *             If the coreplugin could not be resolved.
     * @throws MavenReportException
     *             If the findBugs plugins URL could not be resolved.
     * 
     */
    protected void addClassScreenerToFindBugs( final FindBugs2 findBugs )
    {

        if ( this.onlyAnalyze != null )
        {
            this.getLog().debug( "  Adding ClassScreener " );
            // The argument is a comma-separated list of classes and packages
            // to select to analyze. (If a list item ends with ".*",
            // it specifies a package, otherwise it's a class.)
            StringTokenizer stringToken = new StringTokenizer( this.onlyAnalyze, "," );
            while ( stringToken.hasMoreTokens() )
            {
                String stringTokenItem = stringToken.nextToken();
                if ( stringTokenItem.endsWith( ".-" ) )
                {
                    classScreener.addAllowedPrefix( stringTokenItem.substring( 0, stringTokenItem.length() - 1 ) );
                    this.getLog().info(
                                        " classScreener.addAllowedPrefix "
                                                        + ( stringTokenItem.substring( 0, stringTokenItem.length() - 1 ) ) );
                }
                else if ( stringTokenItem.endsWith( ".*" ) )
                {
                    classScreener.addAllowedPackage( stringTokenItem.substring( 0, stringTokenItem.length() - 1 ) );
                    this.getLog().info(
                                        " classScreener.addAllowedPackage "
                                                        + ( stringTokenItem.substring( 0, stringTokenItem.length() - 1 ) ) );
                }
                else
                {
                    classScreener.addAllowedClass( stringTokenItem );
                    this.getLog().info( " classScreener.addAllowedClass " + stringTokenItem );
                }
            }

            findBugs.setClassScreener( classScreener );

        }

        this.getLog().debug( "  Done Adding Class Screeners" );

    }

    /**
     * Adds the specified plugins to findbugs. The coreplugin is always added first.
     * 
     * @param pLocale
     *            The locale to print out the messages. Used here to get the nameof the coreplugin from the properties.
     * @throws ArtifactNotFoundException
     *             If the coreplugin could not be found.
     * @throws ArtifactResolutionException
     *             If the coreplugin could not be resolved.
     * @throws MavenReportException
     *             If the findBugs plugins URL could not be resolved.
     * 
     */
    protected void addPluginsToFindBugs( final Locale pLocale )
        throws ArtifactNotFoundException, ArtifactResolutionException, MavenReportException
    {

        URL corepluginpath = null;

        try
        {
            corepluginpath = this.getCorePluginPath( pLocale ).toURL();
        }
        catch ( final MalformedURLException pException )
        {
            throw new MavenReportException( "The core plugin has an invalid URL", pException );
        }

        this.getLog().debug( "  coreplugin Jar is located at " + corepluginpath.toString() );

        URL[] plugins;

        if ( this.pluginList != null )
        {
            this.getLog().debug( "  Adding Plugins " );
            final String[] pluginJars = this.pluginList.split( "," );

            plugins = new URL[pluginJars.length + 1];

            for ( int i = 0; i < pluginJars.length; i++ )
            {
                String pluginFile = pluginJars[i].trim();

                if ( !pluginFile.endsWith( ".jar" ) )
                {
                    throw new IllegalArgumentException( "Plugin File is not a Jar file: " + pluginFile );
                }

                try
                {
                    plugins[i + 1] = new File( pluginFile ).toURL();
                }
                catch ( final MalformedURLException pException )
                {
                    throw new MavenReportException( "The addin plugin has an invalid URL", pException );
                }
                this.getLog().debug( "  Adding Plugin: " + plugins[i + 1].toString() );

            }
        }
        else
        {
            plugins = new URL[1];
        }

        plugins[0] = corepluginpath;

        DetectorFactoryCollection.rawInstance().setPluginList( plugins );

        this.getLog().debug( "  Done Adding Plugins" );

    }

    /**
     * Adds the specified visitors to findbugs.
     * 
     * @param preferences
     *            The find bugs UserPreferences.
     * 
     */
    protected void addVisitorsToFindBugs( final UserPreferences preferences )
    {
        /*
         * This is done in this order to make sure only one of vistors or omitVisitors options is run. This is
         * consistent with the way the Findbugs commandline and Ant Tasks run.
         */
        if ( this.visitors != null || this.omitVisitors != null )
        {
            boolean enableVisitor = true;
            String[] visitorList;

            if ( this.omitVisitors != null )
            {
                enableVisitor = false;
                visitorList = this.omitVisitors.split( "," );
                this.getLog().debug( "  Omitting visitors : " + this.omitVisitors );

            }
            else
            {
                visitorList = this.visitors.split( "," );
                this.getLog().debug( "  Including visitors : " + this.visitors );
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
     * Lists absolute paths of java source files for denugging purposes.
     * 
     * @param pLocale
     *            The locale to print out the messages.
     * @param pSourceFiles
     *            List of source files.
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
     * Executes the generation of the report.
     * 
     * Callback from Maven Site Plugin or from AbstractMavenReport.execute() => generate().
     * 
     * @param pLocale
     *            the locale the report should be generated for
     * @throws MavenReportException
     *             if anything goes wrong
     * @see org.apache.maven.reporting.AbstractMavenReport #executeReport(java.util.Locale)
     */
    protected void executeReport( final Locale pLocale ) throws MavenReportException
    {
        if ( !skip )
        {
            FindBugs2 findBugs = null;
            this.debugSourceDirectory( pLocale, this.classFilesDirectory );

            if ( !canGenerateReport() )
            {
                getLog().info( "Output class directory doesn't exist. Skipping findbugs." );
                return;
            }

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

            try
            {

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
        }
    }

    /**
     * Retrieve the coreplugin module name
     * 
     * @param pLocale
     *            The locale to print out the messages.
     * @return corePluginName The coreplugin module name.
     * 
     */
    protected String getCorePlugin( final Locale pLocale )
    {
        final ResourceBundle bundle = getBundle( pLocale );
        final String corePluginName = bundle.getString( FINDBUGS_COREPLUGIN );

        return corePluginName;

    }

    /**
     * Get the File reference for the Findbugs core plugin.
     * 
     * @param pLocale
     *            The locale of the messages.
     * @return The File reference to the coreplugin JAR
     * @throws ArtifactNotFoundException
     *             If the coreplugin could not be found.
     * @throws ArtifactResolutionException
     *             If the coreplugin could not be resolved.
     * 
     */
    protected File getCorePluginPath( final Locale pLocale )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        for ( Iterator it = this.pluginArtifacts.iterator(); it.hasNext(); )
        {
            final Artifact artifact = (Artifact) it.next();
            if ( artifact.getArtifactId().equals( this.getCorePlugin( pLocale ) ) )
            {
                this.artifactResolver.resolve( artifact, this.remoteArtifactRepositories, this.localRepository );
                return artifact.getFile();
            }
        }
        return null;
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
     * Collects the java sources from the source roots.
     * 
     * @param pSourceDirectory
     *            The source directory to search for java sources.
     * @param pLocale
     *            The locale to print out the messages.
     * @return A list containing the java sources or an empty list if no java sources are found.
     * @throws IOException
     *             If there are problems searching for java sources.
     * 
     */
    protected List getJavaSources( final Locale pLocale, final File pSourceDirectory ) throws IOException
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
     * Returns the report output directory.
     * 
     * Called by AbstractMavenReport.execute() for creating the sink.
     * 
     * @return full path to the directory where the files in the site get copied to
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        new File( this.outputDirectory ).mkdirs();

        return this.outputDirectory;
    }

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
     * Returns the doxia site renderer.
     * 
     * @return the doxia Renderer
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected Renderer getSiteRenderer()
    {
        return this.siteRenderer;
    }

    /**
     * Returns the threshold parameter to use.
     * 
     * @return A valid threshold parameter.
     * 
     */
    protected ThresholdParameter getThresholdParameter()
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

        return thresholdParameter;

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
     *             Exception that occurs when an artifact file is used, but has not been resolved.
     * @throws IOException
     *             If filter file could not be read.
     * @throws FilterException
     *             If filter file was invalid.
     * @throws ArtifactNotFoundException
     *             If the coreplugin could not be found.
     * @throws ArtifactResolutionException
     *             If the coreplugin could not be resolved.
     * @throws MavenReportException
     *             If the findBugs plugins cannot be initialized
     * 
     */
    protected FindBugs2 initialiseFindBugs( final Locale pLocale, final List pSourceFiles )
        throws DependencyResolutionRequiredException, IOException, FilterException, ArtifactNotFoundException,
        ArtifactResolutionException, MavenReportException
    {
        final Sink sink = this.getSink();
        final ResourceBundle bundle = FindBugsMojo.getBundle( pLocale );
        final Log log = this.getLog();
        final EffortParameter effortParameter = this.getEffortParameter();

        final Project findBugsProject = new Project();

        this.getLog().info( "  Using FindBugs Version: " + edu.umd.cs.findbugs.Version.RELEASE );

        this.bugReporter = this.initialiseReporter( sink, bundle, log, effortParameter );

        if ( this.xmlOutput )
        {
            this.getLog().info( "  Using the xdoc format" );

            if ( !this.xmlOutputDirectory.exists() )
            {
                if ( !this.xmlOutputDirectory.mkdirs() )
                {
                    throw new MavenReportException( "Cannot create xml output directory" );
                }

            }

            BugReporter htmlBugReporter = this.bugReporter;
            this.bugReporter = new XDocsReporter( htmlBugReporter );

            ( (XDocsReporter) this.bugReporter ).setOutputWriter( new FileWriter( new File( this.xmlOutputDirectory
                            + "/findbugs.xml" ) ) );
            ( (XDocsReporter) this.bugReporter ).setResourceBundle( bundle );
            ( (XDocsReporter) this.bugReporter ).setLog( log );
            ( (XDocsReporter) this.bugReporter ).setEffort( this.getEffortParameter() );
            ( (XDocsReporter) this.bugReporter ).setThreshold( this.getThresholdParameter() );
        }

        this.addJavaSourcesToFindBugsProject( pSourceFiles, findBugsProject );
        this.addClasspathEntriesToFindBugsProject( findBugsProject );

        final FindBugs2 findBugs = new FindBugs2();
        findBugs.setBugReporter( this.bugReporter );
        findBugs.setProject( findBugsProject );

        if ( !pluginLoaded )
        {
            this.addPluginsToFindBugs( pLocale );
            pluginLoaded = true;
        }

        final UserPreferences preferences = UserPreferences.createDefaultUserPreferences();

        this.addVisitorsToFindBugs( preferences );

        findBugs.setRelaxedReportingMode( this.relaxed.booleanValue() );
        findBugs.setUserPreferences( preferences );
        findBugs.setAnalysisFeatureSettings( effortParameter.getValue() );
        findBugs.setDetectorFactoryCollection( DetectorFactoryCollection.rawInstance() );

        this.setFindBugsDebug( findBugs );
        this.addFiltersToFindBugs( findBugs );
        this.addClassScreenerToFindBugs( findBugs );

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
        ThresholdParameter thresholdParameter = this.getThresholdParameter();

        final boolean isJXRPluginEnabled = this.isJXRPluginEnabled( pBundle );
        final Reporter bugReporter =
            new Reporter( pSink, pBundle, pLog, thresholdParameter, isJXRPluginEnabled, pEffortParameter );
        bugReporter.setPriorityThreshold( thresholdParameter.getValue() );

        return bugReporter;
    }

    /**
     * Determines if the JXR-Plugin is included in the report section of the POM.
     * 
     * @param pBundle
     *            The bundle to load the artifactIf of the jxr plugin.
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

    /**
     * Sets the Debug Level
     * 
     * @param findBugs
     *            The find bugs to add debug level information.
     * 
     */
    protected void setFindBugsDebug( final FindBugs2 findBugs )
    {
        System.setProperty( "findbugs.classpath.debug", this.debug.toString() );
        System.setProperty( "findbugs.debug", this.debug.toString() );

        if ( this.debug.booleanValue() )
        {
            this.getLog().info( "  Debugging is On" );
        }
        else
        {
            this.getLog().info( "  Debugging is Off" );
        }
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

}
