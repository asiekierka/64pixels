all: jars

.PHONY: all jars serverjar clientjar common client server clean

clean:
	@echo [[[ CLEANING ]]]
	rm -f 64pixels.jar
	rm -f 64px-srvr.jar
	rm -f 64px-app.jar
	rm common/*.class
	rm client/*.class
	rm server/*.class

jars: clientjar serverjar

serverjar: server
	@echo [[[ MAKING SERVER JAR ]]]
	jar cvfm 64px-srvr.jar manifests/srv-std.MF server/*.class common/*.class

clientjar: client
	@echo [[[ MAKING CLIENT JARs ]]]
	jar cvfm 64pixels.jar manifests/cli-std.MF common/*.class client/*.class client/rawcga.bin client/*.wav
	jar cvfm 64px-app.jar manifests/cli-app.MF common/*.class client/*.class	
common:
	@echo [[[ MAKING COMMON CODE ]]]
	javac common/*.java
	
client: common
	@echo [[[ MAKING CLIENT CODE ]]]
	javac client/*.java
	
server: common
	@echo [[[ MAKING SERVER CODE ]]]
	javac server/*.java
