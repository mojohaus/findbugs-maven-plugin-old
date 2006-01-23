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
 * @version $Revision: 34 $ $Date$
 * 
 * @author <a href="mailto:ruettimac@mac.com">Cyrill Ruettimann</a>
 * 
 * $Revision: 34 $
 * $Date$
 * $Author: cyrill $
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
     * The logger to write logs to.
     * 
     */
    private final transient ThresholdParameter mThreshold;

    /**
     * The name of the current class which is analysed by FindBugs.
     * 
     */
    private transient String currentClassName;

    /**
     * Signals if the report for the current class is opened.
     * 
     */
    private transient boolean isCurrentClassReportOpened = false;

    /**
     * Hide default constructor.
     * 
     */
    private Reporter()
    {
        super();

        mSink = null;
        mBundle = null;
        mLog = null;
        mThreshold = null;
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
     *            @param pThreshold The threshold for the report.
     */
    public Reporter( final Sink pSink, final ResourceBundle pBundle, final Log pLog, ThresholdParameter pThreshold )
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
        mSink = pSink;
        mBundle = pBundle;
        mLog = pLog;
        mThreshold = pThreshold;

        initialiseReport();
    }

    /**
     * Initialises the report.
     */
    private void initialiseReport()
    {
        mSink.head();
        mSink.title();
        mSink.text( getReportTitle() );
        mSink.title_();
        mSink.head_();

        mSink.body();

        // the title of the report
        mSink.section1();
        mSink.sectionTitle1();
        mSink.text( getReportTitle() );
        mSink.sectionTitle1_();

        // information about FindBugs
        mSink.paragraph();
        mSink.text( getReportLinkTitle() + " " );
        mSink.link( getFindBugsLink() );
        mSink.text( getFindBugsName() );
        mSink.link_();
        mSink.paragraph_();

        mSink.paragraph();
        mSink.text( getThresholdTitle() + " " );
        mSink.italic();
        mSink.text( mThreshold.getName() );
        mSink.italic_();
        mSink.paragraph_();

        // the files section
        mSink.section1_();
        mSink.sectionTitle1();
        mSink.text( getFilesTitle() );
        mSink.sectionTitle1_();
    }

    /**
     * @see edu.umd.cs.findbugs.AbstractBugReporter
     *      #doReportBug(edu.umd.cs.findbugs.BugInstance)
     */
    protected void doReportBug( final BugInstance pBugInstance )
    {
        mLog.debug( "  Found a bug: " + pBugInstance.getMessage() );

        addBugReport( pBugInstance );
    }

    /**
     * @see edu.umd.cs.findbugs.AbstractBugReporter
     *      #reportAnalysisError(edu.umd.cs.findbugs.AnalysisError)
     */
    public void reportAnalysisError( final AnalysisError pAnalysisError )
    {
        mLog.debug( "  Found an analysisError: " + pAnalysisError.getMessage() );
    }

    /**
     * @see edu.umd.cs.findbugs.AbstractBugReporter
     *      #reportMissingClass(java.lang.String)
     */
    public void reportMissingClass( final String pMissingClass )
    {
        mLog.debug( "  Found a missing class: " + pMissingClass );
    }

    /**
     * @see edu.umd.cs.findbugs.BugReporter#finish()
     */
    public void finish()
    {
        mLog.debug( "Finished searching for bugs!" );

        // close the last class report section
        if ( isCurrentClassReportOpened )
        {
            closeClassReportSection();
        }

        // close the report, write it
        mSink.section1_();
        mSink.body_();
        mSink.flush();
        mSink.close();
    }

    /**
     * @see edu.umd.cs.findbugs.BugReporter#getRealBugReporter()
     */
    public BugReporter getRealBugReporter()
    {
        return this;
    }

    /**
     * @see edu.umd.cs.findbugs.ba.ClassObserver
     *      #observeClass(org.apache.bcel.classfile.JavaClass)
     */
    public void observeClass( final JavaClass clazz )
    {
        mLog.debug( "Observe class: " + clazz.getClassName() );

        currentClassName = clazz.getClassName();

        if ( isCurrentClassReportOpened )
        {
            closeClassReportSection();
        }

        isCurrentClassReportOpened = false;
    }

    /**
     * Closes the class report section.
     */
    protected void closeClassReportSection()
    {
        mSink.table_();
        mSink.section2_();
    }

    /**
     * Gets the report title.
     * 
     * @return The report title.
     * 
     */
    protected String getReportTitle()
    {
        final String reportTitle = mBundle.getString( REPORT_TITLE_KEY );

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
        final String reportLink = mBundle.getString( LINKTITLE_KEY );

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
        final String link = mBundle.getString( LINK_KEY );

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
        final String name = mBundle.getString( NAME_KEY );

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
        final String fileTitle = mBundle.getString( FILES_KEY );

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
        final String threshholdTitle = mBundle.getString( THRESHOLD_KEY );

        return threshholdTitle;
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
        final String lineNumber = valueForLine( line );
        final String category = pattern.getCategory();

        if ( !isCurrentClassReportOpened )
        {
            openClassReportSection();
            isCurrentClassReportOpened = true;
        }

        mSink.tableRow();

        // bug
        mSink.tableCell();
        mSink.text( pBugInstance.getMessage() );
        mSink.tableCell_();

        // category
        mSink.tableCell();
        mSink.text( category );
        mSink.tableCell_();

        // line
        mSink.tableCell();
        mSink.text( lineNumber );
        mSink.tableCell_();

        mSink.tableRow_();
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
            value = mBundle.getString( NOLINE_KEY );
        }
        else
        {
            final int startLine = pLine.getStartLine();
            final int endLine = pLine.getEndLine();

            value = String.valueOf( startLine ) + "-" + String.valueOf( endLine );
        }

        return value;
    }

    /**
     * Initialised a bug report section in the report for a particular class.
     */
    protected void openClassReportSection()
    {
        final String columnBugText = mBundle.getString( COLUMN_BUG_KEY );
        final String columnBugCategory = mBundle.getString( COLUMN_CATEGORY_KEY );
        final String columnLineText = mBundle.getString( COLUMN_LINE_KEY );

        mSink.section2();
        mSink.sectionTitle2();
        mSink.text( currentClassName );
        mSink.sectionTitle2_();
        mSink.table();
        mSink.tableRow();

        // bug
        mSink.tableHeaderCell();
        mSink.text( columnBugText );
        mSink.tableHeaderCell_();

        // category
        mSink.tableHeaderCell();
        mSink.text( columnBugCategory );
        mSink.tableHeaderCell_();

        // line
        mSink.tableHeaderCell();
        mSink.text( columnLineText );
        mSink.tableHeaderCell_();

        mSink.tableRow_();
    }
}
