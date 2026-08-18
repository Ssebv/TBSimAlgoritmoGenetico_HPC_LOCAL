#!/usr/bin/env python3
"""
Recalcula la tabla de escalabilidad y el speedup máximo desde los CSV crudos.

Convierte la columna ACUMULADA `Tiempo (s)` en tiempo por generación y agrega
por configuración (núcleos x población), reproduciendo la cifra de 6,3x
reportada en el capítulo 5 de la tesis.

Uso:
    python3 analisis/recompute_speedup.py                       # experimento factorial
    python3 analisis/recompute_speedup.py 'resultados/*.csv'    # otro conjunto
    python3 analisis/recompute_speedup.py --csv salida.csv      # exportar la tabla

Sobre los datos: los CSV del experimento factorial final (12 configuraciones,
3.000 generaciones) no están versionados en este repositorio —ver
`resultados/README.md`. Cuando se depositen en
`resultados/experimento_final/`, este script los toma sin cambios.

Autor: Sebastián Ignacio Allende Cuello — licencia MIT.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from tbsim_stats import (  # noqa: E402
    COL_CORES,
    FormatoCSVError,
    COL_POP,
    cargar_directorio,
    resumen_por_configuracion,
    speedup,
)

PATRON_POR_DEFECTO = "resultados/experimento_final/local_stats*.csv"


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__.split("\n")[1])
    p.add_argument(
        "patron",
        nargs="?",
        default=PATRON_POR_DEFECTO,
        help=f"Glob de CSV a analizar (por defecto: {PATRON_POR_DEFECTO})",
    )
    p.add_argument("--csv", metavar="RUTA", help="Exportar la tabla agregada a un CSV")
    args = p.parse_args()

    print(f"Leyendo: {args.patron}")
    try:
        df = cargar_directorio(args.patron)
    except (FileNotFoundError, FormatoCSVError):
        print(
            f"\nNo se encontraron CSV en '{args.patron}'.\n\n"
            "Los datos del experimento factorial final (12 configuraciones:\n"
            "2/4/6/8 núcleos x poblaciones 50/100/500) no están versionados en\n"
            "este repositorio; se ejecutaron en un equipo Apple M1 en 2024-2025.\n"
            "Ver la sección «Procedencia de los datos» del README.\n\n"
            "Para analizar las corridas que sí están versionadas:\n"
            "    python3 analisis/recompute_speedup.py 'resultados/local_stats*.csv'",
            file=sys.stderr,
        )
        return 2

    print(f"Generaciones cargadas: {len(df)}")
    print(f"Esquema(s) de CSV detectado(s): {sorted(df['esquema'].unique())}")
    print(
        "\nNota: `Tiempo (s)` es ACUMULADO; el tiempo por generación se obtiene\n"
        "por diferencia. La primera generación de cada corrida se descarta\n"
        "(incluye arranque de JVM y carga del simulador).\n"
    )

    resumen = resumen_por_configuracion(df)

    print("=" * 78)
    print(
        f"{'Configuración':>16} {'Gens':>6} {'s/gen medio':>12} "
        f"{'mediana':>9} {'σ':>7} {'Fitness máx':>13}"
    )
    print("=" * 78)
    for _, r in resumen.iterrows():
        media = r["s_por_gen_media"]
        med = r["s_por_gen_mediana"]
        desv = r["s_por_gen_desv"]
        fit = r["fitness_max"]
        print(
            f"{r['config']:>16} {int(r['generaciones']):>6} "
            f"{media:>12.2f} {med:>9.2f} {desv:>7.2f} {fit:>13,.0f}"
            if media == media
            else f"{r['config']:>16} {int(r['generaciones']):>6} {'—':>12}"
        )
    print("=" * 78)

    try:
        s = speedup(resumen)
    except ValueError as e:
        print(f"\nNo se pudo calcular el speedup: {e}", file=sys.stderr)
        return 1

    print(
        f"\nMás lenta : {s['config_lenta']:>12}  {s['t_lenta']:.2f} s/gen"
        f"\nMás rápida: {s['config_rapida']:>12}  {s['t_rapida']:.2f} s/gen"
        f"\n\nSPEEDUP MÁXIMO = {s['speedup']:.2f}x"
    )

    # Un speedup calculado sobre configuraciones que no pertenecen al diseño
    # factorial no es comparable con la cifra de 6,3x de la tesis: mezcla
    # variaciones de población que nada tienen que ver con el paralelismo.
    nucleos_disenno, pobl_disenno = {2, 4, 6, 8}, {50, 100, 500}
    nucleos = set(resumen[COL_CORES].unique())
    pobl = set(resumen[COL_POP].unique())
    if not (nucleos <= nucleos_disenno and pobl <= pobl_disenno):
        print(
            "\n" + "!" * 78 + "\n"
            "ATENCIÓN: este conjunto NO es el diseño factorial de la tesis\n"
            f"  (núcleos esperados {sorted(nucleos_disenno)}, encontrados {sorted(nucleos)};\n"
            f"   poblaciones esperadas {sorted(pobl_disenno)}, encontradas {sorted(pobl)}).\n"
            "Son corridas exploratorias de calibración. El speedup de arriba NO es\n"
            "comparable con el 6,3x reportado en el capítulo 5: compara configuraciones\n"
            "que difieren simultáneamente en núcleos y población.\n" + "!" * 78
        )

    # Speedup iso-población: mismo tamaño de población, distinto número de núcleos.
    print("\nSpeedup iso-población (misma población, 2 núcleos -> máximo núcleos):")
    hay_iso = False
    for pop, grupo in resumen.groupby(COL_POP):
        g = grupo.dropna(subset=["s_por_gen_media"])
        if len(g) < 2:
            continue
        hay_iso = True
        base = g.loc[g[COL_CORES].idxmin()]
        top = g.loc[g[COL_CORES].idxmax()]
        ratio = base["s_por_gen_media"] / top["s_por_gen_media"]
        print(
            f"  Pop {int(pop):>4}: {base['config']} ({base['s_por_gen_media']:.2f} s) "
            f"-> {top['config']} ({top['s_por_gen_media']:.2f} s) = {ratio:.2f}x"
        )
    if not hay_iso:
        print("  (se requiere más de una configuración de núcleos por población)")

    if args.csv:
        resumen.to_csv(args.csv, index=False)
        print(f"\nTabla exportada a: {args.csv}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
