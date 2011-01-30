if exist 64pixels.jar del 64pixels.jar
jar cvfm 64pixels.jar MANIFEST.MF *.class rawcga.bins
jarsigner -verbose -keystore ../keystore.dat -storepass B7xa37SYmcP 64pixels.jar craftr
