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

import edu.umd.cs.findbugs.BugInstance
import edu.umd.cs.findbugs.BugReporter
import edu.umd.cs.findbugs.BugReporterObserver
import edu.umd.cs.findbugs.ErrorCountingBugReporter
import edu.umd.cs.findbugs.ProjectStats
import edu.umd.cs.findbugs.TextUIBugReporter
import edu.umd.cs.findbugs.classfile.ClassDescriptor
import edu.umd.cs.findbugs.classfile.IClassObserver

import edu.umd.cs.findbugs.AnalysisError
import edu.umd.cs.findbugs.classfile.MethodDescriptor

class DelegateBugReporter extends TextUIBugReporter
{

    List reporterObserverList= []
    Writer writer = new PrintWriter( System.out )
    def bugCount = 0
    def fileCount = 0
    def missingClassCount = 0
    def errorCount = 0
    Set missingClassSet = []

/*
    Object invokeMethod( String name, Object[] params )
    {
        writer.write( "Calling invokeMethod $name \n" )
        writer.flush()

    }
*/
    /**
     * Set the error-reporting verbosity level.
     *
     * @param level the verbosity level
     */
    void setErrorVerbosity(int level)
    {
        println "setting Error Verbosity"

        this.errorVerbosity = level

        this.reporterObserverList.each()
        {
            ErrorCountingBugReporter bugReporter = it
            println bugReporter.toString()

            bugReporter.setErrorVerbosity( level )
        }
    }

    /**
     * Set the priority threshold.
     *
     * @param threshold bug instances must be at least as important as
     *                  this priority to be reported
    void setPriorityThreshold(int threshold)
    {
        System.out.println "setting Priority Threshold"

        this.priorityThreshold = threshold

        System.out.println "set Priority Threshold to $priorityThreshold"
        System.out.println "Finished setting Priority Threshold"
        this.reporterObserverList.each()
        {
            ErrorCountingBugReporter bugReporter = it
            println bugReporter.toString()

            bugReporter.setPriorityThreshold( threshold )
        }
    }
*/

    

    /**
     * Finish reporting bugs.
     * If any bug reports have been queued, calling this method
     * will flush them.
     */
     void finish()
     {

         println "wrapping it up"
         println "bugCount is $bugCount"
         println "fileCount is $fileCount"
         println "missingClassCount is $missingClassCount"
         println "errorCount is $errorCount"
         println()

         def proxyStats = this.projectStats

         proxyStats.recomputeFromClassStats()
         println "---- Delegate Reporter Project Stats ----"
         println "Code Size is " + proxyStats.codeSize
         println "Number of Classes is " + proxyStats.numClasses
         println "Time Stamp is " + proxyStats.timestamp
         println "Total Bugs is " + proxyStats.totalBugs

         println()
         println proxyStats
         println()

         
         println "reporterObserverList size is "  + reporterObserverList.size()

         this.reporterObserverList.each()
         {
             ErrorCountingBugReporter bugReporter = it
             println( "Finishing $bugReporter" )
             def stats = bugReporter.projectStats

             stats.recomputeFromClassStats()
             println "Code Size is " + stats.codeSize
             println "Number of is " + stats.numClasses
             println "Time Stamp is " + stats.timestamp
             println "Total Bugs is " + stats.totalBugs

             println()
             println()
             println stats
             println()

             bugReporter.finish()
         }
     }

    /**
     * Report any accumulated error messages.
     */
    void reportQueuedErrors()
    {

        println "Finishing QueuedErrors"

        this.reporterObserverList.each()
        {
            ErrorCountingBugReporter bugReporter = it

            bugReporter.reportQueuedErrors()
        }
    }

    /**
     * Add an observer.
     *
     * @param observer the observer
     */
//    void addObserver(BugReporterObserver observer){ }

    /* (non-Javadoc)
     * @see edu.umd.cs.findbugs.IFindBugsEngine#addClassObserver(edu.umd.cs.findbugs.classfile.IClassObserver)
     */
    public void addClassObserver(ErrorCountingBugReporter classObserver)
    {
        reporterObserverList.add(classObserver)
    }

    /**
     * Get ProjectStats object used to store statistics about
     * the overall project being analyzed.
     */
//    ProjectStats getProjectStats() {  }

    /**
     * Get the real bug reporter at the end of a chain of delegating bug reporters.
     * All non-delegating bug reporters should simply "return this".
     *
     * @return the real bug reporter at the end of the chain, or
     *          this object if there is no delegation
     */
    BugReporter getRealBugReporter()
    {
        return this
    }


    /**
     * @param bugInstance
     *            The bug to report
     * @see edu.umd.cs.findbugs.AbstractBugReporter #doReportBug(edu.umd.cs.findbugs.BugInstance)
     */
    protected void doReportBug( BugInstance bugInstance )
    {
        ++this.bugCount

        this.reporterObserverList.each()
        {
            ErrorCountingBugReporter bugReporter = it

            bugReporter.reportBug( bugInstance )
        }
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
        ++this.fileCount

/*
        this.reporterObserverList.each()
        {
            ErrorCountingBugReporter bugReporter = it

            bugReporter.observeClass( clazz )
        }
*/        
    }


    public void reportMissingClass(String s)
    {
        println( "delegate reportMissingClass" )
        super.reportMissingClass(s); //To change body of overridden methods use File | Settings | File Templates.
    }

    public void reportAnalysisError( AnalysisError analysisError )
    {
        println( "delegate reportMissingClass" )
        super.reportAnalysisError(analysisError); //To change body of overridden methods use File | Settings | File Templates.
    }

    public void reportMissingClass( ClassNotFoundException e )
    {
        super.reportMissingClass(e)
    }

    public void logError(String s)
    {
        ++this.errorCount

        this.reporterObserverList.each()
        {
            ErrorCountingBugReporter bugReporter = it

            bugReporter.logError( s )
        }
    }
                             
    public void logError( String s, Throwable throwable )
    {
        ++this.errorCount

        this.reporterObserverList.each()
        {
            ErrorCountingBugReporter bugReporter = it

            bugReporter.logError( s, throwable )
        }
    }

    public void reportSkippedAnalysis( MethodDescriptor methodDescriptor )
    {
        super.reportSkippedAnalysis( methodDescriptor )

    }


    public int getMissingClassCount()
    {
        return this.missingClassCount
    }



}