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

import org.apache.maven.project.MavenProject

import org.codehaus.groovy.maven.mojo.GroovyMojo



/**
 * Launch the Findbugs GUI.
 * It will use all the parameters in the POM fle.
 * FindBugs 1.3.2 GUI does not have a load option.
 *
 * @since 2.0
 * @goal gui
 *
 *
 * @description Launch the Findbugs GUI using the parameters in the POM fle.
 * @requiresDependencyResolution compile
 * @requiresProject
 *
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id: FindBugsGui.groovy gleclaire $
 */

class FindBugsGui extends GroovyMojo
{
    /**
     * The name of the property resource bundle (Filesystem).
     *
     */
    static final String BUNDLE_NAME = "findbugs"

    /**
     * The name of the coreplugin.
     *
     */
    static final String FINDBUGS_COREPLUGIN = "report.findbugs.coreplugin"

    /**
     * The regex pattern to search for java class files.
     *
     */
    static final String JAVA_REGEX_PATTERN = "**/*.class"


    /**
     * Directory containing the class files for FindBugs to analyze.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     */
    File classFilesDirectory

    /**
     * turn on Findbugs debugging
     *
     * @parameter default-value="false"
     */
    Boolean debug

    /**
     * List of artifacts this plugin depends on. Used for resolving the Findbugs coreplugin.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    ArrayList pluginArtifacts

    /**
     * Effort of the bug finders. Valid values are Min, Default and Max.
     *
     * @parameter default-value="Default"
     *
     */
    String effort


    /**
     * The plugin list to include in the report. This is a comma-delimited list.
     *
     * @parameter
     *
     */
    String pluginList

    /**
     * Maven Project
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    MavenProject project

    /**
     * Resource bundle for a specific locale.
     *
     * @parameter
     * @readonly
     *
     */
    ResourceBundle bundle

    /**
     * Specifies the directory where the findbugs native xml output will be generated.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     *
     */
    File findbugsXmlOutputDirectory


    void execute()
    {

        def auxClasspathElements = project.compileClasspathElements

        ant.project.setProperty('basedir', findbugsXmlOutputDirectory.getAbsolutePath() )
        

        ant.java(classname: "edu.umd.cs.findbugs.LaunchAppropriateUI", fork: "true", failonerror: "true", clonevm: "true")
        {
//           jvmarg('-Dfindbugs.jaws=true')
           sysproperty(key: "findbugs.jaws", value: "true")
           classpath()
           {

                           auxClasspathElements.each() { auxClasspathElement ->
                               if ( log.isDebugEnabled() )
                               {
                                   log.info( "  Trying to Add to AuxClasspath ->" + auxClasspathElements.toString() )
                               }
                               pathelement(location: auxClasspathElement.toString() )
                           }
          }
        }
   }

}