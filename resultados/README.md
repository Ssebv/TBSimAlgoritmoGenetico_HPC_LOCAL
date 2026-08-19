# Datos de telemetría — diccionario y advertencias

Este directorio contiene los CSV de telemetría del algoritmo genético.

---

## ⚠️ Advertencia 1: `Tiempo (s)` cambia de significado según el formato

**No existe una única convención.** Antes de operar con esta columna hay que
saber de qué corrida procede:

| Formato | `Tiempo (s)` significa | Cómo obtener el tiempo por generación |
|---|---|---|
| `local_stats*.csv` (exploratorias) | Tiempo **acumulado** desde el inicio | `df["Tiempo (s)"].diff()` |
| `stats_*_v6*.csv` (experimento factorial) | Ya es el **tiempo por generación** | usarla tal cual |

Aplicar la convención equivocada produce cifras falsas **en ambos sentidos**:
un `diff()` sobre una serie que ya es por generación descarta la mitad de las
filas (las diferencias negativas) y subestima el tiempo; omitir el `diff()` en
una serie acumulada lo dispara.

`analisis/tbsim_stats.py` **detecta** la convención en lugar de asumirla: si la
serie es monótona creciente aplica la diferencia, y si fluctúa la usa
directamente. Se recomienda usar esos scripts en vez de leer los CSV a mano.

## ⚠️ Advertencia 2: `Fitness Global` no es el mejor histórico

Pese al nombre, la columna **no es acumulativa**: fluctúa entre generaciones
(en 2C-Pop50 desciende en 1.271 de 3.000 generaciones). Es el mejor individuo
*de esa generación*, no el mejor encontrado hasta el momento.

Consecuencia: el **fitness de la última generación es un valor puntual y
ruidoso**. Las figuras publicadas usan esa métrica —de ahí que el mapa de calor
muestre un patrón no monótono, con 2C-Pop100 en 150.000 y 6C-Pop100 en 54.018—.
Todas las configuraciones alcanzan el tope de 150.000 en algún momento.

Para comparar la calidad entre configuraciones son preferibles las métricas
estables que calcula `resumen_por_configuracion()`:

- `fitness_cola_media` — media de las últimas 500 generaciones
- `pct_gen_en_tope` — porcentaje de generaciones que alcanzan 150.000

Con ellas el efecto de la población es limpio y monótono:

| Población | Fitness (cola) | Generaciones en el tope |
|---|---|---|
| 50 | 63.818 | 0,7 % |
| 100 | 78.428 | 1,8 % |
| 500 | **124.801** | **10,2 %** |

---

## Los tres conjuntos del experimento factorial

Durante el trabajo se ejecutó el diseño factorial (12 configuraciones ×
3.000 generaciones) **más de una vez**, con distinta duración de partido:

| Conjunto | 2C-Pop500 | 8C-Pop50 | Estado |
|---|---|---|---|
| **`finalload`** (versionado aquí) | 1,90 s/gen | 0,30 s/gen | **El que generó las figuras de la tesis** |
| `timeext` / tiempo extendido | 4,75 s/gen | 0,30 s/gen | Re-ejecución; no versionada |

Esto explica el histórico **speedup erróneo de 15,8×**: es
`4,75 / 0,30`, es decir, el conjunto de tiempo extendido. **No** se debió a
confundir tiempo acumulado con tiempo por generación, como afirmaban versiones
preliminares de la fe de erratas. El valor correcto, recalculable con
`analisis/recompute_speedup.py`, es **6,4×** (`1,90 / 0,30`).

### Discrepancia conocida

La tesis reporta el perfil de balance **6C-Pop100 = 0,74 s/gen**. Ese valor
procede del conjunto de tiempo extendido; en el conjunto que generó las
figuras, la misma configuración es **0,49 s/gen**. El fitness de esa celda
(54.018) sí coincide en ambos. No afecta al speedup máximo ni a las
conclusiones. `analisis/verificar_cifras.py` lo señala explícitamente.

---

## Esquemas de columnas

Léelos siempre **por nombre de columna, nunca por posición**.

| Esquema | Columnas | Distintivo | Dónde |
|---|---|---|---|
| **v1** | 28 | Incluye `Chromosoma` | Corridas archivadas en este directorio |
| **v2** | 27 | Sin `Chromosoma` | Salida del código actual (`CSVManager`) |
| **v6** | 17 | Formato reducido, sin `Mejor Fitness Generación` | Experimento factorial |

La columna `Chromosoma` se eliminó deliberadamente de la salida (ver
`CSVManager.prepararCSV`). Como consecuencia, **todas las columnas posteriores
a `Goles Contra` están desplazadas una posición entre v1 y v2**: un script que
lea por índice fallará en silencio.

### Columnas principales

| Columna | Descripción |
|---|---|
| `Generación` | Índice de la generación, desde 1 |
| `Fitness Global` | Mejor individuo de la generación (**no** acumulativo — ver advertencia 2) |
| `Tiempo (s)` | Acumulado **o** por generación según el formato (ver advertencia 1) |
| `CPU (%)`, `Memoria (%)` | Uso de recursos |
| `Goles Favor` / `Goles Contra` | Resultado de la simulación |
| `CPUs (configurados)` | Núcleos asignados — **factor experimental** |
| `Population Size` | Tamaño de población — **factor experimental** |
| `Core0 (%)` … `Core7 (%)` | Uso por núcleo |
| `Mejor Fitness Generación`, `Fitness Promedio`, `Diversidad`, `Peor Fitness` | Solo v1/v2 |
| `Chromosoma` | Solo v1 |

---

## Contenido del directorio

| Archivo | Contenido |
|---|---|
| `experimento_final/stats_*_v6_finalload.csv` | **Los 12 CSV del diseño factorial** (2/4/6/8 núcleos × poblaciones 50/100/500), 3.000 generaciones cada uno |
| `experimento_final/agregados_publicados.csv` | Lo que la tesis afirma, con la procedencia de cada valor |
| `experimento_final/agregados_recalculados.csv` | La tabla completa recalculada desde los CSV crudos |
| `local_stats1..7.csv` | Corridas **exploratorias** de calibración (1C-Pop300/400, 8C-Pop10/100/150). No pertenecen al diseño factorial |
| `ResultadosAlgoritmoGenetico*.csv` | Corridas contra equipos rivales concretos y línea base secuencial |

## Reproducir las cifras

```bash
pip install -r analisis/requirements.txt

python3 analisis/verificar_cifras.py      # contrasta lo publicado con los datos crudos
python3 analisis/recompute_speedup.py     # tabla completa + speedup 6,4x
python3 analisis/figuras.py               # regenera las figuras del README
```

Los scripts originales con los que se produjeron las figuras de la tesis se
conservan, sin modificar, en `analisis/originales/`.
