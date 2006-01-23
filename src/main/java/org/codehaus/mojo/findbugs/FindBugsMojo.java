/*Copyright (c) 2004, The Codehaus
 *
 *Permission is hereby granted, free of charge, to any person obtaining a copy of
 *this software and associated documentation files (the "Software"), to deal in
 *the Software without restriction, including without limitation the rights to
 *use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 *of the Software, and to permit persons to whom the Software is furnished to do
 *so, subject to the following conditions:
 *
 *The above copyright notice and this permission notice shall be included in all
 *copies or substantial portions of the Software.
 *
 *THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *SOFTWARE.
 */

package org.codehaus.mojo.findbugs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;

import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Generates a FindBugs report.
 * 
 * @goal findbugs
 * @description Generates a FindBugs Report.
 * @execute phase="compile"
 * 
 * @author <a href="mailto:ruettimac@mac.com">Cyrill Ruettimann</a>
 * 
 * $Revision: 34 $
 * $Date$
 * $Author: cyrill $
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
     * The regex pattern to search for java sources.
     * 
     */
    private static final String JAVA_REGEX_PATTERN = "**/*.class";

    /**
     * The path to the findbugs coreplugin library.
     * 
     */
    private static final String FINDBUGS_PATH = "findbugs" + File.separator + "coreplugin" + File.separator;

    /**
     * The name of the coreplugin.
     * 
     */
    private static final String FINDBUGS_COREPLUGIN = "coreplugin";

    /**
     * The key to get the findbugs version from the bundle.
     * 
     */
    private static final String FINDBUGS_VERSION_KEY = "report.findbugs.version";

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
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     */
    private transient File classFilesDirectory;

    /**
     * Maven Project
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private transient MavenProject project;

    /**
     * @parameter
     */
    private transient String threshold;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private transient DefaultArtifactRepository localRepository;

    static
    {
        // Tell FindBugs that we do not have a findugs.home
        // Activate FindBugs mode jaws
        System.setProperty( "findbugs.jaws", "true" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return PLUGIN_NAME;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( final Locale pLocale )
    {
        final ResourceBundle bundle = getBundle( pLocale );
        final String name = bundle.getString( NAME_KEY );

        return name;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( final Locale pLocale )
    {
        final ResourceBundle bundle = getBundle( pLocale );
        final String description = bundle.getString( DESCRIPTION_KEY );

        return description;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        final boolean exists = classFilesDirectory.exists();

        return exists;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport
     *      #executeReport(java.util.Locale)
     */
    protected void executeReport( final Locale pLocale )
        throws MavenReportException
    {
        List sourceFiles = new ArrayList();

        debugSourceDirectory( pLocale, classFilesDirectory );

        try
        {
            sourceFiles = getJavaSources( pLocale, classFilesDirectory );
        }
        catch ( final IOException pException )
        {
            final MavenReportException exception = new MavenReportException( "A java source file could not be added",
                                                                             pException );
            throw exception;
        }

        final FindBugs findBugs = initialiseFindBugs( pLocale, sourceFiles );

        try
        {
            findBugs.execute();
        }
        catch ( final IOException pException )
        {
            final MavenReportException exception = new MavenReportException( "Failed executing FindBugs", pException );
            throw exception;
        }
        catch ( final InterruptedException pException )
        {
            final MavenReportException exception = new MavenReportException( "Failed executing FindBugs", pException );
            throw exception;
        }
        catch ( final Exception pException )
        {
            final MavenReportException exception = new MavenReportException( "Failed executing FindBugs", pException );
            throw exception;
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
     * @throws MavenReportException 
     * 
     */
    protected FindBugs initialiseFindBugs( final Locale pLocale, final List pSourceFiles )
        throws MavenReportException
    {
        final Sink sink = getSink();
        final ResourceBundle bundle = getBundle( pLocale );
        final Log log = getLog();

        // Load Findbugs detector plugin
        final String version = bundle.getString( FINDBUGS_VERSION_KEY );
        final String baseDirectory = localRepository.getBasedir();
        final String corepluginpath = baseDirectory + File.separator + FINDBUGS_PATH + version + File.separator
            + FINDBUGS_COREPLUGIN + "-" + version + ".jar";

        final File[] plugins = new File[1];
        plugins[0] = new File( corepluginpath );
        DetectorFactoryCollection.setPluginList( plugins );

        final Reporter bugReporter = initialiseReporter( sink, bundle, log );
        final Project findBugsProject = new Project();
        addJavaSourcesToFindBugsProject( pSourceFiles, findBugsProject );

        final FindBugs findBugs = new FindBugs( bugReporter, findBugsProject );

        final UserPreferences preferences = UserPreferences.createDefaultUserPreferences();
        preferences.enableAllDetectors( true );
        findBugs.setUserPreferences( preferences );

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
     * @return An initialised reporter.
     * 
     */
    protected Reporter initialiseReporter( final Sink pSink, final ResourceBundle pBundle, final Log pLog )
    {
        ThresholdParameter thresholdParameter = ThresholdParameter.DEFAULT;

        if ( threshold == null )
        {
            getLog().info( "  No threshold provided, using default threshold." );
        }
        else
        {
            if ( threshold.equals( ThresholdParameter.HIGH.getName() ) )
            {
                thresholdParameter = ThresholdParameter.HIGH;
                getLog().info( "  Using high threshold." );
            }
            else if ( threshold.equals( ThresholdParameter.NORMAL.getName() ) )
            {
                thresholdParameter = ThresholdParameter.NORMAL;
                getLog().info( "  Using normal threshold." );
            }
            else if ( threshold.equals( ThresholdParameter.LOW.getName() ) )
            {
                thresholdParameter = ThresholdParameter.LOW;
                getLog().info( "  Using low threshold." );
            }
            else if ( threshold.equals( ThresholdParameter.EXP.getName() ) )
            {
                thresholdParameter = ThresholdParameter.EXP;
                getLog().info( "  Using exp threshold." );
            }
            else if ( threshold.equals( ThresholdParameter.IGNORE.getName() ) )
            {
                thresholdParameter = ThresholdParameter.IGNORE;
                getLog().info( "  Using ignore threshold." );
            }
            else
            {
                getLog().info( "  Threshold not recognised, using default threshold" );
            }
        }

        final Reporter bugReporter = new Reporter( pSink, pBundle, pLog, thresholdParameter );
        bugReporter.setPriorityThreshold( thresholdParameter.getValue() );

        return bugReporter;
    }

    /**
     * Collects the java sources from the source roots.
     * 
     * @param pSourceDirectory
     *            The source directory to search for java sources.
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
            final List files = FileUtils.getFiles( pSourceDirectory, JAVA_REGEX_PATTERN, null );
            sourceFiles.addAll( files );
        }

        debugJavaSources( pLocale, sourceFiles );

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
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
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
        final ResourceBundle bundle = ResourceBundle.getBundle( BUNDLE_NAME, pLocale, loader );

        return bundle;
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
        final ResourceBundle bundle = getBundle( pLocale );
        final String sourceRootMessage = bundle.getString( SOURCE_ROOT_KEY );
        getLog().debug( "  " + sourceRootMessage );
        getLog().debug( "    " + pSourceDirectory.getAbsolutePath() );
    }

    protected void debugJavaSources( final Locale pLocale, final List pSourceFiles )
    {
        final ResourceBundle bundle = getBundle( pLocale );
        final String javaSourceMessage = bundle.getString( JAVA_SOURCES_KEY );
        getLog().debug( "  " + javaSourceMessage );

        final Iterator iterator = pSourceFiles.iterator();
        while ( iterator.hasNext() )
        {
            final File currentFile = (File) iterator.next();
            getLog().debug( "    " + currentFile.getAbsolutePath() );
        }
    }
}
