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

import edu.umd.cs.findbugs.BugReporter
import edu.umd.cs.findbugs.ErrorCountingBugReporter
import edu.umd.cs.findbugs.FindBugs2

/**
 * Helper for FindBugs2 class
 *
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id$
 */

class FindBugs2Proxy extends FindBugs2
{

    DelegateBugReporter bugReporter


    void initializeProxyReporter(int thresholdParameter)
    {
        DelegateBugReporter delegateBugReporter = new DelegateBugReporter()
        this.bugReporter = delegateBugReporter
        this.bugReporter.setPriorityThreshold(thresholdParameter)

        super.setBugReporter(new ErrorCountingBugReporter(delegateBugReporter))
    }

    /* (non-Javadoc)
     * @see edu.umd.cs.findbugs.IFindBugsEngine#getBugReporter()
     */

    BugReporter getBugReporter()
    {
        return this.bugReporter
    }

    /* (non-Javadoc)
     * @see edu.umd.cs.findbugs.IFindBugsEngine#setBugReporter(edu.umd.cs.findbugs.BugReporter)
     */

    void setBugReporter(BugReporter bugReporter)
    {
        ErrorCountingBugReporter errorCountingBugReporter = new ErrorCountingBugReporter(bugReporter)
        this.addClassObserver(errorCountingBugReporter)
        this.bugReporter.addClassObserver(errorCountingBugReporter)
    }
}