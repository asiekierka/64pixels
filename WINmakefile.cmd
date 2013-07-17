@echo off
echo [[[ CLEANING ]]]
del 64pixels.jar > NUL
del 64px-srvr.jar > NUL
del common\*.class
del client\*.class
del server\*.class
echo. 
echo [[[ MAKING COMMON CODE ]]]
javac -source 1.5 -target 1.5 -cp . common/*.java
echo.
echo [[[ MAKING CLIENT CODE ]]]
javac -source 1.5 -target 1.5 -cp . client/*.java
echo.
echo [[[ MAKING SERVER CODE ]]]
javac -source 1.5 -target 1.5 -cp . server/*.java
echo.
echo [[[ MAKING CLIENT JARs ]]]
jar cvfm 64pixels.jar manifests/cli-std.MF common/*.class client/*.class client/rawcga.bin client/*.wav
echo.
echo [[[ MAKING SERVER JAR ]]]
jar cvfm 64px-srvr.jar manifests/srv-std.MF server/*.class common/*.class
echo.
pause