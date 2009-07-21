                                                                                           
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


/**
 * Generates a FindBugs Report when the site plugin is run.
 * The HTML report is generated for site commands only.
 * To see more documentation about FindBugs' options, please see the
 * <a href="http://findbugs.sourceforge.net/manual/index.html">FindBugs Manual.</a>
 * 
 * 
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id: FindBugsMojo.groovy 9277 2009-03-25 02:59:25Z gleclaire $
 */
interface FindBugsInfo
{
 
    def findbugsEfforts = [Max: "max", Min:"min", Default:"default"]

    def findbugsThresholds = [High: "high", Exp: "experimental",  Low: "low", Medium:"medium"]

 }
