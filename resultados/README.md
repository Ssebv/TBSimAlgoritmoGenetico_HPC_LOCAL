# Datos de telemetría — diccionario y advertencias

Este directorio contiene los CSV de telemetría generados por `CSVManager.java`
durante las corridas del algoritmo genético.

---

## ⚠️ Advertencia crítica: `Tiempo (s)` es ACUMULADO

**La columna `Tiempo (s)` NO es el tiempo por generación: es el tiempo total
transcurrido desde el inicio de la corrida.**

Para obtener el tiempo por generación hay que aplicar una diferencia:

```python
df["tiempo_gen"] = df["Tiempo (s)"].diff()
```

Interpretar esa columna directamente como tiempo por generación produce cifras
de speedup infladas. **Ese error concreto originó un speedup erróneo de 15,8x
en versiones preliminares de este trabajo; la cifra correcta es 6,3x**, y existe
una fe de erratas al respecto. Los scripts de `analisis/` aplican la corrección
automáticamente a través de `tbsim_stats.cargar_corrida()`; se recomienda usarlos
en lugar de leer los CSV a mano.

Nota adicional: el valor de la **primera generación** incluye el arranque de la
JVM y la carga del simulador, por lo que no es comparable con el resto. Los
scripts lo descartan.

---

## Los dos esquemas de CSV

Existen dos formatos, ambos válidos. **Léelos siempre por nombre de columna,
nunca por posición.**

| Esquema | Columnas | `Chromosoma` | Dónde aparece |
|---|---|---|---|
| **v1** | 28 | Sí (posición 12) | Corridas archivadas en este directorio |
| **v2** | 27 | No | Salida del código actual |

La columna `Chromosoma` se eliminó deliberadamente de la salida (ver el
comentario en `CSVManager.prepararCSV`) porque su tamaño dominaba el archivo.
Como consecuencia, **todas las columnas posteriores a `Goles Contra` están
desplazadas una posición entre ambos esquemas**: un script que lea por índice
producirá resultados incorrectos en silencio.

### Columnas

| Columna | Descripción |
|---|---|
| `Generación` | Índice de la generación, desde 1 |
| `Mejor Fitness Generación` | Mejor aptitud de esa generación |
| `Fitness Global` | Mejor aptitud histórica acumulada |
| `Fitness Promedio` | Aptitud media de la población |
| `Diversidad` | Métrica de diversidad genética |
| `Peor Fitness` | Peor aptitud de la generación |
| `CPU (%)` | Uso agregado de CPU |
| `Memoria (%)` | Uso de memoria |
| `Tiempo (s)` | **ACUMULADO** desde el inicio (ver advertencia) |
| `Goles Favor` / `Goles Contra` | Resultado de la simulación del mejor individuo |
| `Chromosoma` | *(solo v1)* Vector de genes del mejor individuo |
| `OS`, `OS Version`, `Java Version`, `OS Arquitectura` | Entorno de ejecución |
| `CPUs (configurados)` | Núcleos asignados al paralelismo — **factor experimental** |
| `Population Size` | Tamaño de población — **factor experimental** |
| `Mutation Rate`, `Crossover Rate` | Tasas de variación |
| `Core0 (%)` … `Core7 (%)` | Uso por núcleo individual |

---

## Qué contiene cada archivo

| Archivo | Configuración | Naturaleza |
|---|---|---|
| `local_stats1.csv` … `local_stats7.csv` | 1C-Pop300/400, 8C-Pop10/100/150 | Corridas **exploratorias** de calibración |
| `ResultadosAlgoritmoGeneticoAIKHomo.csv` | — | Corrida contra el equipo AIKHomo |
| `ResultadosAlgoritmoGeneticoBasicTeam.csv` | — | Corrida contra BasicTeam |
| `ResultadosAlgoritmoGeneticoDoogHomoG_AIKHomo150Gens.csv` | — | Corrida de 150 generaciones |
| `ResultadosAlgoritmoGenetico_FinalNoParalelo.csv` | Secuencial | Línea base sin paralelismo |
| `experimento_final/agregados_publicados.csv` | 5 celdas | Agregados publicados en la tesis, con procedencia |

**Ninguna de las corridas versionadas pertenece al diseño factorial final.** Sus
configuraciones (1 y 8 núcleos; poblaciones 10/100/150/300/400) no coinciden con
el diseño de la tesis (2/4/6/8 núcleos × poblaciones 50/100/500).

---

## El experimento factorial final

Las 12 configuraciones × 3.000 generaciones que sustentan el capítulo 5 se
ejecutaron en un equipo **Apple M1 entre 2024 y 2025** y **sus CSV no se
versionaron**. Se ha verificado que no están en ningún punto del historial de
este repositorio.

Mientras tanto:

- `analisis/verificar_cifras.py` comprueba la consistencia de las cifras
  publicadas (6,3x, iso-población 2x, el tope de fitness) **sin necesidad de
  esos datos**. Funciona hoy.
- `analisis/recompute_speedup.py` y `analisis/figuras.py` los recalculan desde
  cero **en cuanto se depositen** los CSV en `resultados/experimento_final/`,
  sin cambios en el código.

Si los archivos aparecen, basta con copiarlos ahí y ejecutar:

```bash
python3 analisis/recompute_speedup.py
python3 analisis/figuras.py
```
