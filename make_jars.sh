#!/bin/sh
cd craftr-srvr
jar cvfm ../64px-srvr.jar MANIFEST.MF *.class
cd ../craftr
jar cvfm ../64pixels.jar MANIFEST.MF *.class rawcga.bin *.wav
jar cvfm ../64px-app.jar MANIFESTa.MF *.class rawcga.bin *.wav
cd ../common
jar uvf ../64px-srvr.jar *.class
jar uvf ../64pixels.jar *.class
jar uvf ../64px-app.jar *.class
cd ..
chown asiekierka *.jar

