#!/usr/bin/env python3
"""
Verifica las cifras titulares de la tesis a partir de los agregados publicados.

A diferencia de `recompute_speedup.py` —que necesita los CSV crudos del
experimento factorial, no versionados— este script se ejecuta **hoy**, sin
datos adicionales, usando `resultados/experimento_final/agregados_publicados.csv`.
Cada valor de ese archivo lleva su procedencia (capítulo de la tesis).

Su objetivo es que cualquier lector pueda comprobar por sí mismo que las cifras
declaradas son consistentes entre sí, en lugar de tener que confiar en ellas.

Uso:
    python3 analisis/verificar_cifras.py

Devuelve 0 si todas las comprobaciones pasan, 1 si alguna falla.

Autor: Sebastián Ignacio Allende Cuello — licencia MIT.
"""

from __future__ import annotations

import sys
from pathlib import Path

import pandas as pd

RAIZ = Path(__file__).resolve().parent.parent
AGREGADOS = RAIZ / "resultados" / "experimento_final" / "agregados_publicados.csv"

# Cifras declaradas en el resumen y el capítulo 5 de la tesis.
SPEEDUP_DECLARADO = 6.3
SPEEDUP_ISO_POP500_DECLARADO = 2.0
FITNESS_MAXIMO = 150_000

# Valor erróneo publicado en versiones preliminares; existe fe de erratas.
SPEEDUP_ERRONEO = 15.8

TOLERANCIA = 0.05  # 5 %


def comprobar(descripcion: str, obtenido: float, esperado: float,
              tol: float = TOLERANCIA) -> bool:
    ok = abs(obtenido - esperado) <= abs(esperado) * tol
    marca = "OK  " if ok else "FALLA"
    print(f"  [{marca}] {descripcion}\n           obtenido={obtenido:.2f}  declarado={esperado:.2f}")
    return ok


def main() -> int:
    if not AGREGADOS.exists():
        print(f"No se encontró {AGREGADOS}", file=sys.stderr)
        return 1

    df = pd.read_csv(AGREGADOS)
    print(f"Agregados publicados: {AGREGADOS.relative_to(RAIZ)}")
    print(f"Configuraciones con tiempo publicado: {df['s_por_gen'].notna().sum()} de 12\n")

    con_tiempo = df.dropna(subset=["s_por_gen"])
    resultados = []

    # 1. Speedup máximo = config más lenta / config más rápida.
    lenta = con_tiempo.loc[con_tiempo["s_por_gen"].idxmax()]
    rapida = con_tiempo.loc[con_tiempo["s_por_gen"].idxmin()]
    speedup = lenta["s_por_gen"] / rapida["s_por_gen"]
    print(f"1) Speedup máximo — {lenta['config']} ({lenta['s_por_gen']} s/gen) "
          f"vs {rapida['config']} ({rapida['s_por_gen']} s/gen)")
    resultados.append(comprobar("speedup máximo", speedup, SPEEDUP_DECLARADO))

    # 2. El speedup NO es 15,8x (error de versiones preliminares: interpretar la
    #    columna acumulada `Tiempo (s)` como si fuera tiempo por generación).
    print("\n2) Control de regresión sobre la fe de erratas")
    no_es_erroneo = abs(speedup - SPEEDUP_ERRONEO) > 1.0
    print(f"  [{'OK  ' if no_es_erroneo else 'FALLA'}] el speedup no es {SPEEDUP_ERRONEO}x "
          f"(valor erróneo de versiones preliminares)")
    resultados.append(no_es_erroneo)

    # 3. Speedup iso-población a 500 individuos: aísla el efecto del paralelismo.
    pop500 = con_tiempo[con_tiempo["poblacion"] == 500]
    print("\n3) Speedup iso-población (500 individuos, 2 -> 8 núcleos)")
    if len(pop500) >= 2:
        base = pop500.loc[pop500["cores"].idxmin()]
        top = pop500.loc[pop500["cores"].idxmax()]
        iso = base["s_por_gen"] / top["s_por_gen"]
        print(f"   {base['config']} ({base['s_por_gen']} s) -> {top['config']} ({top['s_por_gen']} s)")
        resultados.append(comprobar("speedup iso-población", iso, SPEEDUP_ISO_POP500_DECLARADO))
    else:
        print("  [OMIT] faltan configuraciones de población 500")

    # 4. Coherencia del mensaje central: "los núcleos dan velocidad, la
    #    población da calidad". Solo las poblaciones grandes llegan al tope.
    print("\n4) Mensaje central: la población, no los núcleos, determina la calidad")
    alcanzan_max = {int(p) for p in df[df["fitness_max"] >= FITNESS_MAXIMO]["poblacion"]}
    no_alcanzan = {int(p) for p in df[df["fitness_max"] < FITNESS_MAXIMO]["poblacion"]}
    ok_calidad = alcanzan_max == {500} and 500 not in no_alcanzan
    print(f"  [{'OK  ' if ok_calidad else 'FALLA'}] alcanzan {FITNESS_MAXIMO:,} puntos "
          f"solo las poblaciones {sorted(alcanzan_max)}; "
          f"no lo alcanzan {sorted(no_alcanzan)}")
    resultados.append(ok_calidad)

    # 5. El tiempo crece con la población a núcleos comparables.
    print("\n5) Coherencia interna: a más población, más tiempo por generación")
    ocho = con_tiempo[con_tiempo["cores"] == 8].sort_values("poblacion")
    if len(ocho) >= 2:
        creciente = ocho["s_por_gen"].is_monotonic_increasing
        print(f"  [{'OK  ' if creciente else 'FALLA'}] con 8 núcleos: "
              + " < ".join(f"{r['config']} ({r['s_por_gen']}s)" for _, r in ocho.iterrows()))
        resultados.append(creciente)

    total, pasadas = len(resultados), sum(resultados)
    print("\n" + "=" * 70)
    print(f"RESULTADO: {pasadas}/{total} comprobaciones pasadas")
    print("=" * 70)
    if pasadas < total:
        return 1
    print(
        "\nNota: esto verifica la CONSISTENCIA de las cifras publicadas entre sí.\n"
        "La verificación desde datos crudos requiere los CSV del experimento\n"
        "factorial (ver resultados/README.md) y se hace con recompute_speedup.py."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
