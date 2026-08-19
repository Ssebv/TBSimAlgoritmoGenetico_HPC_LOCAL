
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""visualize_ag.py
Genera 20 figuras comparativas del Algoritmo Genético a partir de los CSV.
Las figuras se guardan en ./figures.

Añadidos respecto a la versión original:
14. Curva de aprendizaje (fitness medio por generación)
15. Ribbon GF/GC
16. Evolución del uso de CPU con banda ±1σ
17. Heat‑map de correlación
18. Ridgeline de %CPU por núcleo (simplificada a subplots KDE)
19. Swarm+Box de tiempo por generación
20. Violin de diferencias de gol por población
21. Convergencia vs Carga (fitness & CPU)
22. Heat‑map (cores × población) de fitness final
23. Extra: histograma de tiempo medio por generación

Requiere: numpy, pandas, matplotlib, scipy (solo para KDE ridgeline opcional).

Ejemplo de uso
--------------
python visualise_ag.py -d ./csv
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

# ---------- Estilo global ----------
mpl.rcParams.update({
    "font.family": "sans-serif",
    "font.size": 11,
    "axes.spines.top": False,
    "axes.spines.right": False,
})
colors = plt.rcParams["axes.prop_cycle"].by_key()["color"]

# ---------- CLI ----------
parser = ArgumentParser(description="Genera gráficas comparativas del AG")
parser.add_argument("-d", "--csv_dir", default=".", help="Carpeta con CSV/XLSX")
args = parser.parse_args()

CSV_DIR = Path(args.csv_dir)
CSV_PATTERN = [str(CSV_DIR / "stats_*_v*.csv"),
               str(CSV_DIR / "*.csv"),
               str(CSV_DIR / "*.xlsx")]
OUT_DIR = Path("figures"); OUT_DIR.mkdir(exist_ok=True, parents=True)

# ---------- Constantes ----------
NUCLEOS     = [2, 4, 6, 8]
POBLACIONES = [50, 100, 500]
GEN_SIZES   = [500, 1000, 5000]       # valores por defecto
TARGET_GEN  = 5000                    # se re-ajustará si no existe
REF_POP     = 100
WINDOW      = 200                     # media móvil goles

# ---------- Cargar datasets ----------
datasets = {}
for file in chain.from_iterable(glob.glob(p) for p in CSV_PATTERN):
    stem = Path(file).stem
    parts = stem.split("_")
    try:
        c = int([p for p in parts if p.endswith("c")][0][:-1])
        p = int([p for p in parts if p.startswith("pop")][0].replace("pop", ""))
        g = int([p for p in parts if p.startswith("gen")][0].replace("gen", ""))
    except IndexError:
        print(f"⚠️  Ignorado (nombre no estándar): {file}")
        continue
    df = pd.read_csv(file) if file.endswith(".csv") else pd.read_excel(file)
    datasets[(c, p, g)] = df

if not datasets:
    raise SystemExit("No se hallaron datasets válidos.")

# ---------- Ajustar generaciones ----------
GENS = sorted({g for *_, g in datasets})
if TARGET_GEN not in GENS:
    TARGET_GEN = max(GENS)
GEN_SIZES = sorted({g for g in GEN_SIZES if g in GENS} | {TARGET_GEN})
print(f"👉 Generaciones detectadas: {GENS}  (TARGET={TARGET_GEN})")

# ---------- Helpers ----------
avg_time  = lambda c, p, g: datasets[(c, p, g)]["Tiempo (s)"].mean()
speedup   = lambda c, p, g: avg_time(2, p, g) / avg_time(c, p, g)
final_fit = lambda c, p, g: datasets[(c, p, g)]["Fitness Global"].iloc[-1]
total_min = lambda c, p, g: avg_time(c, p, g) * g / 60

# ========== F1–F13 originales ==========
# (copié directamente de tu script; omito por brevedad en este comentario)
# --------- pega aquí tus 13 figuras existentes --------------

# ########################################################################
# NUEVAS FIGURAS
# ########################################################################

# ---------- 14. Curva de aprendizaje (fitness medio) ----------
plt.figure(figsize=(9,5))
for cores in NUCLEOS:
    for pop in POBLACIONES:
        df = datasets[(cores, pop, TARGET_GEN)]
        plt.plot(df.index+1, df["Fitness Global"].rolling(WINDOW).mean(),
                 lw=.9, label=f"{cores}C-P{pop}",
                 color=colors[NUCLEOS.index(cores)], alpha=.4+0.2*POBLACIONES.index(pop))
plt.xlabel("Generación"); plt.ylabel("Fitness (media móvil)")
plt.title(f"Curva de aprendizaje · Ventana={WINDOW}")
plt.grid(ls="--", alpha=.3, which="both")
plt.legend(ncol=3, fontsize=7, bbox_to_anchor=(1.02, 1))
plt.tight_layout()
plt.savefig(OUT_DIR/"14_learning_curve.png", dpi=300, bbox_inches="tight")
plt.close()

# ---------- 15. Ribbon GF / GC ----------
cores_r, pop_r, gen_r = 6, 100, TARGET_GEN
df_r = datasets[(cores_r, pop_r, gen_r)]
x = df_r.index+1
gf, gc = df_r["Goles Favor"], df_r["Goles Contra"]
plt.figure(figsize=(9,4))
plt.fill_between(x, 0, gf, color="#4CAF50", alpha=.35, label="GF")
plt.fill_between(x, 0, -gc, color="#F44336", alpha=.35, label="GC")
plt.plot(x, gf, color="#388E3C", lw=.7)
plt.plot(x, -gc, color="#D32F2F", lw=.7)
plt.axhline(0, color="black", lw=.8)
plt.xlabel("Generación"); plt.ylabel("Goles (positivos=Favor, negativos=Contra)")
plt.title(f"GF vs GC · {cores_r}C · Pop {pop_r}")
plt.legend(); plt.tight_layout()
plt.savefig(OUT_DIR/"15_ribbon_gf_gc.png", dpi=300, bbox_inches="tight")
plt.close()

# ---------- 16. Evolución uso CPU ----------
cores_e, pop_e, gen_e = 4, 100, TARGET_GEN
cpu = datasets[(cores_e, pop_e, gen_e)]["CPU (%)"]
mean = cpu.rolling(100).mean()
std  = cpu.rolling(100).std()
plt.figure(figsize=(9,4))
plt.plot(cpu.index+1, cpu, alpha=.25, lw=.5, label="%CPU bruto")
plt.plot(cpu.index+1, mean, color="#1976D2", lw=1.5, label="Media (100)")
plt.fill_between(cpu.index+1, mean-std, mean+std, color="#64B5F6", alpha=.3, label="±1σ")
plt.ylim(0,105); plt.xlabel("Generación"); plt.ylabel("%CPU")
plt.title(f"Evolución %CPU · {cores_e}C · Pop {pop_e}")
plt.legend(); plt.tight_layout()
plt.savefig(OUT_DIR/"16_cpu_evolution.png", dpi=300, bbox_inches="tight")
plt.close()

# ---------- 17. Heat‑map de correlación ----------
sample_df = next(iter(datasets.values()))
num_df = sample_df.select_dtypes(include="number")
corr = num_df.corr()
plt.figure(figsize=(7,6))
im = plt.imshow(corr, cmap="coolwarm", vmin=-1, vmax=1)
plt.xticks(range(len(corr)), corr.columns, rotation=90, fontsize=7)
plt.yticks(range(len(corr)), corr.columns, fontsize=7)
for i in range(len(corr)):
    for j in range(len(corr)):
        plt.text(j, i, f"{corr.iloc[i,j]:.2f}", ha="center", va="center", fontsize=6, color="black")
plt.colorbar(im, shrink=.8)
plt.title("Matriz de correlación")
plt.tight_layout()
plt.savefig(OUT_DIR/"17_corr_heatmap.png", dpi=300, bbox_inches="tight")
plt.close()

# ---------- 18. Ridgeline (KDE %CPU por núcleo) ----------
cores_kde, pop_kde, gen_kde = 8, 100, TARGET_GEN
df_kde = datasets[(cores_kde, pop_kde, gen_kde)]
plt.figure(figsize=(8,6))
offset = 0
for i in range(cores_kde):
    series = df_kde[f"Core{i} (%)"].dropna()
    if series.empty: continue
    kde = gaussian_kde(series)
    xs = np.linspace(0, 100, 200)
    ys = kde(xs)
    ys = ys / ys.max() * 1.0  # normaliza altura
    plt.fill_between(xs, offset, offset+ys, color=colors[i%len(colors)], alpha=.6)
    plt.text(102, offset+.3, f"Core{i}", va="center", fontsize=8)
    offset += 1.1
plt.yticks([]); plt.xlim(0,100); plt.xlabel("%CPU")
plt.title(f"Ridgeline %CPU · {cores_kde}C · Pop {pop_kde}")
plt.tight_layout()
plt.savefig(OUT_DIR/"18_ridgeline_cpu.png", dpi=300, bbox_inches="tight")
plt.close()

# ---------- 19. Swarm+Box de tiempo ----------
cores_sw, pop_sw, gen_sw = 6, 1000, TARGET_GEN
times = datasets[(cores_sw, pop_sw, gen_sw)]["Tiempo (s)"]
plt.figure(figsize=(6,5))
plt.boxplot(times, widths=.5, showfliers=False)
plt.scatter(np.random.normal(1, .05, size=len(times)), times, s=8, alpha=.4)
plt.ylabel("Tiempo (s)"); plt.title(f"Distribución de tiempo · {cores_sw}C · Pop {pop_sw}")
plt.xticks([])
plt.tight_layout()
plt.savefig(OUT_DIR/"19_swarm_box_time.png", dpi=300, bbox_inches="tight")
plt.close()

# ---------- 20. Violin diff goles ----------
plt.figure(figsize=(6,5))
data_diff = [ (datasets[(4, p, TARGET_GEN)]["Goles Favor"] -
               datasets[(4, p, TARGET_GEN)]["Goles Contra"]) for p in POBLACIONES ]
plt.violinplot(data_diff, showextrema=False)
plt.xticks(range(1, len(POBLACIONES)+1), [f"Pop {p}" for p in POBLACIONES])
plt.ylabel("GF - GC"); plt.title(f"Violín diff goles · 4C")
plt.tight_layout()
plt.savefig(OUT_DIR/"20_violin_goaldiff.png", dpi=300, bbox_inches="tight")
plt.close()

# ---------- 21. Convergencia vs Carga ----------
cores_cc, pop_cc, gen_cc = 8, 100, TARGET_GEN
df_cc = datasets[(cores_cc, pop_cc, gen_cc)]
fig, ax1 = plt.subplots(figsize=(8,4))
ax1.plot(df_cc.index+1, df_cc["Fitness Global"].rolling(WINDOW).mean(),
         color="#4CAF50", label="Fitness (mm)")
ax1.set_xlabel("Generación"); ax1.set_ylabel("Fitness")
ax2 = ax1.twinx()
ax2.plot(df_cc.index+1, df_cc["CPU (%)"].rolling(WINDOW).mean(),
         color="#2196F3", label="%CPU (mm)")
ax2.set_ylabel("%CPU")
ax1.grid(ls="--", alpha=.3)
ax1.set_title(f"Convergencia vs Carga · {cores_cc}C · Pop {pop_cc}")
fig.legend(loc="upper left", bbox_to_anchor=(0.1,0.95))
plt.tight_layout()
plt.savefig(OUT_DIR/"21_convergence_vs_load.png", dpi=300, bbox_inches="tight")
plt.close()

# ---------- 22. Heat‑map cores × población ----------
heat = np.zeros((len(NUCLEOS), len(POBLACIONES)))
for i,c in enumerate(NUCLEOS):
    for j,p in enumerate(POBLACIONES):
        heat[i,j] = final_fit(c,p,TARGET_GEN)
plt.figure(figsize=(4,3))
im = plt.imshow(heat, cmap="YlGnBu")
plt.xticks(range(len(POBLACIONES)), POBLACIONES); plt.xlabel("Población")
plt.yticks(range(len(NUCLEOS)), NUCLEOS); plt.ylabel("Núcleos")
for i in range(len(NUCLEOS)):
    for j in range(len(POBLACIONES)):
        plt.text(j, i, f"{heat[i,j]:.0f}", ha="center", va="center", fontsize=7)
plt.title("Fitness final"); plt.colorbar(im, shrink=.8)
plt.tight_layout()
plt.savefig(OUT_DIR/"22_heatmap_cores_pop.png", dpi=300, bbox_inches="tight")
plt.close()

# ---------- 23. Histograma tiempo medio ----------
all_times = [avg_time(c,p,TARGET_GEN) for c in NUCLEOS for p in POBLACIONES]
plt.figure(figsize=(6,4))
plt.hist(all_times, bins=10, color="#FFB300", alpha=.8)
plt.xlabel("Tiempo medio (s)"); plt.ylabel("Frecuencia")
plt.title(f"Histograma tiempo medio · {TARGET_GEN:,} gen")
plt.tight_layout()
plt.savefig(OUT_DIR/"23_hist_avg_time.png", dpi=300, bbox_inches="tight")
plt.close()

print(f"✅ 23 figuras guardadas en {OUT_DIR.resolve()}")
