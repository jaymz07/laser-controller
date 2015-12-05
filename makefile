JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Point.java \
	AudioCommon.java \
	LaserOutputThread.java \
	GraphicalLaserOutput.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
