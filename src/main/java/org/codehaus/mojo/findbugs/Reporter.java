package org.codehaus.mojo.findbugs;

/* Copyright (c) 2004, The Codehaus
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

import java.util.ResourceBundle;

import org.apache.bcel.classfile.JavaClass;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.doxia.sink.Sink;

import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SourceLineAnnotation;

/**
 * The reporter controls the generation of the FindBugs report. It contains call
 * back methods which gets called by FindBugs if a bug is found.
 * 
 * @author $Author: cyrill $
 * @author <a href="mailto:ruettimac@mac.com">Cyrill Ruettimann</a>
 * @version $Id$
 */
public final class Reporter
    extends AbstractBugReporter
{

    /**
     * The key to get the value if the line number is not available.
     * 
     */
    private static final String NOLINE_KEY = "report.findbugs.noline";

    /**
     * The key to get the column title for the line.
     * 
     */
    private static final String COLUMN_LINE_KEY = "report.findbugs.column.line";

    /**
     * The key to get the column title for the bug.
     * 
     */
    private static final String COLUMN_BUG_KEY = "report.findbugs.column.bug";

    /**
     * The key to get the column title for the category.
     * 
     */
    private static final String COLUMN_CATEGORY_KEY = "report.findbugs.column.category";

    /**
     * The key to get the column title for the details.
     * 
     */
    private static final String COLUMN_DETAILS_KEY = "report.findbugs.column.details";

    /**
     * The key to get the report title of the Plug-In from the bundle.
     * 
     */
    private static final String REPORT_TITLE_KEY = "report.findbugs.reporttitle";

    /**
     * The key to get the report link title of the Plug-In from the bundle.
     * 
     */
    private static final String LINKTITLE_KEY = "report.findbugs.linktitle";

    /**
     * The key to get the report link of the Plug-In from the bundle.
     * 
     */
    private static final String LINK_KEY = "report.findbugs.link";

    /**
     * The key to get the name of the Plug-In from the bundle.
     * 
     */
    private static final String NAME_KEY = "report.findbugs.name";

    /**
     * The key to get the files title of the Plug-In from the bundle.
     * 
     */
    private static final String FILES_KEY = "report.findbugs.files";

    /**
     * The key to get the threshold of the report from the bundle.
     * 
     */
    private static final String THRESHOLD_KEY = "report.findbugs.threshold";

    /** 
     * The character to separate URL tokens.
     * 
     */
    private static final String URL_SEPARATOR = "/";

    /** 
     * The key to get the jxr-plugin path prefix.
     * 
     */
    private static final String JXR_PATHPREFIX_KEY = "report.findbugs.jxrplugin.pathprefix";

    /**
     * The key to get the effort of the report from the bundle.
     * 
     */
    private static final String EFFORT_KEY = "report.findbugs.effort";

    /**
     * The key to get the link to FindBugs description page from the bundle.
     * 
     */
    private static final String DETAILSLINK_KEY = "report.findbugs.detailslink";

    /**
     * The key to get the version title for FindBugs from the bundle.
     * 
     */
    private static final String VERSIONTITLE_KEY = "report.findbugs.versiontitle";

    /**
     * The sink to write the report to.
     * 
     */
    private final transient Sink mSink;

    /**
     * The bundle to get the messages from.
     * 
     */
    private final transient ResourceBundle mBundle;

    /**
     * The logger to write logs to.
     * 
     */
    private final transient Log mLog;

    /**
     * The threshold of bugs severity.
     * 
     */
    private final transient ThresholdParameter mThreshold;

    /**
     * The used effort for searching bugs.
     * 
     */
    private final transient EffortParameter mEffort;

    /**
     * The name of the current class which is analysed by FindBugs.
     * 
     */
    private transient String mCurrentClassName;

    /**
     * Signals if the report for the current class is opened.
     * 
     */
    private transient boolean mIsCurrentClassReportOpened = false;

    /** 
     * Signals if the jxr report plugin is enabled.
     * 
     */
    private transient boolean mIsJXRReportEnabled = false;

    /**
     * Hide default constructor.
     * 
     */
    private Reporter()
    {
        super();

        this.mSink = null;
        this.mBundle = null;
        this.mLog = null;
        this.mThreshold = null;
        this.mEffort = null;
    }

    /**
     * Default constructor.
     * 
     * @param pSink
     *            The sink to generate the report.
     * @param pBundle
     *            The resource bundle to get the messages from.
     * @param pLog
     *            The logger.
     * @param pThreshold The threshold for the report.
     * @param pIsJXRReportEnabled Is the jxr report plugin enabled.
     * @param pEffort The used effort.
     */
    public Reporter( final Sink pSink, final ResourceBundle pBundle, final Log pLog,
                     final ThresholdParameter pThreshold, final boolean pIsJXRReportEnabled,
                     final EffortParameter pEffort )
    {
        super();

        if ( pSink == null )
        {
            throw new IllegalArgumentException( "pSink not allowed to be null" );
        }

        if ( pBundle == null )
        {
            throw new IllegalArgumentException( "pBundle not allowed to be null" );
        }

        if ( pLog == null )
        {
            throw new IllegalArgumentException( "pLog not allowed to be null" );
        }

        if ( pThreshold == null )
        {
            throw new IllegalArgumentException( "pThreshold not allowed to be null" );
        }

        if ( pEffort == null )
        {
            throw new IllegalArgumentException( "pEffort not allowed to be null" );
        }

        this.mSink = pSink;
        this.mBundle = pBundle;
        this.mLog = pLog;
        this.mThreshold = pThreshold;
        this.mIsJXRReportEnabled = pIsJXRReportEnabled;
        this.mEffort = pEffort;

        this.initialiseReport();
    }

    /**
     * Initialises the report.
     */
    private void initialiseReport()
    {
        this.mSink.head();
        this.mSink.title();
        this.mSink.text( this.getReportTitle() );
        this.mSink.title_();
        this.mSink.head_();

        this.mSink.body();

        // the title of the report
        this.mSink.section1();
        this.mSink.sectionTitle1();
        this.mSink.text( this.getReportTitle() );
        this.mSink.sectionTitle1_();

        // information about FindBugs
        this.mSink.paragraph();
        this.mSink.text( this.getReportLinkTitle() + " " );
        this.mSink.link( this.getFindBugsLink() );
        this.mSink.text( this.getFindBugsName() );
        this.mSink.link_();
        this.mSink.paragraph_();

        this.mSink.paragraph();
        this.mSink.text( getVersionTitle() + " " );
        this.mSink.italic();
        this.mSink.text( getFindBugsVersion() );
        this.mSink.italic_();
        this.mSink.paragraph_();

        this.mSink.paragraph();
        this.mSink.text( this.getThresholdTitle() + " " );
        this.mSink.italic();
        this.mSink.text( this.mThreshold.getName() );
        this.mSink.italic_();
        this.mSink.paragraph_();

        this.mSink.paragraph();
        this.mSink.text( this.getEffortTitle() + " " );
        this.mSink.italic();
        this.mSink.text( this.mEffort.getName() );
        this.mSink.italic_();
        this.mSink.paragraph_();

        // the files section
        this.mSink.section1_();
        this.mSink.sectionTitle1();
        this.mSink.text( this.getFilesTitle() );
        this.mSink.sectionTitle1_();
    }

    /**
     * @param pBugInstance the bug to report
     * @see edu.umd.cs.findbugs.AbstractBugReporter
     *      #doReportBug(edu.umd.cs.findbugs.BugInstance)
     */
    protected void doReportBug( final BugInstance pBugInstance )
    {
        this.mLog.debug( "  Found a bug: " + pBugInstance.getMessage() );

        this.addBugReport( pBugInstance );
    }

    /**
     * Report a queued error.
     * @param pAnalysisError the queued error
     * @see edu.umd.cs.findbugs.AbstractBugReporter
     *      #reportAnalysisError(edu.umd.cs.findbugs.AnalysisError)
     */
    public void reportAnalysisError( final AnalysisError pAnalysisError )
    {
        this.mLog.debug( "  Found an analysisError: " + pAnalysisError.getMessage() );
    }

    /**
     * Report a missing class.
     * @param pMissingClass the name of the class
     * @see edu.umd.cs.findbugs.AbstractBugReporter
     *      #reportMissingClass(java.lang.String)
     */
    public void reportMissingClass( final String pMissingClass )
    {
        this.mLog.debug( "  Found a missing class: " + pMissingClass );
    }

    /**
     * @see edu.umd.cs.findbugs.BugReporter#finish()
     */
    public void finish()
    {
        this.mLog.debug( "Finished searching for bugs!" );

        // close the last class report section
        if ( this.mIsCurrentClassReportOpened )
        {
            this.closeClassReportSection();
        }

        // close the report, write it
        this.mSink.section1_();
        this.mSink.body_();
        this.mSink.flush();
        this.mSink.close();
    }

    /**
     * Get the real bug reporter at the end of a chain of delegating bug reporters.
     * All non-delegating bug reporters should simply "return this".
     * 
     * @return the real bug reporter at the end of the chain, or
     *          this object if there is no delegation
     * @see edu.umd.cs.findbugs.BugReporter#getRealBugReporter()
     */
    public BugReporter getRealBugReporter()
    {
        return this;
    }

    /**
     * Observe a class.
     *
     * @param clazz the class
     * @see edu.umd.cs.findbugs.ba.ClassObserver
     *      #observeClass(org.apache.bcel.classfile.JavaClass)
     */
    public void observeClass( final JavaClass clazz )
    {
        this.mLog.debug( "Observe class: " + clazz.getClassName() );

        this.mCurrentClassName = clazz.getClassName();

        if ( this.mIsCurrentClassReportOpened )
        {
            this.closeClassReportSection();
        }

        this.mIsCurrentClassReportOpened = false;
    }

    /**
     * Closes the class report section.
     */
    protected void closeClassReportSection()
    {
        this.mSink.table_();
        this.mSink.section2_();
    }

    /**
     * Gets the Findbugs Version title of the report.
     * 
     * @return The Findbugs Version used on the report.
     * 
     */
    protected String getVersionTitle()
    {
        final String versionTitle = this.mBundle.getString( VERSIONTITLE_KEY );

        return versionTitle;
    }

    /**
     * Gets the Findbugs Version of the report.
     * 
     * @return The Findbugs Version used on the report.
     * 
     */
    protected String getFindBugsVersion()
    {
        return edu.umd.cs.findbugs.Version.RELEASE;
    }

    /**
     * Gets the report title.
     * 
     * @return The report title.
     * 
     */
    protected String getReportTitle()
    {
        final String reportTitle = this.mBundle.getString( Reporter.REPORT_TITLE_KEY );

        return reportTitle;
    }

    /**
     * Gets the name of the link to FindBugs.
     * 
     * @return The report link title.
     * 
     */
    protected String getReportLinkTitle()
    {
        final String reportLink = this.mBundle.getString( Reporter.LINKTITLE_KEY );

        return reportLink;
    }

    /**
     * Gets the link to FindBugs.
     * 
     * @return The report link.
     * 
     */
    protected String getFindBugsLink()
    {
        final String link = this.mBundle.getString( Reporter.LINK_KEY );

        return link;
    }

    /**
     * Gets the name of FindBugs.
     * 
     * @return The name of FindBugs.
     * 
     */
    protected String getFindBugsName()
    {
        final String name = this.mBundle.getString( Reporter.NAME_KEY );

        return name;
    }

    /**
     * Gets the title for the files title.
     * 
     * @return The name of FindBugs.
     * 
     */
    protected String getFilesTitle()
    {
        final String fileTitle = this.mBundle.getString( Reporter.FILES_KEY );

        return fileTitle;
    }

    /**
     * Gets the threshold title of the report.
     * 
     * @return The threshold title of the report.
     * 
     */
    protected String getThresholdTitle()
    {
        final String threshholdTitle = this.mBundle.getString( Reporter.THRESHOLD_KEY );

        return threshholdTitle;
    }

    /**
     * Gets the effort title of the report.
     * 
     * @return The effort title of the report.
     * 
     */
    protected String getEffortTitle()
    {
        final String effortTitle = this.mBundle.getString( Reporter.EFFORT_KEY );

        return effortTitle;
    }

    /**
     * Gets the link to details description on findbugs site.
     * 
     * @param pType the bug type
     * @return The report link.
     * 
     */
    protected String getDetailsLink( final String pType )
    {
        final String link = this.mBundle.getString( Reporter.DETAILSLINK_KEY )
          + "#" + pType;

        return link;
    }

    /**
     * Adds a bug to the report. A call to <code>initialiseClassReport</code>
     * is needed prior to call <code>addBugReport</code>.
     * 
     * @param pBugInstance
     *            The bug to add.
     * 
     */
    protected void addBugReport( final BugInstance pBugInstance )
    {
        final SourceLineAnnotation line = pBugInstance.getPrimarySourceLineAnnotation();
        final BugPattern pattern = pBugInstance.getBugPattern();
        final String lineNumber = this.valueForLine( line );
        final String category = pattern.getCategory();
        final String type = pattern.getType();

        if ( !this.mIsCurrentClassReportOpened )
        {
            this.openClassReportSection();
            this.mIsCurrentClassReportOpened = true;
        }

        this.mSink.tableRow();

        // bug
        this.mSink.tableCell();
        this.mSink.text( pBugInstance.getMessageWithoutPrefix() );
        this.mSink.tableCell_();

        // category
        this.mSink.tableCell();
        this.mSink.text( category );
        this.mSink.tableCell_();

        // description link
        this.mSink.tableCell();
        this.mSink.link( this.getDetailsLink( type ) );
        this.mSink.text( type );
        this.mSink.link_();
        this.mSink.tableCell_();

        // line
        this.mSink.tableCell();
        if ( this.mIsJXRReportEnabled )
        {
            this.mSink.rawText( this.assembleJXRHyperlink( line, lineNumber ) );
        }
        else
        {
            this.mSink.text( lineNumber );
        }

        this.mSink.tableCell_();
        this.mSink.tableRow_();
    }

    /**
     * Return the value to display. If FindBugs does not provide a line number,
     * a default message is returned. The line number otherwise.
     * 
     * @param pLine
     *            The line to get the value from.
     * @return The line number the bug appears or a statement that there is no
     *         source line available.
     * 
     */
    protected String valueForLine( final SourceLineAnnotation pLine )
    {
        String value = null;

        if ( pLine == null )
        {
            value = this.mBundle.getString( Reporter.NOLINE_KEY );
        }
        else
        {
            final int startLine = pLine.getStartLine();
            final int endLine = pLine.getEndLine();

            if ( startLine == endLine )
            {
                if ( startLine == -1 )
                {
                    value = this.mBundle.getString( Reporter.NOLINE_KEY );
                }
                else
                {
                    value = String.valueOf( startLine );
                }
            }
            else
            {
                value = String.valueOf( startLine ) + "-" + String.valueOf( endLine );
            }
        }

        return value;
    }

    /** Assembles the hyperlink to point to the source code.
     *
     * @param pLine The line number object with the bug.
     * @param pLineNumber The line number to show in the hyperlink.
     * @return The hyperlink which points to the code.
     * 
     */
    protected String assembleJXRHyperlink( final SourceLineAnnotation pLine, final String pLineNumber )
    {
        String hyperlink = null;
        final String prefix = this.mBundle.getString( Reporter.JXR_PATHPREFIX_KEY );
        final String path = prefix + Reporter.URL_SEPARATOR + this.mCurrentClassName.replaceAll( "[.]", "/" );

        if ( pLine == null )
        {
            hyperlink = "<a href=" + path + ".html>" + pLineNumber + "</a>";
        }
        else
        {
            hyperlink = "<a href=" + path + ".html#" + pLine.getStartLine() + ">" + pLineNumber + "</a>";
        }

        return hyperlink;
    }

    /**
     * Initialised a bug report section in the report for a particular class.
     */
    protected void openClassReportSection()
    {
        final String columnBugText = this.mBundle.getString( Reporter.COLUMN_BUG_KEY );
        final String columnBugCategory = this.mBundle.getString( Reporter.COLUMN_CATEGORY_KEY );
        final String columnDescriptionLink = this.mBundle.getString( Reporter.COLUMN_DETAILS_KEY );
        final String columnLineText = this.mBundle.getString( Reporter.COLUMN_LINE_KEY );

        this.mSink.section2();
        this.mSink.sectionTitle2();
        this.mSink.text( this.mCurrentClassName );
        this.mSink.sectionTitle2_();
        this.mSink.table();
        this.mSink.tableRow();

        // bug
        this.mSink.tableHeaderCell();
        this.mSink.text( columnBugText );
        this.mSink.tableHeaderCell_();

        // category
        this.mSink.tableHeaderCell();
        this.mSink.text( columnBugCategory );
        this.mSink.tableHeaderCell_();

        // description link
        this.mSink.tableHeaderCell();
        this.mSink.text( columnDescriptionLink );
        this.mSink.tableHeaderCell_();

        // line
        this.mSink.tableHeaderCell();
        this.mSink.text( columnLineText );
        this.mSink.tableHeaderCell_();

        this.mSink.tableRow_();
    }
}
