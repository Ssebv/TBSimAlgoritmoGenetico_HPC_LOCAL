## MAKEFILE PARA PROYECTO DE ALGORITMOS GENÉTICOS 
# java version "17.0.2"
# javac 17.0.2

JAVAC = javac
JAVA = java
JAR = jar
SRCDIR = src
BINDIR = bin
LIBDIR = lib
DOCDIR = doc
CLASSPATH = $(LIBDIR)/jenetics-7.2.0.jar
SOURCES = $(shell find $(SRCDIR) -name '*.java')
CLASSES = $(SOURCES:$(SRCDIR)/%.java=$(BINDIR)/%.class)
MAINCLASS = MainJenetics
JFLAGS = -g -sourcepath $(SRCDIR) -d $(BINDIR) -classpath $(CLASSPATH)
JAVAFLAGS = -classpath $(CLASSPATH)
MANIFEST = Manifest.txt

all: $(CLASSES)

$(BINDIR)/%.class: $(SRCDIR)/%.java
	mkdir -p $(@D)
	$(JAVAC) $(JFLAGS) $< 

run: all
	$(JAVA) -classpath $(CLASSPATH):$(BINDIR) $(MAINCLASS) $(TASKID)

clean:
	$(RM) -r $(BINDIR)/*

doc:
	javadoc -d $(DOCDIR) -sourcepath $(SRCDIR) $(SOURCES)

jar: $(CLASSES)
	echo "Main-Class: $(MAINCLASS)" > $(MANIFEST)
	$(JAR) cfm $(MAINCLASS).jar $(MANIFEST) -C $(BINDIR) .

.PHONY: all clean doc run jar