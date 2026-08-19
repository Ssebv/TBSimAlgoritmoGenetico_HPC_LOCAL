# NOTICE — Componentes de terceros

Este repositorio contiene, además del trabajo original del autor, software de
terceros que **no** está cubierto por la licencia MIT del archivo [`LICENSE`](./LICENSE).
Cada componente conserva su licencia original y sus obligaciones de atribución.

## TeamBots™ / TBSim — Tucker Balch, GTRC y CMU

> **Copyright (c)1999, 2000 Tucker Balch, GTRC and CMU. All rights Reserved.**

El simulador sobre el que se ejecuta este experimento es **TeamBots™**, obra de
**Tucker Balch, Georgia Tech Research Corporation (GTRC) y Carnegie Mellon
University (CMU)**, a quienes se reconoce expresamente como autores del software
base del que este trabajo deriva.

El texto completo e íntegro del aviso de copyright, cuya distribución junto al
software es **obligatoria** según sus propios términos, se encuentra en:

**[`THIRD_PARTY/TeamBots-COPYRIGHT.html`](./THIRD_PARTY/TeamBots-COPYRIGHT.html)**

### Archivos cubiertos

| Ruta | Nº archivos | Contenido |
|---|---|---|
| `src/EDU/gatech/cc/is/**` | 156 `.java` | Núcleo de TeamBots (Georgia Tech) |
| `src/EDU/cmu/cs/coral/**` | 53 `.java` | Extensiones CORAL (CMU) |
| `teams/**` | 18 `.java` | Equipos de ejemplo distribuidos con TeamBots |
| `simuladores_avaluados/TeamBot_*/**` | — | Distribuciones de referencia evaluadas |
| `robocup.dsc` | 1 | Descriptor de escenario de TeamBots |

En la raíz de `src/`, los siguientes archivos son **obra derivada** de TeamBots
(verificado por comparación directa contra la distribución original incluida en
`simuladores_avaluados/TeamBot_Base/`):

| Archivo | Estado respecto del original |
|---|---|
| `TBSimNoGraphics.java` | Modificado por el autor (ejecución sin interfaz gráfica) |
| `SimulationCanvas.java` | Modificado por el autor |
| `AIKHomoG.java` | Modificado por el autor — original de Håkan L. Younes |
| `BrianTeam.java` | Idéntico al original — de Brian McNamara (GaTech) |
| `NewRobotSpec.java` | Clase auxiliar derivada de la especificación de robots de TBSim |

### Restricciones que impone dicha licencia

Reproducidas aquí por conveniencia; el archivo enlazado arriba es el texto normativo:

1. No se puede derivar ingresos del software TeamBots en sí mismo.
2. Todo trabajo derivado debe reconocer a Tucker Balch, GTRC y CMU.
3. Toda copia debe ir acompañada del aviso de copyright y del archivo completo.
4. Se deben obedecer las restricciones del Gobierno de EE.UU. sobre
   redistribución o exportación.

Este repositorio es un trabajo académico sin fines de lucro, distribuido con la
atribución exigida por el punto 2 y el archivo de aviso exigido por el punto 3.

## Bibliotecas en `lib/`

| Archivo | Componente | Licencia |
|---|---|---|
| `jenetics-7.2.0.jar` | Jenetics | Apache License 2.0 |
| `oshi-core.jar` | OSHI | MIT |
| `jna-5.12.1.jar`, `jna-platform-5.12.1.jar` | Java Native Access | Apache 2.0 / LGPL 2.1 |
| `slf4j-api-1.7.36.jar`, `slf4j-simple-1.7.36.jar` | SLF4J | MIT |
| `jgap-3.4.4.jar` | JGAP | LGPL — evaluado en fase exploratoria, no usado en el experimento final |
| `collections.jar` | `com.sun.java.util.collections` (Sun Microsystems) | Ver [`lib/LICENSE.collections`](./lib/LICENSE.collections) |

## Material de referencia

`analisis/nlhpc/` contiene material formativo del Laboratorio Nacional de
Computación de Alto Rendimiento (NLHPC), Universidad de Chile, conservado como
referencia de la fase exploratoria en clúster. Los derechos pertenecen a sus autores.

## Trabajo original del autor

Cubierto por la licencia MIT ([`LICENSE`](./LICENSE)) — Sebastián Ignacio Allende Cuello, 2026:

- **Motor evolutivo e instrumentación** (`src/`): `MainJenetics`, `Configuracion`,
  `ConfiguracionSingleton`, `RuntimeConfig`, `LogManager`, `CSVManager`,
  `CpuSampler`, `GeneticEngineBuilder`, `EvolutionManager`, `GenerationProcessor`,
  `GenerationTracker`, `FuncionEvaluacionJenetics`, `DiversityManager`,
  `DiversityInjector`, `AdaptiveMutationController`, `UniqueEliteSelector`,
  `BasicTeamAG`.
- **Análisis** (`analisis/*.py`), **construcción** (`Makefile`), **ejecución**
  (`run_program.sh`, `AG_HPC.sh`) y **documentación** (`README.md`, este archivo).
