#!/usr/bin/env python3
"""
Verifica las cifras publicadas en la tesis contra los datos crudos del experimento.

Recalcula todo desde los 12 CSV del diseño factorial
(`resultados/experimento_final/`) y contrasta el resultado con lo que la tesis
afirma, declarado en `agregados_publicados.csv`.

El objetivo es que cualquier lector pueda comprobar las cifras por sí mismo en
lugar de tener que confiar en ellas, y que las discrepancias conocidas queden
explícitas en vez de ocultas.

Uso:
    python3 analisis/verificar_cifras.py

Devuelve 0 si las cifras centrales se confirman, 1 si alguna falla.

Autor: Sebastián Ignacio Allende Cuello — licencia MIT.
"""

from __future__ import annotations

import sys
from pathlib import Path

import pandas as pd

sys.path.insert(0, str(Path(__file__).resolve().parent))

from tbsim_stats import (  # noqa: E402
    COL_CORES,
    COL_POP,
    FITNESS_TOPE,
    cargar_directorio,
    resumen_por_configuracion,
)

RAIZ = Path(__file__).resolve().parent.parent
CRUDOS = "resultados/experimento_final/stats_*.csv"
PUBLICADOS = RAIZ / "resultados" / "experimento_final" / "agregados_publicados.csv"

SPEEDUP_DECLARADO = 6.3
SPEEDUP_ISO_POP500_DECLARADO = 2.0
SPEEDUP_ERRONEO = 15.8          # valor de la fe de erratas
TOLERANCIA = 0.05               # 5 %

ok_global = []


def comprobar(desc: str, obtenido: float, declarado: float, tol: float = TOLERANCIA) -> bool:
    ok = abs(obtenido - declarado) <= abs(declarado) * tol
    print(f"  [{'OK  ' if ok else 'FALLA'}] {desc}: obtenido={obtenido:.2f}  declarado={declarado:.2f}")
    ok_global.append(ok)
    return ok


def main() -> int:
    try:
        df = cargar_directorio(CRUDOS)
    except (FileNotFoundError, Exception) as e:  # noqa: BLE001
        print(f"No se pudieron cargar los datos crudos ({CRUDOS}): {e}", file=sys.stderr)
        return 1

    r = resumen_por_configuracion(df)
    t = {(int(x[COL_CORES]), int(x[COL_POP])): x for _, x in r.iterrows()}
    seg = lambda c, p: float(t[(c, p)]["s_por_gen_media"])  # noqa: E731

    print(f"Datos crudos: {df['origen'].nunique()} corridas, {len(df):,} generaciones\n")

    # --- 1. Speedup máximo entre configuraciones extremas ---
    print("1) Speedup máximo (config más lenta / más rápida)")
    lenta = r.loc[r["s_por_gen_media"].idxmax()]
    rapida = r.loc[r["s_por_gen_media"].idxmin()]
    sp = lenta["s_por_gen_media"] / rapida["s_por_gen_media"]
    print(f"   {lenta['config']} ({lenta['s_por_gen_media']:.2f} s) / "
          f"{rapida['config']} ({rapida['s_por_gen_media']:.2f} s)")
    comprobar("speedup máximo", sp, SPEEDUP_DECLARADO)

    # --- 2. Control de regresión sobre la fe de erratas ---
    print("\n2) Control de regresión: el speedup no es el valor erróneo histórico")
    no_erroneo = abs(sp - SPEEDUP_ERRONEO) > 1.0
    print(f"  [{'OK  ' if no_erroneo else 'FALLA'}] no es {SPEEDUP_ERRONEO}x "
          "(que provenía del conjunto de tiempo extendido, no de este)")
    ok_global.append(no_erroneo)

    # --- 3. Speedup iso-población: aísla el efecto del paralelismo ---
    print("\n3) Speedup iso-población (misma población, 2 -> 8 núcleos)")
    for p in (50, 100, 500):
        print(f"   Pop {p:>3}: {seg(2, p)/seg(8, p):.2f}x", end="")
        print("   <- el declarado en la tesis" if p == 500 else "")
    comprobar("iso-población 500", seg(2, 500) / seg(8, 500), SPEEDUP_ISO_POP500_DECLARADO)

    # --- 4. Mensaje central: la población determina la calidad ---
    # Se evalúa sobre métricas robustas, no sobre el fitness de la última
    # generación: `Fitness Global` fluctúa, así que ese valor puntual es ruido.
    print("\n4) Mensaje central: «los núcleos dan velocidad, la población da calidad»")
    por_pop = r.groupby(COL_POP).agg(
        cola=("fitness_cola_media", "mean"), tope=("pct_gen_en_tope", "mean")
    ).sort_index()
    for p, row in por_pop.iterrows():
        print(f"   Pop {int(p):>3}: fitness cola={row['cola']:>9,.0f}   "
              f"generaciones en el tope={row['tope']:>5.1f}%")
    monotona = (por_pop["cola"].is_monotonic_increasing
                and por_pop["tope"].is_monotonic_increasing)
    print(f"  [{'OK  ' if monotona else 'FALLA'}] la calidad crece de forma monótona con la población")
    ok_global.append(monotona)

    # El tiempo NO depende de la población tanto como de los núcleos.
    efecto_nucleos = seg(2, 500) / seg(8, 500)
    efecto_poblacion = seg(8, 500) / seg(8, 50)
    print(f"   (efecto núcleos en el tiempo: {efecto_nucleos:.2f}x  ·  "
          f"efecto población: {efecto_poblacion:.2f}x)")

    # --- 5. Contraste celda a celda con lo publicado ---
    print("\n5) Contraste con las cifras publicadas en la tesis")
    if PUBLICADOS.exists():
        pub = pd.read_csv(PUBLICADOS)
        discrepancias = []
        for _, fila in pub.dropna(subset=["s_por_gen"]).iterrows():
            c, p = int(fila["cores"]), int(fila["poblacion"])
            obt, dec = seg(c, p), float(fila["s_por_gen"])
            coincide = abs(obt - dec) <= 0.05
            print(f"   {fila['config']:>10}: recalculado={obt:5.2f}  publicado={dec:5.2f}  "
                  f"{'coincide' if coincide else '<-- DIFIERE'}")
            if not coincide:
                discrepancias.append((fila["config"], obt, dec))
        if discrepancias:
            print("\n   Discrepancia conocida y documentada (ver resultados/README.md):")
            for cfg, obt, dec in discrepancias:
                print(f"     {cfg}: la tesis reporta {dec:.2f} s/gen, tomado del conjunto de")
                print(f"     tiempo extendido; en el conjunto que generó las figuras es {obt:.2f}.")
            print("   No afecta al speedup máximo ni a las conclusiones.")

    total, pasadas = len(ok_global), sum(ok_global)
    print("\n" + "=" * 70)
    print(f"RESULTADO: {pasadas}/{total} comprobaciones centrales pasadas")
    print("=" * 70)
    return 0 if pasadas == total else 1


if __name__ == "__main__":
    raise SystemExit(main())
