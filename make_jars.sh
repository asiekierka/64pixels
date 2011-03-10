#!/bin/sh
echo [[[ MAKING SERVER JAR ]]]
jar cvfm 64px-srvr.jar manifests/srv-std.MF server/*.class common/*.class
echo [[[ MAKING CLIENT JARs ]]]
jar cvfm 64pixels.jar manifests/cli-std.MF common/*.class client/*.class client/rawcga.bin client/*.wav
jar cvfm 64px-app.jar manifests/cli-app.MF common/*.class client/*.class client/rawcga.bin client/*.wav

