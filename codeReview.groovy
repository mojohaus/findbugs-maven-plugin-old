#!/usr/bin/env groovy

@Grapes([
     @Grab(group='org.codenarc', module='CodeNarc', version='0.16.1'),
     @GrabConfig(systemClassLoader = true)
])

def ant = new AntBuilder()

ant.java(classname: "org.codenarc.CodeNarc", fork: "false", failonerror: "false", clonevm: "false") {

  arg(value: '-basedir=./src/main/groovy')
  arg(value: '-title="FindBugs Maven Plugin"')
  arg(value: """"-rulesetfiles=rulesets/braces.xml,rulesets/convention.xml,rulesets/dry.xml,rulesets/exceptions.xml,
              rulesets/formatting.xml,rulesets/groovyism.xml,rulesets/imports.xml,rulesets/naming.xml,rulesets/unnecessary.xml,
              rulesets/unused.xml""")

}
