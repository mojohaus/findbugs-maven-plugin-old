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

import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.logging.Log;

import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.TextUIBugReporter;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * The reporter controls the generation of the FindBugs report. It contains call back methods which gets called by
 * FindBugs if a bug is found.
 * 
 * @author $Author: cyrill $
 * @author <a href="mailto:ruettimac@mac.com">Cyrill Ruettimann</a>
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id$
 */
public final class Reporter extends TextUIBugReporter
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
     * The key to get the column title for the bugs.
     * 
     */
    private static final String COLUMN_BUGS_KEY = "report.findbugs.column.bugs";

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
     * The key to get the files title of the Plug-In from the bundle.
     * 
     */
    private static final String SUMMARY_KEY = "report.findbugs.summary";

    /**
     * The key to column title for the Class.
     * 
     */
    private static final String COLUMN_CLASS_KEY = "report.findbugs.column.class";

    /**
     * The key to column title for the Classes.
     * 
     */
    private static final String COLUMN_CLASSES_KEY = "report.findbugs.column.classes";

    /**
     * The key to column title for the errors.
     * 
     */
    private static final String COLUMN_ERRORS_KEY = "report.findbugs.column.errors";

    /**
     * The key to column title for the files.
     * 
     */
    private static final String COLUMN_FILES_KEY = "report.findbugs.column.files";

    /**
     * The key to column title for the files.
     * 
     */
    private static final String COLUMN_MISSINGCLASSES_KEY = "report.findbugs.column.missingclasses";

    /**
     * The sink to write the report to.
     * 
     */
    private final Sink sink;

    /**
     * The bundle to get the messages from.
     * 
     */
    private final ResourceBundle bundle;

    /**
     * The logger to write logs to.
     * 
     */
    private final Log mavenLog;

    /**
     * The threshold of bugs severity.
     * 
     */
    private final ThresholdParameter threshold;

    /**
     * The used effort for searching bugs.
     * 
     */
    private final EffortParameter effort;

    /**
     * The name of the current class which is analysed by FindBugs.
     * 
     */
    private String currentClassName;

    /**
     * Signals if the report for the current class is opened.
     * 
     */
    private boolean mIsCurrentClassReportOpened = false;

    /**
     * Signals if the jxr report plugin is enabled.
     * 
     */
    private boolean isJXRReportEnabled = false;

    /**
     * The Collection of Bugs and Error collected during analysis.
     * 
     */
    private SortedBugCollection bugCollection = new SortedBugCollection();

    /**
     * The running total of bugs reported.
     * 
     */
    private int bugCount;

    /**
     * The running total of missing classes reported.
     * 
     */
    private int missingClassCount;

    /**
     * The running total of files analyzed.
     * 
     */
    private int fileCount;

    /**
     * The Set of missing classes names reported.
     * 
     */
    private Set missingClassSet = new HashSet();

    /**
     * The running total of errors reported.
     * 
     */
    private int errorCount;

    /**
     * Default constructor.
     * 
     * @param pSink
     *            The sink to generate the report.
     * @param pBundle
     *            The resource bundle to get the messages from.
     * @param pLog
     *            The logger.
     * @param pThreshold
     *            The threshold for the report.
     * @param isJXRReportEnabled
     *            Is the jxr report plugin enabled.
     * @param pEffort
     *            The used effort.
     */
    public Reporter( final Sink pSink, final ResourceBundle pBundle, final Log pLog,
                     final ThresholdParameter pThreshold, final boolean isJXRReportEnabled,
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

        this.sink = pSink;
        this.bundle = pBundle;
        this.mavenLog = pLog;
        this.threshold = pThreshold;
        this.isJXRReportEnabled = isJXRReportEnabled;
        this.effort = pEffort;
        this.currentClassName = "";

        this.bugCount = 0;
        this.missingClassCount = 0;
        this.errorCount = 0;
        this.fileCount = 0;

        this.initialiseReport();
    }

    /**
     * Hide default constructor.
     * 
     */
    private Reporter()
    {
        super();

        this.sink = null;
        this.bundle = null;
        this.mavenLog = null;
        this.threshold = null;
        this.effort = null;
    }

    /**
     * @see edu.umd.cs.findbugs.BugReporter#finish()
     */
    public void finish()
    {
        this.mavenLog.debug( "Finished searching for bugs!" );

        this.printSummary();

        this.printFilesSummary();

        this.mIsCurrentClassReportOpened = false;

        for ( Iterator i = this.bugCollection.iterator(); i.hasNext(); )
        {
            BugInstance bugInstance = ( BugInstance ) i.next();

            this.mavenLog.debug( "Annotation Class is " + bugInstance.getPrimarySourceLineAnnotation().getClassName() );
            this.mavenLog.debug( "Class is " + this.currentClassName );
            this.mavenLog.debug( " " );

            if ( !bugInstance.getPrimarySourceLineAnnotation().getClassName().equals( this.currentClassName ) )
            {

                this.currentClassName = bugInstance.getPrimarySourceLineAnnotation().getClassName();

                if ( this.mIsCurrentClassReportOpened )
                {
                    this.closeClassReportSection();
                    this.mIsCurrentClassReportOpened = false;
                }
            }

            this.printBug( bugInstance );
        }

        // close the last class report section
        if ( this.mIsCurrentClassReportOpened )
        {
            this.closeClassReportSection();
        }

        // close the report, write it
        this.sink.section1_();
        this.sink.body_();
        this.sink.flush();
        this.sink.close();

        this.mavenLog.debug( "bugCount = " + this.bugCount );
        this.mavenLog.debug( "errorCount = " + this.errorCount );
        this.mavenLog.debug( "missingClassCount = " + this.missingClassCount );
    }

    /**
     * Get the real bug reporter at the end of a chain of delegating bug reporters. All non-delegating bug reporters
     * should simply "return this".
     * 
     * @return the real bug reporter at the end of the chain, or this object if there is no delegation
     * @see edu.umd.cs.findbugs.BugReporter#getRealBugReporter()
     */
    public BugReporter getRealBugReporter()
    {
        return this;
    }

    /**
     * Observe a class.
     * 
     * @param clazz
     *            the class
     * @see edu.umd.cs.findbugs.classfile.IClassObserver #observeClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
     */
    public void observeClass( final ClassDescriptor clazz )
    {
        this.mavenLog.debug( "Observe class: " + clazz.getClassName() );

        ++this.fileCount;
    }

    /**
     * Report a queued error.
     * 
     * @param analysisError
     *            the queued error
     * @see edu.umd.cs.findbugs.AbstractBugReporter #reportAnalysisError(edu.umd.cs.findbugs.AnalysisError)
     */
    public void reportAnalysisError( final AnalysisError analysisError )
    {
        this.mavenLog.debug( "  Found an analysisError: " + analysisError.getMessage() );
        this.bugCollection.addError( analysisError.getMessage() );
        ++this.errorCount;
        super.reportAnalysisError( analysisError );
    }

    public void logError( String message )
    {
        this.mavenLog.debug( "  Found an analysisError: " + message );
        this.bugCollection.addError( message );
        ++this.errorCount;
        super.logError( message );
    }

    public void logError( String message, Throwable e )
    {
        this.mavenLog.debug( "  Found an analysisError: " + message );
        this.bugCollection.addError( message );
        ++this.errorCount;
        super.logError( message, e );
    }

    /**
     * Report a missing class.
     * 
     * @param missingClass
     *            the name of the class
     * @see edu.umd.cs.findbugs.AbstractBugReporter #reportMissingClass(java.lang.String)
     */
    public void reportMissingClass( final String missingClass )
    {
        this.mavenLog.debug( "Found a missing class: " + missingClass );
        if ( this.missingClassSet.add( missingClass ) )
        {
            ++this.missingClassCount;
        }
        super.reportMissingClass( missingClass );
    }

    public void reportMissingClass( ClassNotFoundException ex )
    {
        this.mavenLog.debug( "Found a missing class: " + ex.getMessage() );
        if ( this.missingClassSet.add( ex.getMessage() ) )
        {
            ++this.missingClassCount;
        }
        super.reportMissingClass( ex );
    }

    /* (non-Javadoc)
     * @see edu.umd.cs.findbugs.classfile.IErrorLogger#reportMissingClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
     */
    public void reportMissingClass( ClassDescriptor classDescriptor )
    {
        this.reportMissingClass( classDescriptor.toDottedClassName() );
    }

    /**
     * Initialises the report.
     */
    private void initialiseReport()
    {
        this.sink.head();
        this.sink.title();
        this.sink.text( this.getReportTitle() );
        this.sink.title_();
        this.sink.head_();

        this.sink.body();

        // the title of the report
        this.sink.section1();
        this.sink.sectionTitle1();
        this.sink.text( this.getReportTitle() );
        this.sink.sectionTitle1_();

        // information about FindBugs
        this.sink.paragraph();
        this.sink.text( this.getReportLinkTitle() + " " );
        this.sink.link( this.getFindBugsLink() );
        this.sink.text( this.getFindBugsName() );
        this.sink.link_();
        this.sink.paragraph_();

        this.sink.paragraph();
        this.sink.text( this.getVersionTitle() + " " );
        this.sink.italic();
        this.sink.text( this.getFindBugsVersion() );
        this.sink.italic_();
        this.sink.paragraph_();

        this.sink.paragraph();
        this.sink.text( this.getThresholdTitle() + " " );
        this.sink.italic();
        this.sink.text( this.threshold.getName() );
        this.sink.italic_();
        this.sink.paragraph_();

        this.sink.paragraph();
        this.sink.text( this.getEffortTitle() + " " );
        this.sink.italic();
        this.sink.text( this.effort.getName() );
        this.sink.italic_();
        this.sink.paragraph_();
        this.sink.section1_();

    }

    /**
     * Print the bug collection to a line in the table
     *  
     * @param bugInstance the bug to print
     */
    protected void printBug( final BugInstance bugInstance )
    {
        final SourceLineAnnotation line = bugInstance.getPrimarySourceLineAnnotation();
        final BugPattern pattern = bugInstance.getBugPattern();
        final String lineNumber = this.valueForLine( line );
        final String category = pattern.getCategory();
        final String type = pattern.getType();

        this.mavenLog.debug( "Bug line = " + line.getClassName() );
        this.mavenLog.debug( "Bug pattern = " + pattern.getShortDescription() );
        this.mavenLog.debug( "Bug line Number = " + lineNumber );
        this.mavenLog.debug( "Bug Category = " + category );
        this.mavenLog.debug( "Bug Type = " + type );
        this.mavenLog.debug( " " );

        if ( !this.mIsCurrentClassReportOpened )
        {
            this.openClassReportSection();
            this.mIsCurrentClassReportOpened = true;
        }

        this.sink.tableRow();

        // bug
        this.sink.tableCell();
        this.sink.text( bugInstance.getMessageWithoutPrefix() );
        this.sink.tableCell_();

        // category
        this.sink.tableCell();
        this.sink.text( category );
        this.sink.tableCell_();

        // description link
        this.sink.tableCell();
        this.sink.link( this.getDetailsLink( type ) );
        this.sink.text( type );
        this.sink.link_();
        this.sink.tableCell_();

        // line
        this.sink.tableCell();
        if ( this.isJXRReportEnabled )
        {
            this.sink.rawText( this.assembleJXRHyperlink( line, lineNumber ) );
        }
        else
        {
            this.sink.text( lineNumber );
        }

        this.sink.tableCell_();
        this.sink.tableRow_();
    }

    /**
     * Assembles the hyperlink to point to the source code.
     * 
     * @param pLine
     *            The line number object with the bug.
     * @param pLineNumber
     *            The line number to show in the hyperlink.
     * @return The hyperlink which points to the code.
     * 
     */
    protected String assembleJXRHyperlink( final SourceLineAnnotation pLine, final String pLineNumber )
    {
        String hyperlink = null;
        final String prefix = this.bundle.getString( Reporter.JXR_PATHPREFIX_KEY );
        final String path = prefix + Reporter.URL_SEPARATOR + this.currentClassName.replaceAll( "[.]", "/" );

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
     * Closes the class report section.
     */
    protected void closeClassReportSection()
    {
        this.mavenLog.debug( "Closing report Section" );
        this.sink.table_();
        this.sink.section2_();
    }

    /**
     * @param bugInstance The bug to report
     * @see edu.umd.cs.findbugs.AbstractBugReporter #doReportBug(edu.umd.cs.findbugs.BugInstance)
     */
    protected void doReportBug( final BugInstance bugInstance )
    {
        this.mavenLog.debug( "  Found a bug: " + bugInstance.getMessage() );
        if ( this.bugCollection.add( bugInstance ) )
        {
            ++this.bugCount;
            this.notifyObservers( bugInstance );
            this.notifyObservers( bugInstance );
        }

    }

    /**
     * Gets the link to details description on findbugs site.
     * 
     * @param pType
     *            the bug type
     * @return The report link.
     * 
     */
    protected String getDetailsLink( final String pType )
    {
        final String link = this.bundle.getString( Reporter.DETAILSLINK_KEY ) + "#" + pType;

        return link;
    }

    /**
     * Gets the effort title of the report.
     * 
     * @return The effort title of the report.
     * 
     */
    protected String getEffortTitle()
    {
        final String effortTitle = this.bundle.getString( Reporter.EFFORT_KEY );

        return effortTitle;
    }

    /**
     * Gets the title for the files title.
     * 
     * @return The name for the File Section.
     * 
     */
    protected String getFilesTitle()
    {
        final String fileTitle = this.bundle.getString( Reporter.FILES_KEY );

        return fileTitle;
    }

    /**
     * Gets the title for the summary section.
     * 
     * @return The name for the Summary Section.
     * 
     */
    protected String getSummaryTitle()
    {
        final String summaryTitle = this.bundle.getString( Reporter.SUMMARY_KEY );

        return summaryTitle;
    }

    /**
     * Gets the link to FindBugs.
     * 
     * @return The report link.
     * 
     */
    protected String getFindBugsLink()
    {
        final String link = this.bundle.getString( Reporter.LINK_KEY );

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
        final String name = this.bundle.getString( Reporter.NAME_KEY );

        return name;
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
     * Gets the name of the link to FindBugs.
     * 
     * @return The report link title.
     * 
     */
    protected String getReportLinkTitle()
    {
        final String reportLink = this.bundle.getString( Reporter.LINKTITLE_KEY );

        return reportLink;
    }

    /**
     * Gets the report title.
     * 
     * @return The report title.
     * 
     */
    protected String getReportTitle()
    {
        final String reportTitle = this.bundle.getString( Reporter.REPORT_TITLE_KEY );

        return reportTitle;
    }

    /**
     * Gets the threshold title of the report.
     * 
     * @return The threshold title of the report.
     * 
     */
    protected String getThresholdTitle()
    {
        final String threshholdTitle = this.bundle.getString( Reporter.THRESHOLD_KEY );

        return threshholdTitle;
    }

    /**
     * Gets the Findbugs Version title of the report.
     * 
     * @return The Findbugs Version used on the report.
     * 
     */
    protected String getVersionTitle()
    {
        final String versionTitle = this.bundle.getString( VERSIONTITLE_KEY );

        return versionTitle;
    }

    /**
     * Initialised a bug report section in the report for a particular class.
     */
    protected void openClassReportSection()
    {
        final String columnBugText = this.bundle.getString( Reporter.COLUMN_BUG_KEY );
        final String columnBugCategory = this.bundle.getString( Reporter.COLUMN_CATEGORY_KEY );
        final String columnDescriptionLink = this.bundle.getString( Reporter.COLUMN_DETAILS_KEY );
        final String columnLineText = this.bundle.getString( Reporter.COLUMN_LINE_KEY );

        this.mavenLog.debug( "Opening Class Report Section" );

        this.sink.anchor( this.currentClassName );
        this.sink.anchor_();

        this.sink.section2();
        this.sink.sectionTitle2();
        this.sink.text( this.currentClassName );
        this.sink.sectionTitle2_();
        this.sink.table();
        this.sink.tableRow();

        // bug
        this.sink.tableHeaderCell();
        this.sink.text( columnBugText );
        this.sink.tableHeaderCell_();

        // category
        this.sink.tableHeaderCell();
        this.sink.text( columnBugCategory );
        this.sink.tableHeaderCell_();

        // description link
        this.sink.tableHeaderCell();
        this.sink.text( columnDescriptionLink );
        this.sink.tableHeaderCell_();

        // line
        this.sink.tableHeaderCell();
        this.sink.text( columnLineText );
        this.sink.tableHeaderCell_();

        this.sink.tableRow_();
    }

    /**
     * Return the value to display. If FindBugs does not provide a line number, a default message is returned. The line
     * number otherwise.
     * 
     * @param pLine
     *            The line to get the value from.
     * @return The line number the bug appears or a statement that there is no source line available.
     * 
     */
    protected String valueForLine( final SourceLineAnnotation pLine )
    {
        String value = null;

        if ( pLine == null )
        {
            value = this.bundle.getString( Reporter.NOLINE_KEY );
        }
        else
        {
            final int startLine = pLine.getStartLine();
            final int endLine = pLine.getEndLine();

            if ( startLine == endLine )
            {
                if ( startLine == -1 )
                {
                    value = this.bundle.getString( Reporter.NOLINE_KEY );
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

    /**
     * Print the Summary Section.
     */
    protected void printSummary()
    {
        // the summary section
        this.sink.sectionTitle1();
        this.sink.text( this.getSummaryTitle() );
        this.sink.sectionTitle1_();

        this.sink.table();
        this.sink.tableRow();

        // classes
        this.sink.tableHeaderCell();
        this.sink.text( this.bundle.getString( Reporter.COLUMN_CLASSES_KEY ) );
        this.sink.tableHeaderCell_();

        // bugs
        this.sink.tableHeaderCell();
        this.sink.text( this.bundle.getString( Reporter.COLUMN_BUGS_KEY ) );
        this.sink.tableHeaderCell_();

        // Errors
        this.sink.tableHeaderCell();
        this.sink.text( this.bundle.getString( Reporter.COLUMN_ERRORS_KEY ) );
        this.sink.tableHeaderCell_();

        // Missing Classes
        this.sink.tableHeaderCell();
        this.sink.text( this.bundle.getString( Reporter.COLUMN_MISSINGCLASSES_KEY ) );
        this.sink.tableHeaderCell_();

        this.sink.tableRow_();

        this.sink.tableRow();

        // files
        this.sink.tableCell();
        this.sink.text( Integer.toString( this.fileCount ) );
        this.sink.tableCell_();

        // bug
        this.sink.tableCell();
        this.sink.text( Integer.toString( this.bugCount ) );
        this.sink.tableCell_();

        // Errors
        this.sink.tableCell();
        this.sink.text( Integer.toString( this.errorCount  ));
        this.sink.tableCell_();

        // Missing Classes
        this.sink.tableCell();
        this.sink.text( Integer.toString( this.missingClassCount ));
        this.sink.tableCell_();

        this.sink.tableRow_();
        this.sink.table_();

        this.sink.paragraph_();
        this.sink.section1_();
    }

    /**
     * Print the File Summary Section.
     */
    protected void printFilesSummary()
    {
        // the Files section
        this.sink.sectionTitle1();
        this.sink.text( this.getFilesTitle() );
        this.sink.sectionTitle1_();

        /**
         * Class Summary
         */

        int classBugs = 0;

        this.sink.table();
        this.sink.tableRow();

        // files
        this.sink.tableHeaderCell();
        this.sink.text( this.bundle.getString( Reporter.COLUMN_CLASS_KEY ) );
        this.sink.tableHeaderCell_();

        // bugs
        this.sink.tableHeaderCell();
        this.sink.text( this.bundle.getString( Reporter.COLUMN_BUGS_KEY ) );
        this.sink.tableHeaderCell_();

        this.sink.tableRow_();

        for ( Iterator i = this.bugCollection.iterator(); i.hasNext(); )
        {
            BugInstance bugInstance = ( BugInstance ) i.next();

            this.mavenLog.debug( "Annotation Class is " + bugInstance.getPrimarySourceLineAnnotation().getClassName() );
            this.mavenLog.debug( "Class is " + this.currentClassName );
            this.mavenLog.debug( " " );

            if ( bugInstance.getPrimarySourceLineAnnotation().getClassName().equals( this.currentClassName ) )
            {
                ++classBugs;
            }
            else
            {
                if ( this.currentClassName.length() > 0 )
                {
                    this.printFilesSummaryLine( classBugs );
                }

                classBugs = 1;
                this.currentClassName = bugInstance.getPrimarySourceLineAnnotation().getClassName();

            }
        }

        this.printFilesSummaryLine( classBugs );

        this.sink.table_();

    }

    protected void printFilesSummaryLine( int classBugs )
    {
        this.sink.tableRow();

        // class name
        this.sink.tableCell();
        this.sink.link( "#" + this.currentClassName );
        this.sink.text( this.currentClassName );
        this.sink.link_();
        this.sink.tableCell_();

        // class bug total count
        this.sink.tableCell();
        this.sink.text( Integer.toString( classBugs ));
        this.sink.tableCell_();

        this.sink.tableRow_();

    }
}
