set JAVA_HOME=.\jre1.8.0_112
set CATALINA_HOME=.\apache-tomcat-8.5.16

set JAVA_OPTS=-XX:MaxPermSize=128m -Xmx512M -Dwflow.home=./wflow/
REM set JAVA_OPTS=-XX:MaxPermSize=128m -Xmx1024M -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,suspend=n,server=y,address=5115 -Dwflow.home=./wflow/

%CATALINA_HOME%\bin\startup.bat

