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

import org.apache.maven.artifact.DependencyResolutionRequiredException
import org.apache.maven.artifact.resolver.ArtifactNotFoundException
import org.apache.maven.artifact.resolver.ArtifactResolutionException

import org.apache.maven.project.MavenProject


import org.codehaus.groovy.maven.mojo.GroovyMojo

import org.codehaus.plexus.resource.ResourceManager
import org.codehaus.plexus.resource.loader.FileResourceLoader

import org.codehaus.plexus.util.FileUtils


/**
 * Fail the build if there were any FindBugs violations in the source code.
 * An XML report is put out by default in the target directory with the errors.
 * To see more documentation about FindBugs' options, please see the
 * <a href="http://findbugs.sourceforge.net/manual/index.html">FindBugs Manual.</a>
 *
 * 
 * @since 2.0
 * @goal check
 * @phase verify
 * 
 * @requiresDependencyResolution compile
 * @requiresProject
 *
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id: FindbugsViolationCheckMojo.groovy gleclaire $
 */

class FindbugsViolationCheckMojo extends FindbugsAbstractMojo
{

    /**
     * Fail the build on an error.
     *
     * @parameter default-value="true"
     * @since 2.0
     */
    boolean failOnError

    
    void execute()
    {
        Locale locale = Locale.getDefault()
        List sourceFiles
        
        def bundle = ResourceBundle.getBundle( BUNDLE_NAME, locale )

        log.info("Excecuting findbugs:check")

        if ( this.classFilesDirectory.exists() && this.classFilesDirectory.isDirectory() )
        {
            sourceFiles = FileUtils.getFiles(classFilesDirectory, JAVA_REGEX_PATTERN, null)
        }

        if ( !skip && sourceFiles )
        {

            // this goes
           
            log.info("Here goes...............Excecuting findbugs:check")

            File outputFile= new File("${findbugsXmlOutputDirectory}/findbugsCheck.xml" )

            executeFindbugs( locale, outputFile )


  
            if (( bugCount || errorCount ) && failOnError )
            {
                fail("failed with ${bugCount} bugs and ${errorCount} errors ")
            }

        }
        else
        {
            log.info( "Nothing for FindBugs to do here." )
        }
    }

  
}
