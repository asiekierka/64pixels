all: jars

.PHONY: all jars serverjar clientjar common client server clean

clean:
	@echo [[[ CLEANING ]]]
	rm -f 64pixels.jar
	rm -f 64px-srvr.jar
	rm common/*.class
	rm client/*.class
	rm server/*.class

jars: clientjar serverjar

serverjar: server
	@echo [[[ MAKING SERVER JAR ]]]
	jar cvfm 64px-srvr.jar manifests/srv-std.MF server/*.class common/*.class docs/LICENSE

clientjar: client
	@echo [[[ MAKING CLIENT JARs ]]]
	jar cvfm 64pixels.jar manifests/cli-std.MF common/*.class client/*.class client/rawcga.bin client/*.wav docs/LICENSE
	
common:
	@echo [[[ MAKING COMMON CODE ]]]
	javac -source 1.5 -target 1.5 -cp . common/*.java
	
client: common
	@echo [[[ MAKING CLIENT CODE ]]]
	javac -source 1.5 -target 1.5 -cp . client/*.java
	
server: common
	@echo [[[ MAKING SERVER CODE ]]]
	javac -source 1.5 -target 1.5 -cp "lib/*:." server/*.java
