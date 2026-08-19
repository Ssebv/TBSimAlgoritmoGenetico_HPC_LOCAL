# Selección del simulador y del motor evolutivo

Documenta las alternativas evaluadas antes de fijar la plataforma del
experimento. El material de referencia correspondiente vive en
`simuladores_avaluados/`.

## Simuladores evaluados

| Alternativa | Resultado | Motivo |
|---|---|---|
| **TeamBots / TBSim** (`simuladores_avaluados/TeamBot_Base/`) | **Elegido** | Simulador de fútbol robótico maduro, en Java, con modo sin interfaz gráfica (`TBSimNoGraphics`) apto para ejecución masiva por lotes y para medir tiempo de cómputo sin coste de render. |
| **RoboCup Rescue** (`simuladores_avaluados/Robocup_Rescue/`) | Descartado | Dominio de rescate, no de fútbol; carga de despliegue mayor y menos control sobre el bucle de simulación. Se conservan las notas de evaluación en `docs/`. |
| **SoccerBots vía Python** (`simuladores_avaluados/TeamBot_Soccer_Python/`) | Descartado como plataforma principal | Se usó `main.py` para orquestar enfrentamientos "todos contra todos" y determinar el equipo rival de referencia. La capa Python añadía sobrecarga de proceso incompatible con la medición fina de tiempo por generación. |

## Motor de algoritmo genético

| Alternativa | Resultado | Motivo |
|---|---|---|
| **Jenetics 7.2.0** | **Elegido** | Mantenido activamente, API moderna, y —determinante para este trabajo— paralelismo configurable mediante `Executor`, que es justamente la variable independiente del experimento. |
| **JGAP 3.4.4** (`simuladores_avaluados/TeamBot_Jgap/`) | Descartado | Sin mantenimiento a la fecha del trabajo. Se conserva la iteración completa del proyecto sobre JGAP como evidencia de la comparación. |

## Nota sobre el contenido eliminado

Para mantener el repositorio manejable se dejaron de versionar los artefactos
binarios de estas distribuciones de referencia (bytecode `.class`, metadatos de
Kotlin, `.jar` duplicados, registros de simulación antiguos) y el material PDF
de terceros. **Todo ello permanece accesible en el historial y en el tag
[`v1.0-defensa`](https://github.com/Ssebv/TBSimAlgoritmoGenetico_HPC_LOCAL/releases/tag/v1.0-defensa)**, que conserva el estado
íntegro del repositorio en el momento de la defensa.

Material del NLHPC (Universidad de Chile) usado en la fase exploratoria en
clúster: se referencian los cursos en lugar de redistribuir los PDF.
Ver <https://wiki.nlhpc.cl/>. Las notas propias se conservan en
`analisis/nlhpc/apuntes.txt`.
