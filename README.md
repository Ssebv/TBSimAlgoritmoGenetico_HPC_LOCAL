# Algoritmo Genético TeamBots TBSim - Experimento Local

## Descripción del Proyecto

Este proyecto se centra en la aplicación de algoritmos genéticos en entornos locales, utilizando el simulador TBSim para modelar y optimizar el comportamiento de equipos de fútbol robótico. Aunque originalmente el proyecto estaba orientado a entornos de Computación de Alto Rendimiento (HPC), en este experimento se realizará la ejecución en un entorno local. Esto se debe a que en HPC las tareas están limitadas a 30 minutos y se usan checkpoints para reanudar la ejecución, mientras que en local se pueden ejecutar simulaciones completas y visualizar los resultados mediante gráficos para un análisis detallado. El trabajo queda abierto a futuras investigaciones en HPC.

Para el algoritmo genético se utilizó la librería **Jenetics**, ya que aunque se probó **JGAP** en versiones anteriores, esta última no se encuentra actualizada a la fecha. La elección de Jenetics permitió implementar un algoritmo más robusto y modular, adaptado a las necesidades actuales del proyecto, además de aprovechar el paralelismo para intentar utilizar el 100% de los recursos disponibles en el sistema donde se procesará la simulación.

Dentro de la carpeta **simuladores_evaluados** se encuentran diferentes versiones del TBSim:

- **TBSim Basic (Gráfico):** Incluye una interfaz gráfica para visualizar las simulaciones.
- **TBSim No Graphics:** Una versión sin interfaz gráfica, utilizada en este experimento para permitir ejecuciones más rápidas y eficientes en local.
- Además, se incluyó un archivo en **Python** diseñado para simular enfrentamientos "todos contra todos" y determinar el mejor equipo, que servirá como rival en las competiciones.

Posteriormente, se procedió a crear y modularizar los archivos del proyecto (en la carpeta **src**) para que el programa funcione de manera óptima, integrando mecanismos de checkpoints, gestión de diversidad y adaptabilidad en la tasa de mutación. Además, se ha implementado la generación de un archivo `simulacion.log`, donde se registran todos los logs de la simulación, facilitando la depuración y análisis posterior. Esto permitió una mejor organización del código y facilitó la experimentación y el análisis de resultados mediante gráficos.

## Objetivos

### Objetivo General
Evaluar el rendimiento y la evolución de un algoritmo genético aplicado a la simulación de fútbol robótico en un entorno local, analizando la convergencia del fitness, la eficiencia computacional y la relación entre tamaño de la población y uso de recursos.

### Objetivos Específicos
- Configurar un ambiente local de desarrollo y simulación.
- Parametrizar y adaptar el simulador TBSim para la ejecución local.
- Integrar algoritmos genéticos en TBSim para ejecución en local.
- Implementar mecanismos de checkpoints para guardar el estado del algoritmo (aprovechables en HPC, pero aplicados aquí para análisis).
- Registrar y visualizar resultados (tiempo, uso de CPU, evolución del fitness, etc.) y guardar todos los logs en el archivo `simulacion.log`.
- Realizar análisis comparativos y generar gráficos para evaluar el comportamiento del sistema.

## Requerimientos

- **Python:** 3.11 o superior  
- **Java:** Versión "21.0.1" (2023-10-17 LTS)  
- **Bibliotecas:** Jenetics (por ejemplo, jenetics-7.2.0.jar) y/o JGAP (según se utilice)  
- **Herramientas de gráficos:** Excel, Python (matplotlib, seaborn), R, etc.

## Estructura del Proyecto

La estructura principal del proyecto es la siguiente:

- **.vscode/**: Configuraciones específicas de Visual Studio Code.  
- **analisis/**: Directorio para scripts o notebooks de análisis de datos y resultados previos.  
- **bin/**: Archivos compilados (.class).  
- **img/**: Contiene imágenes utilizadas en la documentación (por ejemplo, capturas de simulación).  
- **lib/**: Bibliotecas externas, incluyendo jenetics-7.2.0.jar.  
- **nlhpc/**: Scripts o configuraciones para HPC (se dejó en stand-by debido a los límites de tiempo de 30 minutos).  
- **resultados/**: Almacena salidas (CSV, logs, gráficos) de las simulaciones ejecutadas.  
- **simuladores_evaluados/**:  
  - **Robocup_Rescue/**: Versiones o pruebas con otro simulador.  
  - **TeamBot_Base/**: Contiene el TBSim base (gráfico).  
  - **TeamBot_Jgap/**: Pruebas con JGAP (no actualizado).  
  - **TeamBot_Soccer_Python/**: Scripts de Python para simulaciones “todos contra todos”.  
- **src/**: Contiene el código fuente principal del proyecto (clases del algoritmo genético, lógica de TBSim adaptada, etc.).  
- **teams/**: Directorio para configuraciones específicas de equipos.  
- **simulacion.log**: Archivo donde se almacenan todos los logs generados durante la simulación, facilitando la depuración y el análisis de resultados.  
- **.gitignore**: Lista de archivos y directorios que no se subirán al repositorio.

El proyecto también incluye un **Makefile** en la raíz para automatizar las tareas de compilación, ejecución, generación de documentación y empaquetado:

- `make`      : Compila el código fuente.  
- `make run`  : Ejecuta el programa.  
- `make clean`: Elimina los archivos compilados.  
- `make doc`  : Genera la documentación.  
- `make zip`  : Empaqueta los archivos para su entrega.

---

## Explicación de los Archivos en src/

### 1. MainJenetics.java
**Función:**  
Punto de entrada de la aplicación.

**Descripción:**  
- Carga la configuración global a través de `ConfiguracionSingleton`.
- Inicializa el sistema de logging (`LogManager`) y el registro en CSV (`CSVManager`).
- Gestiona los checkpoints, originalmente diseñados para HPC, y los aplica también en local.
- Crea y ejecuta la instancia de `EvolutionManager` para orquestar el proceso evolutivo en bloques de generaciones.
- Genera el archivo `simulacion.log` donde se registran todos los logs de la simulación.

### 2. Configuracion.java & ConfiguracionSingleton.java
**Configuracion.java:**  
Define todos los parámetros clave del experimento, tales como tamaño de población, número de generaciones, tasas de mutación y cruce, umbrales de mejora, estrategias de diversidad y el número de núcleos a utilizar.

**ConfiguracionSingleton.java:**  
Implementa el patrón Singleton para asegurar que la configuración se cargue una única vez y sea accesible globalmente.

### 3. LogManager.java
**Función:**  
Gestiona y formatea los mensajes de log.

**Descripción:**  
- Configura el logger para imprimir mensajes con colores (si se habilitan).
- Registra información detallada de cada generación (mejor fitness por generación, fitness global, promedio, diversidad, tiempo transcurrido).
- Registra eventos importantes como la selección de élites, resultados de partidos, checkpoints y un resumen final.
- Además, envía estos logs al archivo `simulacion.log` para su análisis posterior.
- Escribe los datos en el CSV mediante `CSVManager`.

### 4. CSVManager.java
**Función:**  
Administra la creación, escritura y cierre del archivo CSV.

**Descripción:**  
- Prepara el archivo CSV con una cabecera que incluye columnas para:  
  Generación, Mejor Fitness Generación, Fitness Global, Fitness Promedio, Diversidad, Peor Fitness, CPU (%), Memoria (%), Tiempo (s), Goles Favor, Goles Contra, OS, OS Version, Java Version, OS Arquitectura, CPUs (configurados), Population Size, Mutation Rate, Crossover Rate.
- El método `escribirLineaCSV` escribe una línea con los datos capturados, asegurando que el tiempo se registre en segundos.
- Se incluye un método de prueba para verificar la correcta escritura del archivo.

### 5. GeneticEngineBuilder.java
**Función:**  
Construye y configura el engine del algoritmo genético utilizando la librería Jenetics.

**Descripción:**  
- Define la fábrica de genotipos (un cromosoma de 60 genes con valores entre 1 y 5).
- Calcula dinámicamente las tasas de mutación y de cruce en función de la generación actual.
- Configura los operadores de variación (mutador y cruce) y la selección (se utiliza un `RouletteWheelSelector` para favorecer la exploración).
- Crea un `ExecutorService` configurado con `NUM_CORES` para ejecutar evaluaciones en paralelo.

### 6. GenerationProcessor.java
**Función:**  
Procesa cada bloque de generaciones generado por el engine.

**Descripción:**  
- Incrementa el contador global de generaciones mediante `GenerationTracker`.
- Calcula estadísticas (mejor, peor, promedio fitness y diversidad) y las registra a través de `LogManager` y `CSVManager`.
- Invoca estrategias de diversificación mediante `DiversityInjector` en caso de estancamiento.
- Gestiona el guardado de checkpoints a intervalos definidos.

### 7. FuncionEvaluacionJenetics.java
**Función:**  
Evalúa el fitness de cada genotipo mediante simulaciones en TBSim.

**Descripción:**  
- Valida que el genotipo tenga la cantidad mínima de genes.
- Ejecuta simulaciones para obtener resultados (goles a favor y en contra) y calcula el fitness usando una fórmula que combina componentes ofensivos, defensivos y bonus, con un factor de amplificación de 1.5.
- Registra y retorna el mejor fitness de la generación y actualiza el fitness global.
- Utiliza procesamiento paralelo para acelerar la evaluación.

### 8. DiversityManager.java & DiversityInjector.java
**DiversityManager.java:**  
Reintroduce diversidad reemplazando aleatoriamente un porcentaje de la población con nuevos individuos, asegurándose de que el mejor se preserve.

**DiversityInjector.java:**  
Calcula la diversidad de la población (basada en la distancia Euclidiana entre genotipos) y, si cae por debajo de un umbral, inyecta nuevos individuos reemplazando a los peores para evitar la convergencia prematura.

### 9. AdaptiveMutationController.java
**Función:**  
Ajusta la tasa de mutación de forma adaptativa según el número de generaciones sin mejora.

**Descripción:**  
- Si se detectan 15 generaciones consecutivas sin mejora, aumenta la tasa de mutación exponencialmente hasta un máximo, favoreciendo la exploración.

### 10. GenerationTracker.java
**Función:**  
Lleva el conteo global de generaciones de forma sincronizada.

**Descripción:**  
- Proporciona métodos para incrementar y obtener el contador de generaciones, asegurando la coherencia en entornos concurrentes.

### 11. BasicTeamAG.java
**Función:**  
Define el comportamiento del equipo de robots, modificando el BasicTeam base proporcionado por el simulador.

**Descripción:**  
- Se utilizan 60 parámetros en total: 10 para cada uno de los 5 jugadores y 6 parámetros adicionales para la coordinación del equipo.
- Estos parámetros determinan la fuerza, precisión, velocidad, y otros aspectos críticos en la toma de decisiones tanto ofensivas como defensivas.
- Se implementaron modificaciones para optimizar la ofensiva, basándose en pruebas "todos contra todos" realizadas con un archivo en Python, cuyo objetivo es encontrar el mejor equipo base que se utilizará como rival.
- Entre los parámetros clave se encuentran:
  - **NUM_PLAYERS:** 5  
  - **FIELD_LENGTH, GOAL_WIDTH, RANGE, IDEAL_DISTANCE:** Parámetros del campo y distancias relevantes.
  - **PlayerParams:** Clase interna que encapsula parámetros individuales (fuerza, peso defensivo, ataque, umbral de pase, evasión de oponentes, velocidad, precisión de disparo y pase, posicionamiento).
  - **Parámetros adicionales:** teamCoordinationWeight, proximityWeight, goalAlignmentWeight, gameStateAdaptation, aggressivenessWeight, defensiveLineWeight, que influyen en la estrategia global del equipo.

---

## Parámetros de Configuración del Experimento

### Generaciones
- **TARGET_GENERATIONS:** 1000  
  *Define el número total de generaciones que se ejecutarán en el experimento, es decir, el algoritmo evolucionará la población durante 1000 generaciones en total.*

- **DEFAULT_GENERATIONS:** 50  
  *Indica el tamaño de cada bloque de generaciones procesadas secuencialmente. En lugar de ejecutar las 1000 generaciones de una sola vez, el algoritmo se ejecuta en bloques de 50 generaciones. Esto permite realizar tareas intermedias (por ejemplo, guardar checkpoints, actualizar estadísticas, etc.) entre cada bloque y facilita el análisis del progreso.*

### Población
- **INITIAL_POPULATION_SIZE:** 100  
  *Define el número de individuos que componen cada generación. En este experimento se fija en 100 para mantener un nivel adecuado de diversidad y realizar comparaciones de rendimiento.*

### Tasas de Variación
- **MUTATION_RATE:** 0.40  
  *Probabilidad base de que un gen se modifique en cada generación. Una tasa del 40% favorece la exploración del espacio de soluciones, aunque si es demasiado alta puede destruir información valiosa.*

- **CROSSOVER_RATE:** 0.70 (local)  
  *Probabilidad de realizar un cruce entre dos individuos. En entornos locales se utiliza una tasa del 70%, lo que permite combinar las características de los padres para generar descendientes con potencialmente mejores soluciones.*

- **ELITE_COUNT:** 5  
  *Número de mejores individuos que se preservan sin modificaciones en cada generación mediante elitismo. Esto ayuda a mantener soluciones de alta calidad, aunque debe usarse con cautela para no reducir la diversidad.*

### Detección de Estancamiento
- **THRESHOLD_MEJORA:** 0.09  
  *Umbral mínimo de mejora en fitness que debe alcanzarse en una generación para considerarse que hay progreso. Si la mejora es menor, se puede detectar estancamiento.*

- **MAX_GENERACIONES_SIN_MEJORA:** 15  
  *Número de generaciones consecutivas sin una mejora significativa (por debajo del umbral) que desencadenan estrategias de diversificación.*

### Gestión de Diversidad
- **MIN_DIVERSITY_THRESHOLD:** 0.5  
  *Valor mínimo permitido de diversidad entre los individuos, medido como la distancia promedio entre los genotipos. Esto evita la convergencia prematura.*

- **DIVERSITY_INJECTION_PERCENTAGE:** 0.3  
  *Porcentaje de la población que se reemplaza por nuevos individuos aleatorios si la diversidad cae por debajo del umbral, ayudando a mantener la exploración.*

### Checkpoints
- **CHECKPOINT_INTERVAL:** 10  
  *Intervalo en generaciones en el que se guarda el estado del algoritmo. Esto es útil para poder reanudar la ejecución en caso de fallos, especialmente en entornos HPC, aunque en este experimento se realiza en local.*

### Operador de Cruce y Optimización
- **USE_UNIFORM_CROSSOVER:** false (cruce de un solo punto)  
  *Determina el tipo de operador de cruce a utilizar. En este caso, se utiliza el cruce de un solo punto, que divide el cromosoma en un punto fijo y combina segmentos de los padres.*

- **OPTIMIZE:** Optimize.MAXIMUM  
  *Define la estrategia de optimización, es decir, se busca maximizar el fitness.*

### Configuración de Núcleos
- **NUM_CORES:** Se probarán configuraciones de 1, 2, 4 y 8  
  *Define el número de threads para la ejecución paralela, permitiendo evaluar el impacto en el tiempo de ejecución y la carga de CPU.*

### Parámetros del Equipo (BasicTeamAG)
- **Total de parámetros:** 60 (10 por cada uno de los 5 jugadores + 6 parámetros adicionales para la coordinación del equipo).  
  *Estos parámetros definen la estrategia ofensiva y defensiva del equipo. Se optimizaron tras evaluaciones previas (incluyendo simulaciones "todos contra todos" en Python) para determinar el mejor equipo base que se utilizará como rival.*

---

## Gráficos y Análisis Posteriores

### Gráfico 1: Tiempo vs. CPU
- **Objetivo:**  
  Medir el tiempo total de ejecución y la carga de CPU para distintas configuraciones de núcleos (1, 2, 4, 8).
- **Datos Capturados:**  
  - **Tiempo (s):** Tiempo de ejecución en segundos.  
  - **CPU (%):** Porcentaje de carga de CPU durante la ejecución.
- **Interpretación:**  
  - **Eje X:** Número de núcleos utilizados.  
  - **Eje Y:** Tiempo de ejecución y carga de CPU.  
  - **Análisis:** Se espera que al aumentar los núcleos, el tiempo disminuya gracias a la paralelización, mientras la carga de CPU se acerque a su valor máximo, evaluando así la escalabilidad del algoritmo.

### Gráfico 2: Fitness vs. Generación
- **Objetivo:**  
  Visualizar la evolución del fitness a lo largo de las generaciones.
- **Datos Capturados:**  
  - **Mejor Fitness Generación:** Mejor valor obtenido en cada generación.
  - **Fitness Global:** Mejor valor acumulado hasta ese momento.
  - **Fitness Promedio:** Promedio del fitness en la generación.
  - **Peor Fitness:** Valor mínimo en la generación.
- **Interpretación:**  
  - **Eje X:** Generaciones (1 a 1000).  
  - **Eje Y:** Valores de fitness.  
  - **Análisis:** La comparación entre estas curvas permitirá evaluar la convergencia del algoritmo y detectar posibles estancamientos.

### Gráfico 3: CPU vs. Población
- **Objetivo:**  
  Evaluar el impacto del tamaño de la población en la carga de CPU.
- **Datos Capturados:**  
  - **Population Size:** Tamaño de la población (en este experimento se fija en 50, pero se podrá variar en futuros experimentos).  
  - **CPU (%):** Porcentaje de carga de CPU.
- **Interpretación:**  
  - **Eje X:** Tamaño de la población.  
  - **Eje Y:** Carga de CPU.  
  - **Análisis:** Este gráfico ayudará a balancear la diversidad y la eficiencia, ya que un mayor tamaño de población puede incrementar la carga de CPU y el tiempo de ejecución.

---

## Conclusiones y Extensiones Futuras

- **Tiempo vs. CPU:**  
  Se determinará la escalabilidad del algoritmo y se identificarán cuellos de botella en la ejecución paralela.
  
- **Fitness vs. Generación:**  
  Se evaluará la convergencia del algoritmo y la efectividad de las estrategias de diversificación para mantener la diversidad.
  
- **CPU vs. Población:**  
  Permitirá optimizar el balance entre diversidad y eficiencia computacional.
  
- **Extensiones Optativas:**  
  - Ajustar las tasas de mutación y cruce para optimizar la exploración y explotación.  
  - Experimentar con diferentes operadores de cruce (uniforme vs. de un solo punto).  
  - Modificar umbrales de mejora y la frecuencia de checkpoints.  
  - Variar el tamaño de la población y aumentar el número de simulaciones por individuo para evaluaciones más robustas.

---


