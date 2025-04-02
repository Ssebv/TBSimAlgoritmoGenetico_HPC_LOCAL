# Configuración básica
JAVAC = javac
JAVA = java
JAR = jar
SRCDIR = src
BINDIR = bin
LIBDIR = lib
DOCDIR = doc
MAINCLASS = MainJenetics

# Librerías
JENETICS_JAR = $(LIBDIR)/jenetics-7.2.0.jar
OSHI_JAR = $(LIBDIR)/oshi-core.jar
JNA_JAR = $(LIBDIR)/jna-5.12.1.jar
JNA_PLATFORM_JAR = $(LIBDIR)/jna-platform-5.12.1.jar
SLF4J_API_JAR = $(LIBDIR)/slf4j-api-1.7.36.jar
SLF4J_SIMPLE_JAR = $(LIBDIR)/slf4j-simple-1.7.36.jar

# Classpath completo
CLASSPATH = $(BINDIR):$(JENETICS_JAR):$(OSHI_JAR):$(JNA_JAR):$(JNA_PLATFORM_JAR):$(SLF4J_API_JAR):$(SLF4J_SIMPLE_JAR)

# Fuentes
SOURCES = $(shell find $(SRCDIR) -name '*.java')
CLASSES = $(SOURCES:$(SRCDIR)/%.java=$(BINDIR)/%.class)

# Opciones
JFLAGS = -g -sourcepath $(SRCDIR) -d $(BINDIR) -classpath $(CLASSPATH) -Xlint:none
JAVAFLAGS = -classpath $(CLASSPATH)
MANIFEST = Manifest.txt

.PHONY: all clean doc run jar check-libs

# -----------------------------------
# Objetivos principales
# -----------------------------------

all: check-libs $(CLASSES)

# Compilar archivos .java
$(BINDIR)/%.class: $(SRCDIR)/%.java
	@mkdir -p $(@D)
	@echo "Compilando $<..."
	@$(JAVAC) $(JFLAGS) $<

# Ejecutar con posibilidad de pasar parámetros (ej: make run TASKID=8)
run: all
	@echo "Ejecutando $(MAINCLASS)..."
	@$(JAVA) $(JAVAFLAGS) $(MAINCLASS) $(TASKID)

# Documentación
doc:
	@mkdir -p $(DOCDIR)
	@javadoc -d $(DOCDIR) -sourcepath $(SRCDIR) $(SOURCES)

# JAR ejecutable
jar: all
	@echo "Main-Class: $(MAINCLASS)" > $(MANIFEST)
	@$(JAR) cfm $(MAINCLASS).jar $(MANIFEST) -C $(BINDIR) .
	@echo "✅ JAR generado: $(MAINCLASS).jar"

# Limpieza
clean:
	@rm -rf $(BINDIR) $(DOCDIR) $(MAINCLASS).jar $(MANIFEST)
	@echo "🧹 Limpieza completada."

# Validar existencia de librerías necesarias
check-libs:
	@if [ ! -f $(JENETICS_JAR) ]; then echo "Falta $(JENETICS_JAR)"; exit 1; fi
	@if [ ! -f $(OSHI_JAR) ]; then echo "Falta $(OSHI_JAR)"; exit 1; fi
	@if [ ! -f $(JNA_JAR) ]; then echo "Falta $(JNA_JAR)"; exit 1; fi
	@if [ ! -f $(JNA_PLATFORM_JAR) ]; then echo "Falta $(JNA_PLATFORM_JAR)"; exit 1; fi
	@if [ ! -f $(SLF4J_API_JAR) ]; then echo "Falta $(SLF4J_API_JAR)"; exit 1; fi
	@if [ ! -f $(SLF4J_SIMPLE_JAR) ]; then echo "Falta $(SLF4J_SIMPLE_JAR)"; exit 1; fi
	@echo "Todas las librerías requeridas están presentes."

