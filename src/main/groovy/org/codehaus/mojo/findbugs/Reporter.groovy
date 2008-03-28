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


import org.apache.maven.doxia.sink.Sink
import org.apache.maven.plugin.logging.Log

import edu.umd.cs.findbugs.AnalysisError
import edu.umd.cs.findbugs.BugInstance
import edu.umd.cs.findbugs.BugPattern
import edu.umd.cs.findbugs.BugReporter
import edu.umd.cs.findbugs.SortedBugCollection
import edu.umd.cs.findbugs.SourceLineAnnotation
import edu.umd.cs.findbugs.TextUIBugReporter
import edu.umd.cs.findbugs.classfile.ClassDescriptor

/**
 * The reporter controls the generation of the FindBugs report. It contains call back methods which gets called by
 * FindBugs if a bug is found.
 * 
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id$
 */
class Reporter extends TextUIBugReporter
{

    /**
     * The key to get the value if the line number is not available.
     * 
     */
    static final String NOLINE_KEY = "report.findbugs.noline"

    /**
     * The key to get the column title for the line.
     * 
     */
    static final String COLUMN_LINE_KEY = "report.findbugs.column.line"

    /**
     * The key to get the column title for the bug.
     * 
     */
    static final String COLUMN_BUG_KEY = "report.findbugs.column.bug"

    /**
     * The key to get the column title for the bugs.
     * 
     */
    static final String COLUMN_BUGS_KEY = "report.findbugs.column.bugs"

    /**
     * The key to get the column title for the category.
     * 
     */
    static final String COLUMN_CATEGORY_KEY = "report.findbugs.column.category"

    /**
     * The key to get the column title for the details.
     * 
     */
    static final String COLUMN_DETAILS_KEY = "report.findbugs.column.details"

    /**
     * The key to get the report title of the Plug-In from the bundle.
     * 
     */
    static final String REPORT_TITLE_KEY = "report.findbugs.reporttitle"

    /**
     * The key to get the report link title of the Plug-In from the bundle.
     * 
     */
    static final String LINKTITLE_KEY = "report.findbugs.linktitle"

    /**
     * The key to get the report link of the Plug-In from the bundle.
     * 
     */
    static final String LINK_KEY = "report.findbugs.link"

    /**
     * The key to get the name of the Plug-In from the bundle.
     * 
     */
    static final String NAME_KEY = "report.findbugs.name"

    /**
     * The key to get the files title of the Plug-In from the bundle.
     * 
     */
    static final String FILES_KEY = "report.findbugs.files"

    /**
     * The key to get the threshold of the report from the bundle.
     * 
     */
    static final String THRESHOLD_KEY = "report.findbugs.threshold"

    /**
     * The character to separate URL tokens.
     * 
     */
    static final String URL_SEPARATOR = "/"

    /**
     * The key to get the jxr-plugin path prefix.
     * 
     */
    static final String JXR_PATHPREFIX_KEY = "report.findbugs.jxrplugin.pathprefix"

    /**
     * The key to get the effort of the report from the bundle.
     * 
     */
    static final String EFFORT_KEY = "report.findbugs.effort"

    /**
     * The key to get the link to FindBugs description page from the bundle.
     * 
     */
    static final String DETAILSLINK_KEY = "report.findbugs.detailslink"

    /**
     * The key to get the version title for FindBugs from the bundle.
     * 
     */
    static final String VERSIONTITLE_KEY = "report.findbugs.versiontitle"

    /**
     * The key to get the files title of the Plug-In from the bundle.
     * 
     */
    static final String SUMMARY_KEY = "report.findbugs.summary"

    /**
     * The key to column title for the Class.
     * 
     */
    static final String COLUMN_CLASS_KEY = "report.findbugs.column.class"

    /**
     * The key to column title for the Classes.
     * 
     */
    static final String COLUMN_CLASSES_KEY = "report.findbugs.column.classes"

    /**
     * The key to column title for the errors.
     * 
     */
    static final String COLUMN_ERRORS_KEY = "report.findbugs.column.errors"

    /**
     * The key to column title for the files.
     * 
     */
    static final String COLUMN_FILES_KEY = "report.findbugs.column.files"

    /**
     * The key to column title for the files.
     * 
     */
    static final String COLUMN_MISSINGCLASSES_KEY = "report.findbugs.column.missingclasses"

    /**
     * The sink to write the report to.
     * 
     */
    Sink sink

    /**
     * The bundle to get the messages from.
     * 
     */
    ResourceBundle bundle

    /**
     * The logger to write logs to.
     * 
     */
    Log mavenLog

    
    /**
     * The threshold of bugs severity.
     * 
     */
    ThresholdParameter threshold

    /**
     * The used effort for searching bugs.
     * 
     */
    EffortParameter effort

    /**
     * The name of the current class which is analysed by FindBugs.
     * 
     */
    String currentClassName

    /**
     * Signals if the report for the current class is opened.
     * 
     */
    boolean mIsCurrentClassReportOpened = false

    /**
     * Signals if the jxr report plugin is enabled.
     * 
     */
    boolean isJXRReportEnabled = false

    /**
     * The Collection of Bugs and Error collected during analysis.
     * 
     */
    SortedBugCollection bugCollection = new SortedBugCollection()

    /**
     * The running total of bugs reported.
     * 
     */
    int bugCount

    /**
     * The running total of missing classes reported.
     * 
     */
    int missingClassCount

    /**
     * The running total of files analyzed.
     * 
     */
    int fileCount

    /**
     * The Set of missing classes names reported.
     * 
     */
    Set missingClassSet = new HashSet()

    /**
     * The running total of errors reported.
     * 
     */
    int errorCount

    /**
     * Default constructor.
     * 
     * @param sink
     *            The sink to generate the report.
     * @param bundle
     *            The resource bundle to get the messages from.
     * @param log
     *            The logger.
     * @param threshold
     *            The threshold for the report.
     * @param isJXRReportEnabled
     *            Is the jxr report plugin enabled.
     * @param effort
     *            The used effort.
     */
     Reporter( Sink sink, ResourceBundle bundle, Log log,
                     ThresholdParameter threshold, boolean isJXRReportEnabled,
                     EffortParameter effort )
    {
        super()

        assert sink
        assert bundle
        assert log
        assert threshold
        assert effort

        this.sink = sink
        this.bundle = bundle
        this.mavenLog = log
        this.threshold = threshold
        this.isJXRReportEnabled = isJXRReportEnabled
        this.effort = effort
        this.currentClassName = ""

        this.bugCount = 0
        this.missingClassCount = 0
        this.errorCount = 0
        this.fileCount = 0

        this.initialiseReport()
    }

    /**
     * Hide default constructor.
     * 
     */
    private Reporter()
    {
        super()

        this.sink = null
        this.bundle = null
        this.mavenLog = null
        this.threshold = null
        this.effort = null
    }

    /**
     * @see edu.umd.cs.findbugs.BugReporter#finish()
     */
    void finish()
    {
        this.mavenLog.debug( "Finished searching for bugs!" )

        this.printSummary()

        this.printFilesSummary()

        this.mIsCurrentClassReportOpened = false

        this.bugCollection.each() {bugInstance ->

            this.mavenLog.debug( "Annotation Class is " + bugInstance.getPrimarySourceLineAnnotation().getClassName() )
            this.mavenLog.debug( "Class is " + this.currentClassName )
            this.mavenLog.debug( " " )

            if ( !bugInstance.getPrimarySourceLineAnnotation().getClassName().equals( this.currentClassName ) )
            {

                this.currentClassName = bugInstance.getPrimarySourceLineAnnotation().getClassName()

                if ( this.mIsCurrentClassReportOpened )
                {
                    this.closeClassReportSection()
                    this.mIsCurrentClassReportOpened = false
                }
            }

            this.printBug( bugInstance )
        }

        // close the last class report section
        if ( this.mIsCurrentClassReportOpened )
        {
            this.closeClassReportSection()
        }

        // close the report, write it
        this.sink.body_()
        this.sink.flush()
        this.sink.close()

        this.mavenLog.debug( "bugCount = " + this.bugCount )
        this.mavenLog.debug( "errorCount = " + this.errorCount )
        this.mavenLog.debug( "missingClassCount = " + this.missingClassCount )
    }

    /**
     * Get the real bug reporter at the end of a chain of delegating bug reporters. All non-delegating bug reporters
     * should simply "return this".
     * 
     * @return the real bug reporter at the end of the chain, or this object if there is no delegation
     * @see edu.umd.cs.findbugs.BugReporter#getRealBugReporter()
     */
    BugReporter getRealBugReporter()
    {
        return this
    }

    /**
     * Observe a class.
     * 
     * @param clazz
     *            the class
     * @see edu.umd.cs.findbugs.classfile.IClassObserver #observeClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
     */
    void observeClass( ClassDescriptor clazz )
    {
        this.mavenLog.debug( "Observe class: " + clazz.getClassName() )

        ++this.fileCount
    }

    /**
     * Report a queued error.
     * 
     * @param analysisError
     *            the queued error
     * @see edu.umd.cs.findbugs.AbstractBugReporter #reportAnalysisError(edu.umd.cs.findbugs.AnalysisError)
     */
    void reportAnalysisError( AnalysisError analysisError )
    {
        this.mavenLog.debug( "  Found an analysisError: " + analysisError.getMessage() )
        this.bugCollection.addError( analysisError.getMessage() )
        ++this.errorCount
        super.reportAnalysisError( analysisError )
    }

    void logError( String message )
    {
        this.mavenLog.debug( "  Found an analysisError: " + message )
        this.bugCollection.addError( message )
        ++this.errorCount
        super.logError( message )
    }

    void logError( String message, Throwable e )
    {
        this.mavenLog.debug( "  Found an analysisError: " + message )
        this.bugCollection.addError( message )
        ++this.errorCount
        super.logError( message, e )
    }

    /**
     * Report a missing class.
     * 
     * @param missingClass
     *            the name of the class
     * @see edu.umd.cs.findbugs.AbstractBugReporter #reportMissingClass(java.lang.String)
     */
    void reportMissingClass( String missingClass )
    {
        this.mavenLog.debug( "Found a missing class: " + missingClass )
        if ( this.missingClassSet.add( missingClass ) )
        {
            ++this.missingClassCount
        }
        super.reportMissingClass( missingClass )
    }

    void reportMissingClass( ClassNotFoundException ex )
    {
        this.mavenLog.debug( "Found a missing class: " + ex.getMessage() )
        if ( this.missingClassSet.add( ex.getMessage() ) )
        {
            ++this.missingClassCount
        }
        super.reportMissingClass( ex )
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.umd.cs.findbugs.classfile.IErrorLogger#reportMissingClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
     */
    void reportMissingClass( ClassDescriptor classDescriptor )
    {
        this.reportMissingClass( classDescriptor.toDottedClassName() )
    }

    /**
     * Initialises the report.
     */
    private void initialiseReport()
    {
        this.sink.head()
        this.sink.title()
        this.sink.text( this.getReportTitle() )
        this.sink.title_()
        this.sink.head_()

        this.sink.body()

        // the title of the report
        this.sink.section1()
        this.sink.sectionTitle1()
        this.sink.text( this.getReportTitle() )
        this.sink.sectionTitle1_()

        // information about FindBugs
        this.sink.paragraph()
        this.sink.text( this.bundle.getString( Reporter.LINKTITLE_KEY ) + " " )
        this.sink.link( this.bundle.getString( Reporter.LINK_KEY ) )
        this.sink.text( this.bundle.getString( Reporter.NAME_KEY ) )
        this.sink.link_()
        this.sink.paragraph_()

        this.sink.paragraph()
        this.sink.text( this.bundle.getString( VERSIONTITLE_KEY ) + " " )
        this.sink.italic()
        this.sink.text( edu.umd.cs.findbugs.Version.RELEASE )
        this.sink.italic_()
        this.sink.paragraph_()

        this.sink.paragraph()
        this.sink.text( this.bundle.getString( Reporter.THRESHOLD_KEY ) + " " )
        this.sink.italic()
        this.sink.text( this.threshold.getName() )
        this.sink.italic_()
        this.sink.paragraph_()

        this.sink.paragraph()
        this.sink.text( this.bundle.getString( Reporter.EFFORT_KEY ) + " " )
        this.sink.italic()
        this.sink.text( this.effort.getName() )
        this.sink.italic_()
        this.sink.paragraph_()
        this.sink.section1_()

    }

    /**
     * Print the bug collection to a line in the table
     * 
     * @param bugInstance
     *            the bug to print
     */
    protected void printBug( BugInstance bugInstance )
    {
        SourceLineAnnotation line = bugInstance.getPrimarySourceLineAnnotation()
        BugPattern pattern = bugInstance.getBugPattern()
        String lineNumber = this.valueForLine( line )
        String category = pattern.getCategory()
        String type = pattern.getType()

        this.mavenLog.debug( "Bug line = " + line.getClassName() )
        this.mavenLog.debug( "Bug pattern = " + pattern.getShortDescription() )
        this.mavenLog.debug( "Bug line Number = " + lineNumber )
        this.mavenLog.debug( "Bug Category = " + category )
        this.mavenLog.debug( "Bug Type = " + type )
        this.mavenLog.debug( " " )

        if ( !this.mIsCurrentClassReportOpened )
        {
            this.openClassReportSection()
            this.mIsCurrentClassReportOpened = true
        }

        this.sink.tableRow()

        // bug
        this.sink.tableCell()
        this.sink.text( bugInstance.getMessageWithoutPrefix() )
        this.sink.tableCell_()

        // category
        this.sink.tableCell()
        this.sink.text( category )
        this.sink.tableCell_()

        // description link
        this.sink.tableCell()
        this.sink.link( this.bundle.getString( Reporter.DETAILSLINK_KEY ) + "#" + type )
        this.sink.text( type )
        this.sink.link_()
        this.sink.tableCell_()

        // line
        this.sink.tableCell()
        if ( this.isJXRReportEnabled )
        {
            this.sink.rawText( assembleJXRHyperlink( line, lineNumber ) )
        }
        else
        {
            this.sink.text( lineNumber )
        }

        this.sink.tableCell_()
        this.sink.tableRow_()
    }

    /**
     * Assembles the hyperlink to point to the source code.
     * 
     * @param line
     *            The line number object with the bug.
     * @param lineNumber
     *            The line number to show in the hyperlink.
     * @return The hyperlink which points to the code.
     * 
     */
    protected String assembleJXRHyperlink( SourceLineAnnotation line, String lineNumber )
    {
        String hyperlink = null
        String prefix = this.bundle.getString( Reporter.JXR_PATHPREFIX_KEY )
        String path =
            prefix + Reporter.URL_SEPARATOR + this.currentClassName.replaceAll( "[.]", "/" ).replaceAll( "[\$].*", "" )

        if ( line == null )
        {
            hyperlink = "<a href=\"" + path + ".html\">" + lineNumber + "</a>"
        }
        else
        {
            hyperlink = "<a href=\"" + path + ".html#" + line.getStartLine() + "\">" + lineNumber + "</a>"
        }

        return hyperlink
    }

    /**
     * Closes the class report section.
     */
    protected void closeClassReportSection()
    {
        this.mavenLog.debug( "Closing report Section" )
        this.sink.table_()
        this.sink.section2_()
    }

    /**
     * @param bugInstance
     *            The bug to report
     * @see edu.umd.cs.findbugs.AbstractBugReporter #doReportBug(edu.umd.cs.findbugs.BugInstance)
     */
    protected void doReportBug( BugInstance bugInstance )
    {
        this.mavenLog.debug( "  Found a bug: " + bugInstance.getMessage() )
        if ( this.bugCollection.add( bugInstance ) )
        {
            ++this.bugCount
            this.notifyObservers( bugInstance )
        }

    }

    /**
     * Gets the report title.
     * 
     * @return The report title.
     * 
     */
    protected String getReportTitle()
    {
        return this.bundle.getString( Reporter.REPORT_TITLE_KEY )
    }

    /**
     * Initialised a bug report section in the report for a particular class.
     */
    protected void openClassReportSection()
    {
        String columnBugText = this.bundle.getString( Reporter.COLUMN_BUG_KEY )
        String columnBugCategory = this.bundle.getString( Reporter.COLUMN_CATEGORY_KEY )
        String columnDescriptionLink = this.bundle.getString( Reporter.COLUMN_DETAILS_KEY )
        String columnLineText = this.bundle.getString( Reporter.COLUMN_LINE_KEY )

        this.mavenLog.debug( "Opening Class Report Section" )

        this.sink.anchor( this.currentClassName )
        this.sink.anchor_()

        this.sink.section2()
        this.sink.sectionTitle2()
        this.sink.text( this.currentClassName )
        this.sink.sectionTitle2_()
        this.sink.table()
        this.sink.tableRow()

        // bug
        this.sink.tableHeaderCell()
        this.sink.text( columnBugText )
        this.sink.tableHeaderCell_()

        // category
        this.sink.tableHeaderCell()
        this.sink.text( columnBugCategory )
        this.sink.tableHeaderCell_()

        // description link
        this.sink.tableHeaderCell()
        this.sink.text( columnDescriptionLink )
        this.sink.tableHeaderCell_()

        // line
        this.sink.tableHeaderCell()
        this.sink.text( columnLineText )
        this.sink.tableHeaderCell_()

        this.sink.tableRow_()
    }

    /**
     * Return the value to display. If FindBugs does not provide a line number, a default message is returned. The line
     * number otherwise.
     * 
     * @param line
     *            The line to get the value from.
     * @return The line number the bug appears or a statement that there is no source line available.
     * 
     */
    protected String valueForLine( SourceLineAnnotation line )
    {
        String value

        if ( line )
        {
            int startLine = line.getStartLine()
            int endLine = line.getEndLine()

            if ( startLine == endLine )
            {
                if ( startLine == -1 )
                {
                    value = this.bundle.getString( Reporter.NOLINE_KEY )
                }
                else
                {
                    value = startLine.toString()
                }
            }
            else
            {
                value = startLine.toString() + "-" + endLine.toString()
            }
        }
        else
        {
            value = this.bundle.getString( Reporter.NOLINE_KEY )
        }

        return value
    }

    /**
     * Print the Summary Section.
     */
    protected void printSummary()
    {
        this.sink.section1()
        
        // the summary section
        this.sink.sectionTitle1()
        this.sink.text( this.bundle.getString( Reporter.SUMMARY_KEY ) )
        this.sink.sectionTitle1_()

        this.sink.table()
        this.sink.tableRow()

        // classes
        this.sink.tableHeaderCell()
        this.sink.text( this.bundle.getString( Reporter.COLUMN_CLASSES_KEY ) )
        this.sink.tableHeaderCell_()

        // bugs
        this.sink.tableHeaderCell()
        this.sink.text( this.bundle.getString( Reporter.COLUMN_BUGS_KEY ) )
        this.sink.tableHeaderCell_()

        // Errors
        this.sink.tableHeaderCell()
        this.sink.text( this.bundle.getString( Reporter.COLUMN_ERRORS_KEY ) )
        this.sink.tableHeaderCell_()

        // Missing Classes
        this.sink.tableHeaderCell()
        this.sink.text( this.bundle.getString( Reporter.COLUMN_MISSINGCLASSES_KEY ) )
        this.sink.tableHeaderCell_()

        this.sink.tableRow_()

        this.sink.tableRow()

        // files
        this.sink.tableCell()
        this.sink.text( this.fileCount.toString() )
        this.sink.tableCell_()

        // bug
        this.sink.tableCell()
        this.sink.text( this.bugCount.toString() )
        this.sink.tableCell_()

        // Errors
        this.sink.tableCell()
        this.sink.text( this.errorCount.toString() )
        this.sink.tableCell_()

        // Missing Classes
        this.sink.tableCell()
        this.sink.text( this.missingClassCount.toString() )
        this.sink.tableCell_()

        this.sink.tableRow_()
        this.sink.table_()

        this.sink.section1_()
    }

    /**
     * Print the File Summary Section.
     */
    protected void printFilesSummary()
    {
        this.sink.section1()
        
        // the Files section
        this.sink.sectionTitle1()
        this.sink.text( this.bundle.getString( Reporter.FILES_KEY ) )
        this.sink.sectionTitle1_()

        /**
         * Class Summary
         */

        int classBugs = 0

        this.sink.table()
        this.sink.tableRow()

        // files
        this.sink.tableHeaderCell()
        this.sink.text( this.bundle.getString( Reporter.COLUMN_CLASS_KEY ) )
        this.sink.tableHeaderCell_()

        // bugs
        this.sink.tableHeaderCell()
        this.sink.text( this.bundle.getString( Reporter.COLUMN_BUGS_KEY ) )
        this.sink.tableHeaderCell_()

        this.sink.tableRow_()

        this.bugCollection.each() { bugInstance ->

        this.mavenLog.debug( "Annotation Class is " + bugInstance.getPrimarySourceLineAnnotation().getClassName() )
            this.mavenLog.debug( "Class is " + this.currentClassName )
            this.mavenLog.debug( " " )

            if ( bugInstance.getPrimarySourceLineAnnotation().getClassName().equals( this.currentClassName ) )
            {
                ++classBugs
            }
            else
            {
                if ( this.currentClassName.length() > 0 )
                {
                    this.printFilesSummaryLine( classBugs )
                }

                classBugs = 1
                this.currentClassName = bugInstance.getPrimarySourceLineAnnotation().getClassName()
            }
        }

        this.printFilesSummaryLine( classBugs )

        this.sink.table_()
        
        this.sink.section1_()
    }

    protected void printFilesSummaryLine( int classBugs )
    {
        this.sink.tableRow()

        // class name
        this.sink.tableCell()
        this.sink.link( "#" + this.currentClassName )
        this.sink.text( this.currentClassName )
        this.sink.link_()
        this.sink.tableCell_()

        // class bug total count
        this.sink.tableCell()
        this.sink.text( classBugs.toString() )
        this.sink.tableCell_()

        this.sink.tableRow_()
    }
}
