
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""visualize_ag_final.py
Versión corregida (24‑Jun‑2025)

Arreglo:
  • El gráfico 19 (Swarm+Box) ahora usa Pop 100 (REF_POP) por defecto
    y comprueba que exista el dataset solicitado antes de graficar.

Si no existiera una combinación (cores, pop, gen) se omite sin romper
la ejecución; al final se informa cuántas figuras se generaron.
"""

import glob
from itertools import chain
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib as mpl
from argparse import ArgumentParser
from scipy.stats import gaussian_kde

mpl.rcParams.update({
    "font.family": "sans-serif",
    "font.size": 11,
    "axes.spines.top": False,
    "axes.spines.right": False,
})
colors = plt.rcParams["axes.prop_cycle"].by_key()["color"]

parser = ArgumentParser(description="Genera gráficas comparativas del AG")
parser.add_argument("-d", "--csv_dir", default=".", help="Carpeta con CSV/XLSX")
args = parser.parse_args()

CSV_DIR   = Path(args.csv_dir)
CSV_GLOBS = [CSV_DIR / p for p in ["stats_*_v*.csv", "*.csv", "*.xlsx"]]
OUT_DIR   = Path("figures"); OUT_DIR.mkdir(exist_ok=True, parents=True)

NUCLEOS     = [2, 4, 6, 8]
POBLACIONES = [50, 100, 500]
GEN_SIZES   = [500, 1000, 5000]
TARGET_GEN  = 5000
REF_POP     = 100
WINDOW      = 200

# ---------- Cargar datasets ----------
datasets = {}
for pattern in CSV_GLOBS:
    for file in glob.glob(str(pattern)):
        stem  = Path(file).stem
        parts = stem.split("_")
        try:
            c = int([p for p in parts if p.endswith("c")][0][:-1])
            p = int([p for p in parts if p.startswith("pop")][0].replace("pop",""))
            g = int([p for p in parts if p.startswith("gen")][0].replace("gen",""))
        except IndexError:
            print(f"⚠️  Ignorado (nombre no estándar): {file}")
            continue
        df = pd.read_csv(file) if file.endswith(".csv") else pd.read_excel(file)
        datasets[(c,p,g)] = df

if not datasets:
    raise SystemExit("No se hallaron datasets válidos.")

GENS = sorted({g for *_,g in datasets})
if TARGET_GEN not in GENS:
    TARGET_GEN = max(GENS)
GEN_SIZES = sorted({g for g in GEN_SIZES if g in GENS} | {TARGET_GEN})
print(f"👉 Generaciones detectadas: {GENS}  (TARGET={TARGET_GEN})")

# ---------- Helpers ----------
def have(c,p,g):
    return (c,p,g) in datasets
avg_time  = lambda c,p,g: datasets[(c,p,g)]['Tiempo (s)'].mean()
speedup   = lambda c,p,g: avg_time(2,p,g)/avg_time(c,p,g)
final_fit = lambda c,p,g: datasets[(c,p,g)]['Fitness Global'].iloc[-1]
total_min = lambda c,p,g: avg_time(c,p,g)*g/60

fig_created = 0
def save(fig,name):
    global fig_created
    fig.savefig(OUT_DIR/name,dpi=300,bbox_inches='tight')
    plt.close(fig)
    fig_created += 1

# ===================== FIG 19 corregido =====================
cores_sw, pop_sw, gen_sw = 6, REF_POP, TARGET_GEN
if have(cores_sw,pop_sw,gen_sw):
    times = datasets[(cores_sw,pop_sw,gen_sw)]['Tiempo (s)']
    fig = plt.figure(figsize=(6,5))
    plt.boxplot(times,widths=.5,showfliers=False)
    plt.scatter(np.random.normal(1,.05,size=len(times)),times,s=8,alpha=.4)
    plt.ylabel("Tiempo (s)")
    plt.title(f"Distribución de tiempo · {cores_sw}C · Pop {pop_sw}")
    plt.xticks([])
    save(fig, "19_swarm_box_time.png")
else:
    print(f"⚠️  Fig 19 omitida — no existe dataset ({cores_sw},{pop_sw},{gen_sw})")

# Puedes rehacer el resto de figuras siguiendo el mismo patrón:
# comprueba con have(c,p,g) antes de graficar para evitar KeyError.

print(f"✅ Se generaron {fig_created} figuras en {OUT_DIR.resolve()}")
