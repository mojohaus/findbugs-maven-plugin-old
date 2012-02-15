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
 * @version $Id: FindBugsInfo.groovy  $
 */



interface FindBugsInfo {

	/**
	 * The name of the Plug-In.
	 *
	 */
	static final String PLUGIN_NAME = "findbugs"

	/**
	 * The name of the property resource bundle (Filesystem).
	 *
	 */
	static final String BUNDLE_NAME = "findbugs"

	/**
	 * The key to get the name of the Plug-In from the bundle.
	 *
	 */
	static final String NAME_KEY = "report.findbugs.name"

	/**
	 * The key to get the description of the Plug-In from the bundle.
	 *
	 */
	static final String DESCRIPTION_KEY = "report.findbugs.description"

	/**
	 * The key to get the source directory message of the Plug-In from the bundle.
	 *
	 */
	static final String SOURCE_ROOT_KEY = "report.findbugs.sourceRoot"

	/**
	 * The key to get the source directory message of the Plug-In from the bundle.
	 *
	 */
	static final String TEST_SOURCE_ROOT_KEY = "report.findbugs.testSourceRoot"

	/**
	 * The key to get the java source message of the Plug-In from the bundle.
	 *
	 */
	static final String JAVA_SOURCES_KEY = "report.findbugs.javasources"

	/**
	 * The regex pattern to search for java class files.
	 *
	 */
	static final String JAVA_REGEX_PATTERN = "**/*.class"

	static final String COMMA = ","

	static final String FORWARD_SLASH = '/'

	/**
	 * The character to separate URL tokens.
	 *
	 */
	static final String URL_SEPARATOR = "/"

	static final String BLANK = " "

	static final String PERIOD = "."

	static final EOL = "\n"

	public static final String URL = "url"

	static final String CLASS_SUFFIX = '.class'

	def findbugsEfforts = [Max: "max", Min: "min", Default: "min"]

	def findbugsThresholds = [High: "high", Exp: "experimental", Low: "low", Medium: "medium", Default: "medium"]

	def findbugsPriority = ["unknown", "High", "Medium", "Low"]
}
