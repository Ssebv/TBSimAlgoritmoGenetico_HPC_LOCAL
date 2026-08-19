"""
tbsim_stats — carga y normalización de los CSV de telemetría del experimento.

Este módulo centraliza dos cosas que, hechas a mano, han producido errores en el
pasado:

1. **La columna `Tiempo (s)` cambia de significado según el formato.**
   - En los CSV antiguos (`local_stats*.csv`, corridas exploratorias) es
     **acumulada**: el tiempo total transcurrido desde el inicio.
   - En los CSV del experimento factorial (`stats_*_v6*.csv`) ya es el
     **tiempo por generación**.

   Este módulo lo **detecta**, no lo asume: si la serie es monótona creciente
   se aplica `diff()`; si fluctúa, se toma tal cual. Asumir lo uno o lo otro
   produce cifras de speedup falsas en ambos sentidos.

2. **Existen tres esquemas de CSV.** Las corridas archivadas (esquema v1)
   tienen 28 columnas e incluyen `Chromosoma`; el código actual emite 27 sin
   esa columna (ver `CSVManager.prepararCSV`); el experimento factorial usó un
   formato reducido de 17 columnas (v6). Todo acceso aquí es **por nombre de
   columna**, nunca por posición, de modo que los tres funcionan.

Uso:
    from tbsim_stats import cargar_corrida, cargar_directorio
    df = cargar_corrida("resultados/local_stats5.csv")

Autor: Sebastián Ignacio Allende Cuello — licencia MIT.
"""

from __future__ import annotations

import glob
import os

import pandas as pd

# Nombres canónicos de las columnas de interés, tal como los emite CSVManager.java.
COL_GEN = "Generación"
COL_TIEMPO_ACUM = "Tiempo (s)"
COL_FITNESS_GLOBAL = "Fitness Global"
COL_FITNESS_GEN = "Mejor Fitness Generación"
COL_CORES = "CPUs (configurados)"
COL_POP = "Population Size"
COL_CROMOSOMA = "Chromosoma"
COL_GOLES_FAVOR = "Goles Favor"

_REQUERIDAS = [COL_GEN, COL_TIEMPO_ACUM, COL_CORES, COL_POP]


class FormatoCSVError(ValueError):
    """El CSV no tiene el formato de telemetría esperado."""


def detectar_esquema(df: pd.DataFrame) -> str:
    """Devuelve 'v1' (con Chromosoma), 'v6' (formato reducido) o 'v2'."""
    if COL_CROMOSOMA in df.columns:
        return "v1"
    if COL_FITNESS_GEN not in df.columns:
        return "v6"
    return "v2"


def tiempo_es_acumulado(serie: pd.Series, tolerancia: float = 0.05) -> bool:
    """Determina si `Tiempo (s)` es acumulada o ya viene por generación.

    Una serie acumulada es monótona creciente salvo reanudaciones desde
    checkpoint. Una serie por generación fluctúa: aproximadamente la mitad de
    sus diferencias consecutivas son negativas.

    Se decide por la proporción de diferencias negativas: por debajo de
    `tolerancia` se considera acumulada.
    """
    dif = serie.diff().dropna()
    if len(dif) == 0:
        return True
    return (dif < 0).mean() <= tolerancia


def cargar_corrida(ruta: str) -> pd.DataFrame:
    """Carga un `local_stats*.csv` y añade columnas derivadas.

    Columnas añadidas:
      - `tiempo_gen`: segundos por generación (diff del acumulado).
      - `config`: etiqueta 'NC-PopM' de la configuración.
      - `esquema`: 'v1' o 'v2'.

    La primera generación queda con `tiempo_gen = NaN` de forma deliberada: su
    valor acumulado incluye el arranque de la JVM y la carga del simulador, y no
    es comparable con el resto. Las agregaciones de pandas la excluyen sola.
    """
    df = pd.read_csv(ruta)

    faltantes = [c for c in _REQUERIDAS if c not in df.columns]
    if faltantes:
        raise FormatoCSVError(
            f"{ruta}: faltan columnas {faltantes}.\n"
            f"Columnas encontradas: {list(df.columns)}\n"
            "¿Es un CSV de telemetría de este experimento? Ver resultados/README.md."
        )

    df = df.sort_values(COL_GEN).reset_index(drop=True)

    # --- La corrección central: detectar la convención de la columna ---
    if tiempo_es_acumulado(df[COL_TIEMPO_ACUM]):
        df["tiempo_acumulado"] = True
        df["tiempo_gen"] = df[COL_TIEMPO_ACUM].diff()
        # Un acumulado que decrece indica reinicio desde checkpoint.
        if (df["tiempo_gen"] < 0).any():
            n = int((df["tiempo_gen"] < 0).sum())
            print(
                f"  aviso: {os.path.basename(ruta)}: {n} salto(s) negativo(s) en "
                "el tiempo acumulado (¿reanudación desde checkpoint?). Se descartan."
            )
            df.loc[df["tiempo_gen"] < 0, "tiempo_gen"] = pd.NA
    else:
        # Ya viene por generación: usarla tal cual sería un error aplicar diff().
        df["tiempo_acumulado"] = False
        df["tiempo_gen"] = df[COL_TIEMPO_ACUM]

    df["esquema"] = detectar_esquema(df)
    df["config"] = (
        df[COL_CORES].astype(str) + "C-Pop" + df[COL_POP].astype(str)
    )
    df["origen"] = os.path.basename(ruta)
    return df


def cargar_directorio(patron: str) -> pd.DataFrame:
    """Carga y concatena todos los CSV que casen con `patron` (glob).

    Lanza FileNotFoundError si no hay coincidencias, para que los scripts que
    dependen de datos ausentes fallen con un mensaje claro en vez de producir
    una tabla vacía.
    """
    rutas = sorted(glob.glob(patron))
    if not rutas:
        raise FileNotFoundError(f"Ningún archivo coincide con: {patron}")

    marcos = []
    for r in rutas:
        try:
            marcos.append(cargar_corrida(r))
        except FormatoCSVError as e:
            print(f"  omitido: {e.args[0].splitlines()[0]}")
    if not marcos:
        raise FormatoCSVError(f"Ningún archivo de {patron} tiene formato válido.")
    return pd.concat(marcos, ignore_index=True)


# Tope de aptitud impuesto por la función de evaluación ("capping").
FITNESS_TOPE = 150_000


def resumen_por_configuracion(df: pd.DataFrame) -> pd.DataFrame:
    """Agrega por (núcleos, población): tiempo por generación y calidad.

    Sobre las métricas de fitness: pese a su nombre, `Fitness Global` NO es el
    mejor histórico acumulado —fluctúa de una generación a otra—, de modo que
    `fitness_final` (el valor de la última generación) es un snapshot ruidoso.
    Las figuras publicadas usan esa métrica, y por eso muestran un patrón no
    monótono. Para comparar la calidad entre configuraciones son preferibles
    `fitness_cola_media` y `pct_gen_en_tope`, que son estables.
    """
    agrupado = df.groupby([COL_CORES, COL_POP], as_index=False).agg(
        generaciones=(COL_GEN, "count"),
        s_por_gen_media=("tiempo_gen", "mean"),
        s_por_gen_mediana=("tiempo_gen", "median"),
        s_por_gen_desv=("tiempo_gen", "std"),
        fitness_max=(COL_FITNESS_GLOBAL, "max"),
        fitness_final=(COL_FITNESS_GLOBAL, "last"),
        fitness_mediana=(COL_FITNESS_GLOBAL, "median"),
    )

    # Métricas robustas: media de la cola final y proporción de generaciones
    # que alcanzan el tope. Se calculan aparte porque necesitan la serie entera.
    robustas = []
    for (c, p), g in df.groupby([COL_CORES, COL_POP]):
        fg = g.sort_values(COL_GEN)[COL_FITNESS_GLOBAL]
        robustas.append({
            COL_CORES: c,
            COL_POP: p,
            "fitness_cola_media": fg.tail(500).mean(),
            "pct_gen_en_tope": 100.0 * (fg >= FITNESS_TOPE).mean(),
        })
    agrupado = agrupado.merge(pd.DataFrame(robustas), on=[COL_CORES, COL_POP])
    agrupado["config"] = (
        agrupado[COL_CORES].astype(str) + "C-Pop" + agrupado[COL_POP].astype(str)
    )
    return agrupado.sort_values([COL_CORES, COL_POP]).reset_index(drop=True)


def speedup(resumen: pd.DataFrame, columna: str = "s_por_gen_media") -> dict:
    """Calcula el speedup máximo (config más lenta / config más rápida)."""
    validos = resumen.dropna(subset=[columna])
    if validos.empty:
        raise ValueError("No hay tiempos por generación válidos para calcular speedup.")
    lenta = validos.loc[validos[columna].idxmax()]
    rapida = validos.loc[validos[columna].idxmin()]
    return {
        "config_lenta": lenta["config"],
        "t_lenta": float(lenta[columna]),
        "config_rapida": rapida["config"],
        "t_rapida": float(rapida[columna]),
        "speedup": float(lenta[columna] / rapida[columna]),
    }
