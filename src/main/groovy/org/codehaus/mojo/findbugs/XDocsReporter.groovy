package org.codehaus.mojo.findbugs

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") you may not use this file except in compliance
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


import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject

import edu.umd.cs.findbugs.AbstractBugReporter
import edu.umd.cs.findbugs.BugInstance
import edu.umd.cs.findbugs.BugPattern
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
class XDocsReporter extends TextUIBugReporter
{

    /**
     * The key to get the value if the line number is not available.
     *
     */
    static final String NOLINE_KEY = "report.findbugs.noline"

    /**
     * The sink to write the report to.
     *
     */
    FindbugsXdocSink sink

    /**
     * The bundle to get the messages from.
     *
     */
    ResourceBundle resourceBundle

    /**
     * The logger to write logs to.
     *
     */
    Log log

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
    boolean isCurrentClassReportOpened = false

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
     * The Collection of Bugs and Error collected during analysis.
     *
     */
    SortedBugCollection bugCollection = new SortedBugCollection()

    /**
     * The output Writer stream.
     *
     */
    Writer outputWriter

    /**
     * The MavenProject Object
     *
     */
    MavenProject mavenProject


    /**
     * Default constructor.
     *
     * @param realBugReporter
     *            the BugReporter to Delegate
     */
    XDocsReporter(MavenProject mavenProject)
    {
        super()

        this.mavenProject = mavenProject;
        this.sink = null
        this.resourceBundle = null
        this.log = null
        this.threshold = null
        this.effort = null
    }

    /**
     * @see edu.umd.cs.findbugs.BugReporter#finish()
     */
    void finish()
    {

        this.isCurrentClassReportOpened = false

        this.bugCollection.each() {bugInstance ->

            this.log.debug("Annotation Class is " + bugInstance.getPrimarySourceLineAnnotation().getClassName())
            this.log.debug("Class is " + this.currentClassName)
            this.log.debug(" ")

            if ( !bugInstance.getPrimarySourceLineAnnotation().getClassName().equals(this.currentClassName) )
            {

                this.currentClassName = bugInstance.getPrimarySourceLineAnnotation().getClassName()

                if ( this.isCurrentClassReportOpened )
                {
                    this.closeClassReportSection()
                    this.isCurrentClassReportOpened = false
                }
            }

            this.printBug(bugInstance)
        }

        // close file tag if needed
        if ( this.isCurrentClassReportOpened )
        {
            this.closeClassReportSection()
        }

        this.isCurrentClassReportOpened = false

        // close the report, write it
        this.printErrors()
        this.printSource()
        this.getSink().body_()
        this.getSink().flush()
        this.getSink().close()

    }

    /**
     * @return the sink
     */
    FindbugsXdocSink getSink()
    {
        if ( !this.sink )
        {
            this.sink = new FindbugsXdocSink(this.getOutputWriter())

            // Initialises the report.
            this.getSink().head()
            this.getSink().head_()

            this.getSink().body(this.getFindBugsVersion(), this.threshold.getName(), this.effort.getName())

        }
        return this.sink
    }

    void logError(String message)
    {
        this.bugCollection.addError(message)
        super.logError(message)
    }

    void logError(String message, Throwable e)
    {
        this.bugCollection.addError(message)
        super.logError(message, e)
    }

    /**
     * Observe a class.
     *
     * @param classDescriptor
     *            The Class to Observe
     * @see edu.umd.cs.findbugs.classfile.IClassObserver #observeClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
     */
    void observeClass(ClassDescriptor classDescriptor)
    {
        ++this.fileCount
        this.currentClassName = classDescriptor.toDottedClassName()

        if ( this.isCurrentClassReportOpened )
        {
            this.closeClassReportSection()
        }

        this.isCurrentClassReportOpened = false
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.umd.cs.findbugs.classfile.IErrorLogger#reportMissingClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
     */

    void reportMissingClass(ClassDescriptor classDescriptor)
    {
        ++this.missingClassCount

        this.bugCollection.addMissingClass(classDescriptor.toDottedClassName())
        super.reportMissingClass(classDescriptor)
    }

    void reportMissingClass(ClassNotFoundException ex)
    {
        ++this.missingClassCount

        String missingClassName = AbstractBugReporter.getMissingClassName(ex)
        this.bugCollection.addMissingClass(missingClassName)
        super.reportMissingClass(ex)
    }

    /**
     * Print the bug collection to a line in the table
     *
     * @param bugInstance
     *            the bug to print
     */
    protected void printBug(BugInstance bugInstance)
    {
        SourceLineAnnotation line = bugInstance.getPrimarySourceLineAnnotation()
        BugPattern pattern = bugInstance.getBugPattern()
        String lineNumber = this.valueForLine(line)
        String category = pattern.getCategory()
        String type = pattern.getType()
        String priority = this.evaluateThresholdParameter(bugInstance.getPriority())
        String message = bugInstance.getMessage()

        if ( !this.isCurrentClassReportOpened )
        {
            this.getSink().classTag(this.currentClassName)
            this.isCurrentClassReportOpened = true
        }

        this.log.debug("  Found a bug: " + bugInstance.getMessage() + bugInstance.getMessageWithPriorityType())
        this.getSink().bugInstance(type, priority, category, message, lineNumber)
    }

    /**
     * Closes the class report section.
     */
    protected void closeClassReportSection()
    {
        this.getSink().classTag_()
    }

    /**
     * Returns the threshold string value for the integer input.
     *
     * @param thresholdValue
     *            The ThresholdValue integer to evaluate.
     * @return The string valueof the Threshold object.
     *
     */
    protected String evaluateThresholdParameter(int thresholdValue)
    {
        String thresholdName

        switch ( thresholdValue )
        {
            case 1:
                thresholdName = ThresholdParameter.HIGH.getName()
                break
            case 2:
                thresholdName = ThresholdParameter.NORMAL.getName()
                break
            case 3:
                thresholdName = ThresholdParameter.LOW.getName()
                break
            case 4:
                thresholdName = ThresholdParameter.EXP.getName()
                break
            case 5:
                thresholdName = ThresholdParameter.IGNORE.getName()
                break
            default:
                thresholdName = "Invalid Priority"
        }

        return thresholdName

    }

    /**
     * Gets the Findbugs Version of the report.
     *
     * @return The Findbugs Version used on the report.
     *
     */
    protected String getFindBugsVersion()
    {
        return edu.umd.cs.findbugs.Version.RELEASE
    }

    /**
     * Closes the class report section.
     */
    protected void printErrors()
    {
        this.log.info("Printing Errors")
        this.getSink().errorTag()

        this.bugCollection.errorIterator().each() {analysisError ->
            this.getSink().analysisErrorTag(analysisError.getMessage())
        }

        this.log.info("Printing Missing classes")

        this.bugCollection.missingClassIterator().each() {missingClass ->
            this.getSink().missingClassTag(missingClass)
        }
        this.getSink().errorTag_()
    }

    /**
     * Output Source Directories.
     */
    protected void printSource()
    {
        this.log.info("Printing Source Roots")
        this.getSink().ProjectTag()

        List srcDirs = mavenProject.getCompileSourceRoots()
        if ( !srcDirs.isEmpty() )
        {
            srcDirs.each() {srcDir ->
                this.getSink().srcDirTag(srcDir)
            }
        }

        this.getSink().ProjectTag_()
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
    protected String valueForLine(SourceLineAnnotation line)
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
                    value = this.resourceBundle.getString(XDocsReporter.NOLINE_KEY)
                } else
                {
                    value = startLine.toString()
                }
            } else
            {
                value = startLine.toString() + "-" + endLine.toString()
            }
        } else
        {
            value = this.resourceBundle.getString(XDocsReporter.NOLINE_KEY)
        }


        return value
    }

    /**
     * @param bugInstance
     *            The bug to report
     * @see edu.umd.cs.findbugs.AbstractBugReporter #doReportBug(edu.umd.cs.findbugs.BugInstance)
     */
    protected void doReportBug(BugInstance bugInstance)
    {
        this.log.debug("  Found a bug: " + bugInstance.getMessage())

        if ( this.bugCollection.add(bugInstance) )
        {
            ++this.bugCount
            this.notifyObservers(bugInstance)
        }
    }
}
     