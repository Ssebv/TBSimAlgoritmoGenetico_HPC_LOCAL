## MAKEFILE PARA PROYECTO DE ALGORITMOS GENÉTICOS
# Asegúrate de tener:
# - JDK instalado (y accesible en PATH)
# - Estructura: src/ para fuentes, bin/ para clases, lib/ para librerías.

JAVAC = javac
JAVA = java
JAR = jar
SRCDIR = src
BINDIR = bin
LIBDIR = lib
DOCDIR = doc
MAINCLASS = MainJenetics

# Librería Jenetics
JENETICS_JAR = $(LIBDIR)/jenetics-7.2.0.jar

# Classpath para compilación y ejecución
CLASSPATH = $(BINDIR):$(JENETICS_JAR)

# Buscamos todos los .java en src/
SOURCES = $(shell find $(SRCDIR) -name '*.java')
CLASSES = $(SOURCES:$(SRCDIR)/%.java=$(BINDIR)/%.class)

# Opciones de compilación: 
# -g para información de depuración, 
# -sourcepath para indicar dónde están las fuentes,
# -d para destino,
# -classpath para las librerías,
# -Xlint:none para no mostrar advertencias.
JFLAGS = -g -sourcepath $(SRCDIR) -d $(BINDIR) -classpath $(CLASSPATH) -Xlint:none

# Opciones de ejecución
JAVAFLAGS = -classpath $(CLASSPATH)

# Archivo Manifest para el jar
MANIFEST = Manifest.txt

.PHONY: all clean doc run jar

# Compilar todo
all: $(CLASSES)

# Regla para compilar cada .java a .class
$(BINDIR)/%.class: $(SRCDIR)/%.java
	mkdir -p $(@D)
	$(JAVAC) $(JFLAGS) $<

# Ejecutar el programa principal
run: all
	@echo "Ejecutando el programa principal..."
	@chmod +x run_program.sh  # Asegurarse de que el script tenga permisos de ejecución
	./run_program.sh $(TASKID)

# Limpiar archivos compilados
clean:
	rm -rf $(BINDIR) $(DOCDIR) $(MAINCLASS).jar

# Generar la documentación
doc:
	javadoc -d $(DOCDIR) -sourcepath $(SRCDIR) $(SOURCES)

# Empaquetar en un JAR ejecutable
jar: $(CLASSES)
	echo "Main-Class: $(MAINCLASS)" > $(MANIFEST)
	$(JAR) cfm $(MAINCLASS).jar $(MANIFEST) -C $(BINDIR) .
