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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.model.ReportPlugin;
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
 * $Revision: 38 $
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
    private static final String FINDBUGS_PATH = "findbugs/coreplugin/";

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
     * The key to get the jxr-plugin artifactId from the bundle.
     * 
     */
    private static final String JXR_ARTIFACT_ID_KEY = "report.findbugs.jxrplugin.artifactid";

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
        FindBugs findBugs = null;

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

        try
        {
            findBugs = initialiseFindBugs( pLocale, sourceFiles );
        }
        catch ( DependencyResolutionRequiredException pException )
        {
            final MavenReportException exception = new MavenReportException( "Failed executing FindBugs", pException );
            throw exception;
        }

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
     * @throws DependencyResolutionRequiredException Exception that occurs when an artifact file is used, but has not been resolved.
     * 
     */
    protected FindBugs initialiseFindBugs( final Locale pLocale, final List pSourceFiles )
        throws DependencyResolutionRequiredException
    {
        final Sink sink = getSink();
        final ResourceBundle bundle = getBundle( pLocale );
        final Log log = getLog();

        // Load Findbugs detector plugin
        final String version = bundle.getString( FINDBUGS_VERSION_KEY );
        final String basedir = localRepository.getBasedir();
        final String corepluginpath = basedir + File.separator + FINDBUGS_PATH + version + File.separator
            + FINDBUGS_COREPLUGIN + "-" + version + ".jar";

        final File[] plugins = new File[1];
        plugins[0] = new File( corepluginpath );
        DetectorFactoryCollection.setPluginList( plugins );

        final Reporter bugReporter = initialiseReporter( sink, bundle, log );
        final Project findBugsProject = new Project();
        addJavaSourcesToFindBugsProject( pSourceFiles, findBugsProject );
        addClasspathEntriesToFindBugsProject( findBugsProject );

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

        final boolean isJXRPluginEnabled = isJXRPluginEnabled(pBundle);
        final Reporter bugReporter = new Reporter( pSink, pBundle, pLog, thresholdParameter, isJXRPluginEnabled );
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

    /** Adds the dependend libraries of the project to the findbugs aux classpath.
     *
     * @param pFindBugsProject The find bugs project to add the aux classpath entries.
     * @throws DependencyResolutionRequiredException Exception that occurs when an artifact file is used, but has not been resolved.
     * 
     */
    protected void addClasspathEntriesToFindBugsProject( final Project pFindBugsProject )
        throws DependencyResolutionRequiredException
    {
        final List entries = getProject().getCompileClasspathElements();
        final Iterator iterator = entries.iterator();
        while ( iterator.hasNext() )
        {
            final String currentEntry = (String) iterator.next();
            pFindBugsProject.addAuxClasspathEntry( currentEntry );
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

    /** Determines if the JXR-Plugin is included in the report section of the POM.
     *
     * @param pBundle The bundle to load the artifactIf of the jxr plugin.
     * @return True if the JXR-Plugin is included in the POM, false otherwise.
     * 
     */
    protected boolean isJXRPluginEnabled(final ResourceBundle pBundle) {
        boolean isEnabled = false;

        final String artifactId = pBundle.getString( JXR_ARTIFACT_ID_KEY );

        final List reportPlugins = getProject().getReportPlugins();
        final Iterator iterator = reportPlugins.iterator();
        while(iterator.hasNext()) {
            final ReportPlugin currentPlugin = (ReportPlugin) iterator.next();
            final String currentArtifactId = currentPlugin.getArtifactId();
            if (artifactId.equals( currentArtifactId )) {
                isEnabled = true;
            }
        }

        return isEnabled;
    }
}
