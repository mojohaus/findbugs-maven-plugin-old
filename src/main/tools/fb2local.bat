
cls
@call mvn install:install-file -DpomFile=findbugs.pom -Dfile=lib/findbugs.jar -DgroupId=net.sourceforge.findbugs -DartifactId=findbugs -Dversion=%1% -Dpackaging=jar


@call mvn install:install-file -DpomFile=bcel.pom -Dfile=lib/bcel.jar -DgroupId=net.sourceforge.findbugs -DartifactId=bcel -Dversion=%1% -Dpackaging=jar


@call mvn install:install-file -DpomFile=annotations.pom -Dfile=lib/annotations.jar -DgroupId=net.sourceforge.findbugs -DartifactId=annotations -Dversion=%1% -Dpackaging=jar
 

@call mvn install:install-file -DpomFile=coreplugin.pom -Dfile=plugin/coreplugin.jar -DgroupId=net.sourceforge.findbugs -DartifactId=coreplugin -Dversion=%1% -Dpackaging=jar


@call mvn install:install-file -DpomFile=findbugs-ant.pom -Dfile=lib/findbugs-ant.jar -DgroupId=net.sourceforge.findbugs -DartifactId=findbugs-ant -Dversion=%1% -Dpackaging=jar


@call mvn install:install-file -DpomFile=findbugsGUI.pom -Dfile=lib/findbugsGUI.jar -DgroupId=net.sourceforge.findbugs -DartifactId=findbugsGUI -Dversion=%1% -Dpackaging=jar


@call mvn install:install-file -DpomFile=jsr305.pom -Dfile=jsr305.jar -DgroupId=net.sourceforge.findbugs -DartifactId=jsr305 -Dversion=%1% -Dpackaging=jar

