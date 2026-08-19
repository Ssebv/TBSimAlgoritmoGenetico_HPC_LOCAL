#!/usr/bin/env python3
"""
Regenera las figuras del README y del capítulo 5 desde los CSV de telemetría.

Figuras que produce:
  01_curva_aprendizaje.png              Fitness global vs generación
  09_heatmap_fitness_cores_poblacion.png  Mapa de calor fitness x (núcleos, población)
  10_barras_tiempo.png                  Tiempo medio por generación, por configuración

Las figuras 09 y 10 requieren el diseño factorial completo (12 configuraciones);
la 01 funciona con cualquier corrida individual.

Uso:
    python3 analisis/figuras.py                          # factorial completo
    python3 analisis/figuras.py --patron 'resultados/local_stats*.csv'
    python3 analisis/figuras.py --salida /tmp/figuras

Nota sobre los tiempos: la columna `Tiempo (s)` de los CSV es ACUMULADA. Este
script deriva el tiempo por generación mediante `tbsim_stats`, que aplica la
diferencia. Ver resultados/README.md.

Autor: Sebastián Ignacio Allende Cuello — licencia MIT.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # backend sin ventana: funciona en CI y por SSH
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402

sys.path.insert(0, str(Path(__file__).resolve().parent))

from tbsim_stats import (  # noqa: E402
    COL_CORES,
    FormatoCSVError,
    COL_FITNESS_GLOBAL,
    COL_GEN,
    COL_POP,
    cargar_directorio,
    resumen_por_configuracion,
)

PATRON_POR_DEFECTO = "resultados/experimento_final/stats_*.csv"
NUCLEOS_DISENNO = [2, 4, 6, 8]
POBLACIONES_DISENNO = [50, 100, 500]


def fig01_curva_aprendizaje(df, salida: Path) -> None:
    fig, ax = plt.subplots(figsize=(11, 6))
    for cfg, grupo in df.groupby("config"):
        g = grupo.sort_values(COL_GEN)
        ax.plot(g[COL_GEN], g[COL_FITNESS_GLOBAL], label=cfg, alpha=0.85, linewidth=1.4)
    ax.set_xlabel("Generación")
    ax.set_ylabel("Fitness global (mejor histórico)")
    ax.set_title("Curva de aprendizaje: evolución del fitness global")
    ax.grid(True, alpha=0.3)
    if df["config"].nunique() <= 12:
        ax.legend(fontsize=8, ncol=2)
    fig.tight_layout()
    destino = salida / "01_curva_aprendizaje.png"
    fig.savefig(destino, dpi=150)
    plt.close(fig)
    print(f"  escrito: {destino}")


def fig09_heatmap(resumen, salida: Path) -> None:
    # Los ejes se derivan de los datos: con el factorial completo coinciden con
    # el diseño (2/4/6/8 x 50/100/500); con otros conjuntos el mapa sigue siendo
    # legible en lugar de quedar vacío.
    nucleos = sorted({int(c) for c in resumen[COL_CORES]})
    poblaciones = sorted({int(p) for p in resumen[COL_POP]})

    matriz = np.full((len(nucleos), len(poblaciones)), np.nan)
    for _, r in resumen.iterrows():
        i = nucleos.index(int(r[COL_CORES]))
        j = poblaciones.index(int(r[COL_POP]))
        matriz[i, j] = r["fitness_final"]

    maximo = np.nanmax(matriz) if not np.all(np.isnan(matriz)) else 1.0

    fig, ax = plt.subplots(figsize=(7, 6))
    im = ax.imshow(matriz, cmap="viridis", aspect="auto")
    ax.set_xticks(range(len(poblaciones)), [str(p) for p in poblaciones])
    ax.set_yticks(range(len(nucleos)), [str(c) for c in nucleos])
    ax.set_xlabel("Tamaño de población")
    ax.set_ylabel("Núcleos configurados")
    ax.set_title("Fitness final por configuración")
    for i in range(len(nucleos)):
        for j in range(len(poblaciones)):
            v = matriz[i, j]
            if np.isnan(v):
                ax.text(j, i, "sin dato", ha="center", va="center",
                        color="#999999", fontsize=8, style="italic")
            else:
                ax.text(j, i, f"{v:,.0f}", ha="center", va="center",
                        color="white" if v < maximo * 0.6 else "black", fontsize=9)
    fig.colorbar(im, ax=ax, label="Fitness final")
    fig.tight_layout()
    destino = salida / "09_heatmap_fitness_cores_poblacion.png"
    fig.savefig(destino, dpi=150)
    plt.close(fig)
    print(f"  escrito: {destino}")


def fig10_barras_tiempo(resumen, salida: Path) -> None:
    datos = resumen.dropna(subset=["s_por_gen_media"]).sort_values("s_por_gen_media")
    fig, ax = plt.subplots(figsize=(11, 6))
    barras = ax.bar(datos["config"], datos["s_por_gen_media"],
                    yerr=datos["s_por_gen_desv"], capsize=3, color="#3b76af")
    ax.set_xlabel("Configuración (núcleos-población)")
    ax.set_ylabel("Tiempo medio por generación (s)")
    ax.set_title("Tiempo medio por generación según configuración")
    ax.grid(True, axis="y", alpha=0.3)
    plt.setp(ax.get_xticklabels(), rotation=45, ha="right")
    # La etiqueta se sitúa por encima de la barra de error para no solaparla.
    for b, v, e in zip(barras, datos["s_por_gen_media"], datos["s_por_gen_desv"].fillna(0)):
        ax.text(b.get_x() + b.get_width() / 2, v + e, f"{v:.2f}",
                ha="center", va="bottom", fontsize=8)
    fig.tight_layout()
    destino = salida / "10_barras_tiempo.png"
    fig.savefig(destino, dpi=150)
    plt.close(fig)
    print(f"  escrito: {destino}")


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__.split("\n")[1])
    p.add_argument("--patron", default=PATRON_POR_DEFECTO,
                   help=f"Glob de CSV a procesar (por defecto: {PATRON_POR_DEFECTO})")
    p.add_argument("--salida", default="img/resultados",
                   help="Directorio donde escribir los PNG (por defecto: img/resultados)")
    args = p.parse_args()

    try:
        df = cargar_directorio(args.patron)
    except (FileNotFoundError, FormatoCSVError):
        print(
            f"No se encontraron CSV en '{args.patron}'.\n\n"
            "Las figuras 09 y 10 requieren el experimento factorial completo\n"
            "(12 configuraciones), cuyos CSV no están versionados en este\n"
            "repositorio —ver «Procedencia de los datos» en el README.\n\n"
            "Para generar la curva de aprendizaje con las corridas versionadas:\n"
            "    python3 analisis/figuras.py --patron 'resultados/local_stats*.csv'",
            file=sys.stderr,
        )
        return 2

    salida = Path(args.salida)
    salida.mkdir(parents=True, exist_ok=True)
    print(f"Corridas cargadas: {df['origen'].nunique()} archivo(s), {len(df)} generaciones")
    print(f"Configuraciones: {sorted(df['config'].unique())}")
    print(f"Directorio de salida: {salida}\n")

    fig01_curva_aprendizaje(df, salida)

    resumen = resumen_por_configuracion(df)
    completo = (set(resumen[COL_CORES]) == set(NUCLEOS_DISENNO)
                and set(resumen[COL_POP]) == set(POBLACIONES_DISENNO))
    if not completo:
        print(
            "\n  aviso: el conjunto cargado no cubre el diseño factorial completo\n"
            f"         (núcleos {sorted(set(int(c) for c in resumen[COL_CORES]))}, "
            f"poblaciones {sorted(set(int(x) for x in resumen[COL_POP]))}).\n"
            "         Las figuras 09 y 10 se generan igualmente, con las celdas\n"
            "         disponibles; no son las publicadas en la tesis."
        )
    fig09_heatmap(resumen, salida)
    fig10_barras_tiempo(resumen, salida)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
