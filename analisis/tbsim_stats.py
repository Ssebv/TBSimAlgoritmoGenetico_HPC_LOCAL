"""
tbsim_stats — carga y normalización de los CSV de telemetría del experimento.

Este módulo centraliza dos cosas que, hechas a mano, han producido errores en el
pasado:

1. **La columna `Tiempo (s)` es ACUMULADA, no por generación.**
   Interpretarla como tiempo por generación fue el origen de un speedup
   erróneo de 15,8x reportado en versiones preliminares de este trabajo. El
   valor correcto es 6,3x. Usar siempre `tiempo_por_generacion()`, que aplica
   `diff()` sobre la serie acumulada.

2. **Existen dos esquemas de CSV.** Las corridas archivadas (esquema v1) tienen
   28 columnas e incluyen `Chromosoma`; el código actual emite 27 columnas sin
   esa columna (ver `CSVManager.prepararCSV`). Todo acceso aquí es **por nombre
   de columna**, nunca por posición, de modo que ambos esquemas funcionan.

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

_REQUERIDAS = [COL_GEN, COL_TIEMPO_ACUM, COL_CORES, COL_POP]


class FormatoCSVError(ValueError):
    """El CSV no tiene el formato de telemetría esperado."""


def detectar_esquema(df: pd.DataFrame) -> str:
    """Devuelve 'v1' (con Chromosoma, 28 col) o 'v2' (sin Chromosoma, 27 col)."""
    return "v1" if COL_CROMOSOMA in df.columns else "v2"


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

    # --- La corrección central: acumulado -> por generación ---
    df["tiempo_gen"] = df[COL_TIEMPO_ACUM].diff()

    # Un acumulado que decrece indica reinicio desde checkpoint o CSV concatenado.
    if (df["tiempo_gen"] < 0).any():
        n = int((df["tiempo_gen"] < 0).sum())
        print(
            f"  aviso: {os.path.basename(ruta)}: {n} salto(s) negativo(s) en el "
            "tiempo acumulado (¿reanudación desde checkpoint?). Se descartan."
        )
        df.loc[df["tiempo_gen"] < 0, "tiempo_gen"] = pd.NA

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


def resumen_por_configuracion(df: pd.DataFrame) -> pd.DataFrame:
    """Agrega por (núcleos, población): tiempo medio/mediano por generación y fitness."""
    agrupado = df.groupby([COL_CORES, COL_POP], as_index=False).agg(
        generaciones=(COL_GEN, "count"),
        s_por_gen_media=("tiempo_gen", "mean"),
        s_por_gen_mediana=("tiempo_gen", "median"),
        s_por_gen_desv=("tiempo_gen", "std"),
        fitness_max=(COL_FITNESS_GLOBAL, "max"),
        fitness_final=(COL_FITNESS_GLOBAL, "last"),
    )
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
