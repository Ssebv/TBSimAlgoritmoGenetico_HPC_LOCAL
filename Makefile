## MAKEFILE PARA PROYECTO DE ALGORITMOS GENÉTICOS 
# java version "21.0.1" 2023-10-17 LTS
# javac 21.0.1

JAVAC = javac
JAVA = java
JAR = jar
SRCDIR = src
BINDIR = bin
LIBDIR = lib
DOCDIR = doc
CLASSPATH = $(LIBDIR)/jgap-3.4.4.jar:
SOURCES = $(shell find $(SRCDIR) -name '*.java')
CLASSES = $(SOURCES:$(SRCDIR)/%.java=$(BINDIR)/%.class)
MAINCLASS = Main
JFLAGS = -g -sourcepath $(SRCDIR) -d $(BINDIR) -classpath $(CLASSPATH)
JAVAFLAGS = -classpath $(CLASSPATH)
MANIFEST = Manifest.txt

# Default rule
all: $(CLASSES)

# Rule to compile .java to .class
$(BINDIR)/%.class: $(SRCDIR)/%.java
	mkdir -p $(@D)
	$(JAVAC) $(JFLAGS) $<

# Rule to run the program
run: all
	$(JAVA) -classpath $(CLASSPATH):$(BINDIR) $(MAINCLASS)

# Rule to clean the project (remove .class files and bin directory)
clean:
	$(RM) -r $(BINDIR)/*

# Rule to generate documentation with Javadoc
doc:
	javadoc -d $(DOCDIR) -sourcepath $(SRCDIR) $(SOURCES)

# Rule to create an executable JAR file
jar: $(CLASSES)
	echo "Main-Class: $(MAINCLASS)" > $(MANIFEST)
	$(JAR) cfm $(MAINCLASS).jar $(MANIFEST) -C $(BINDIR) .

.PHONY: all clean doc run jar