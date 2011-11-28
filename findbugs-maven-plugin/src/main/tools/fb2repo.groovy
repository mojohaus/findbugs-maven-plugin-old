#!/usr/bin/env groovy

def findbugsHome = System.getenv("FINDBUGS_HOME")
def antBuilder = new AntBuilder()

def cli = new CliBuilder(usage:'fb2repo -f findbugs.home -version version -u repositoryURL')
cli.h(longOpt: 'help', 'usage information')
cli.f(argName: 'findbugs.home',  longOpt: 'home', required: false, args: 1, type:GString, 'Findbugs home directory')
cli.v(argName: 'version',  longOpt: 'version', required: true, args: 1, type:GString, 'Findbugs version')
cli.u(argName: 'url',  longOpt: 'url', required: true, args: 1, type:GString, 'Repository URL')

def opt = cli.parse(args)
if (!opt) { return }
if (opt.h) opt.usage()
if (opt.f) findbugsHome = opt.f
def findbugsVersion = opt.v
def repoUrl = opt.u

println "findbugsHome is ${findbugsHome}"
println "findbugsVersion is ${findbugsVersion}"
println "Done parsing"

def cmdPrefix = """"""

println "os.name is " + System.getProperty("os.name")

if (System.getProperty("os.name").toLowerCase().contains("windows")) cmdPrefix = """cmd /c """

def modules = ["findbugs", "annotations", "findbugs-ant", "bcel", "jsr305", "jFormatString" ]

modules.each(){ module ->
    antBuilder.copy(file: new File("${module}.pom"), toFile: new File("pom.xml") ) {
        filterset() {
            filter(token: "findbugs.version", value: "${findbugsVersion}")
        }
    }

    cmd = cmdPrefix + """mvn deploy:deploy-file -DpomFile=pom.xml -Dfile=${findbugsHome}/lib/${module}.jar -DgroupId=com.google.code.findbugs -DartifactId=${module} -Dversion=${findbugsVersion} -Durl=${repoUrl} -Dpackaging=jar"""
    proc = cmd.execute()
    println proc.text
    antBuilder.delete(file: "pom.xml")
}

