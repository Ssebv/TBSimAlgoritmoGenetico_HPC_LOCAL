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
SOURCES = $(shell find $(SRCDIR) -name '*.java') # Encuentra todos los archivos .java en SRCDIR
CLASSES = $(SOURCES:$(SRCDIR)/%.java=$(BINDIR)/%.class) # Mapeo de archivos fuente a archivos de clase
MAINCLASS = Main              # Clase principal para ejecutar
JFLAGS = -g -sourcepath $(SRCDIR) -d $(BINDIR) -classpath $(CLASSPATH) # Flags para el compilador Java
JAVAFLAGS = -classpath $(CLASSPATH) # Flags para ejecutar programas Java
MANIFEST = Manifest.txt       # Nombre del archivo de manifiesto para archivos JAR

# Regla por defecto para hacer todo
all: $(CLASSES)

# Regla para compilar .java a .class
$(BINDIR)/%.class: $(SRCDIR)/%.java
	mkdir -p $(@D)            # Crea el directorio para los archivos de clase si no existe
	$(JAVAC) $(JFLAGS) $<     # Compila el archivo fuente

# Regla para ejecutar el programa
run: all
	$(JAVA) -classpath $(CLASSPATH):$(BINDIR) $(MAINCLASS)

# Regla para limpiar el proyecto (eliminar archivos .class y el directorio bin)
clean:
	$(RM) -r $(BINDIR)/*      # Elimina todos los archivos en el directorio BINDIR

# Regla para generar documentación con Javadoc
doc:
	javadoc -d $(DOCDIR) -sourcepath $(SRCDIR) $(SOURCES) # Genera documentación para todos los archivos fuente

# Regla para crear un archivo JAR ejecutable
jar: $(CLASSES)
	echo "Main-Class: $(MAINCLASS)" > $(MANIFEST) # Crea el archivo de manifiesto
	$(JAR) cfm $(MAINCLASS).jar $(MANIFEST) -C $(BINDIR) . # Crea el archivo JAR

.PHONY: all clean doc run jar # Declara objetivos que no corresponden a archivos