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
import edu.umd.cs.findbugs.ErrorCountingBugReporter
import edu.umd.cs.findbugs.TextUIBugReporter
import edu.umd.cs.findbugs.classfile.ClassDescriptor

import edu.umd.cs.findbugs.AnalysisError
import edu.umd.cs.findbugs.classfile.MethodDescriptor

class DelegateBugReporter extends TextUIBugReporter
{

    List reporterObserverList = []
    def bugCount = 0
    def fileCount = 0
    def missingClassCount = 0
    def errorCount = 0
    Set missingClassSet = []

    /**
     * Set the error-reporting verbosity level.
     *
     * @param level the verbosity level
     */
    void setErrorVerbosity(int level)
    {
        this.errorVerbosity = level

        this.reporterObserverList.each()
                {
                    ErrorCountingBugReporter bugReporter = it
                    bugReporter.setErrorVerbosity(level)
                }
    }

    /**
     * Finish reporting bugs.
     * If any bug reports have been queued, calling this method
     * will flush them.
     */
    void finish()
    {

        this.reporterObserverList.each()
                {
                    ErrorCountingBugReporter bugReporter = it
                    bugReporter.finish()
                }
    }

    /**
     * Report any accumulated error messages.
     */
    void reportQueuedErrors()
    {
        this.reporterObserverList.each()
                {
                    ErrorCountingBugReporter bugReporter = it
                    bugReporter.reportQueuedErrors()
                }
    }

    /* (non-Javadoc)
     * @see edu.umd.cs.findbugs.IFindBugsEngine#addClassObserver(edu.umd.cs.findbugs.classfile.IClassObserver)
     */

    public void addClassObserver(ErrorCountingBugReporter classObserver)
    {
        reporterObserverList << classObserver
    }

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
    protected void doReportBug(BugInstance bugInstance)
    {
        ++this.bugCount

        this.reporterObserverList.each()
                {
                    ErrorCountingBugReporter bugReporter = it
                    bugReporter.reportBug(bugInstance)
                }
    }

    /**
     * Observe a class.
     *
     * @param clazz
     *            the class
     * @see edu.umd.cs.findbugs.classfile.IClassObserver #observeClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
     */
    void observeClass(ClassDescriptor clazz)
    {
        ++this.fileCount
    }


    public void reportMissingClass(String s)
    {
        super.reportMissingClass(s)
    }

    public void reportAnalysisError(AnalysisError analysisError)
    {
        super.reportAnalysisError(analysisError)
    }

    public void reportMissingClass(ClassNotFoundException e)
    {
        super.reportMissingClass(e)
    }

    public void logError(String s)
    {
        ++this.errorCount

        this.reporterObserverList.each()
                {
                    ErrorCountingBugReporter bugReporter = it
                    bugReporter.logError(s)
                }
    }

    public void logError(String s, Throwable throwable)
    {
        ++this.errorCount

        this.reporterObserverList.each()
                {
                    ErrorCountingBugReporter bugReporter = it
                    bugReporter.logError(s, throwable)
                }
    }

    public void reportSkippedAnalysis(MethodDescriptor methodDescriptor)
    {
        super.reportSkippedAnalysis(methodDescriptor)

    }
}