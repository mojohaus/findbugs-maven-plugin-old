/*
 * Copyright (C) 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


assert new File(basedir, 'target/findbugsCheck.xml').exists()


println '**********************************'
println "Checking Findbugs Native XML file"
println '**********************************'

def path = new XmlSlurper().parse(new File(basedir, 'target/findbugsCheck.xml'))

allNodes = path.depthFirst().collect { it }
def findbugsErrors = allNodes.findAll {it.name() == 'BugInstance'}.size()
println "BugInstance size is ${findbugsErrors}"

assert findbugsErrors > 0

def findbugsXmlErrors = allNodes.findAll {it.name() == 'BugInstance' && it.@type == "DLS_DEAD_LOCAL_STORE"}.size()
println "BugInstance with includes size is ${findbugsXmlErrors}"

assert findbugsErrors == findbugsXmlErrors

