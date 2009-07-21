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

import groovy.util.slurpersupport.GPathResult

import org.apache.maven.doxia.tools.SiteTool


/**
 * The reporter controls the generation of the FindBugs report. It contains call back methods which gets called by
 * FindBugs if a bug is found.
 *
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id$
 */
class XDocsReporter implements FindBugsInfo
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
    ResourceBundle bundle

    /**
     * The logger to write logs to.
     *
     */
    Log log

    /**
     * The threshold of bugs severity.
     *
     */
    String threshold

    /**
     * The used effort for searching bugs.
     *
     */
    String effort

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
     * The output Writer stream.
     *
     */
    Writer outputWriter

    /**
     * "org.apache.maven.doxia.tools.SiteTool"
     *
     */
    SiteTool siteTool

    File basedir

    GPathResult findbugsResults


    List bugClasses

    /**
     * The directories containing the sources to be compiled.
     *
     */
    List compileSourceRoots


    /**
     * Default constructor.
     *
     * @param realBugReporter
     *            the BugReporter to Delegate
     */
    XDocsReporter( bundle, basedir , siteTool )
    {
        assert bundle
        assert basedir
        assert siteTool

        this.bundle = bundle
        this.basedir = basedir
        this.siteTool = siteTool

        this.bugClasses = []

        this.sink = null
        this.bundle = null
        this.log = null
        this.threshold = null
        this.effort = null
    }

    /**
     * @return the sink
     */
    FindbugsXdocSink getSink()
    {
        if ( !this.sink )
        {
            this.sink = new FindbugsXdocSink(getOutputWriter())

            // Initialises the report.
        }
        return this.sink
    }


    /**
     * Print the bug collection to a line in the table
     *
     * @param bugInstance
     *            the bug to print
     */
    protected void printBug( String bugClass )
    {

        getSink().classTag(bugClass)

        log.info("printBug bugClass is ${bugClass}")


        findbugsResults.BugInstance.each() { bugInstance ->

            if  ( bugInstance.Class.@classname.text() ==  bugClass )  {

                def type = bugInstance.@type.text()
                def category = bugInstance.@category.text()
                def message = bugInstance.LongMessage.text()
                def priority = evaluateThresholdParameter( bugInstance.@priority.text())
                def line = bugInstance.SourceLine.@start.text()
                log.info( message )

                getSink().bugInstance(type, priority, category, message, line)

            }
        }

        getSink().classTag_()
    }

    /**
     * Returns the threshold string value for the integer input.
     *
     * @param thresholdValue
     *            The ThresholdValue integer to evaluate.
     * @return The string valueof the Threshold object.
     *
     */
    protected String evaluateThresholdParameter(String thresholdValue)
    {
        String thresholdName

        switch ( thresholdValue )
        {
            case "1":
            thresholdName = "High"
            break
            case "2":
            thresholdName = "Normal"
            break
            case "3":
            thresholdName = "Low"
            break
            case "4":
            thresholdName = "Exp"
            break
            case "5":
            thresholdName = "Ignore"
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
        log.info("Printing Errors")
        getSink().errorTag()

            findbugsResults.Error.analysisError.each() {analysisError ->
                getSink().analysisErrorTag(analysisError.message.text())
            }

        log.info("Printing Missing classes")

            findbugsResults.Error.MissingClass.each() {missingClass ->
                getSink().missingClassTag(missingClass.text)
            }

        getSink().errorTag_()
    }

    /**
     * Output Source Directories.
     */
    protected void printSource()
    {
        log.info("Printing Source Roots")
        getSink().ProjectTag()

        if ( !compileSourceRoots.isEmpty() )
        {
            compileSourceRoots.each() {srcDir ->
                getSink().srcDirTag(srcDir)
            }
        }

        getSink().ProjectTag_()
    }

    /**
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
    value = bundle.getString(XDocsReporter.NOLINE_KEY)
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
    value = bundle.getString(XDocsReporter.NOLINE_KEY)
    }


    return value
    }
     */

    public void generateReport( )
    {
        doHeading()
        printBody()

        printErrors()

        printSource()

        getSink().body_()
        getSink().flush()
        getSink().close()

    }

    /**
     * Prints the top header sections of the report.
     */
    private void doHeading()
    {
        getSink().head()
        getSink().head_()

        getSink().body(getFindBugsVersion(), findbugsThresholds.get( threshold ), findbugsEfforts.get( effort ))

    }

    /**
     * Print th bug instance by file
     */
    void printBody()
    {
        log.debug("Finished searching for bugs!")

        findbugsResults.FindBugsSummary.PackageStats.ClassStats.each() { classStats ->

            def classStatsValue = classStats.'@class'.text()
            def classStatsBugCount = classStats.'@bugs'.text()

            if (classStatsBugCount.toInteger() > 0 ) {
                bugClasses << classStatsValue
            }
        }

        bugClasses.each() {bugClass ->
            log.info("finish bugClass is ${bugClass}")

            printBug( bugClass )
        }


    }
}
     