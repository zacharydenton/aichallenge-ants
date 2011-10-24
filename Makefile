JAVAC=javac
JAVAC_ARGS=

SOURCES=MyBot.java
CLASSES=$(SOURCES:%.java=%.class)

.PHONY: all clean

all: $(CLASSES)
	javac *.java
	jar cvfm MyBot.jar Manifest.txt $(CLASSES)

%.class: %.java
	$(JAVAC) $(JAVAC_ARGS) $<

clean:
	-rm -Rf *.class
	-rm -Rf *.jar
