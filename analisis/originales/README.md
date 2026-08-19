# Scripts originales (preservados sin modificar)

Scripts con los que se generaron las figuras del capítulo 5. Se conservan **tal
cual se ejecutaron**, sin correcciones, como evidencia de procedencia.

| Archivo | Estado al ejecutarlo hoy |
|---|---|
| `visualize_ag_final.py` | **Se interrumpe a mitad.** Genera 5 figuras y falla con `KeyError: (6, 1000, 3000)`: la constante `REF_POP` apunta a una población de 1.000 que no existe en el diseño factorial. |
| `visualize_ag_final_fix.py` | Corrección puntual de junio-2025 que reemplaza esa figura usando `REF_POP = 100`. Solo regenera la figura 19. |

No se corrigen a propósito: su valor aquí es documental. **Para regenerar
figuras usar `analisis/figuras.py`**, que está mantenido, cubre las figuras 01,
09 y 10 del README y se ejecuta en la integración continua.

Detalle relevante: la matriz de correlación de estos scripts se calcula sobre
`next(iter(datasets.values()))` —un único archivo arbitrario— y no sobre el
conjunto agregado. Los valores publicados (−0,71 / +0,58 / +0,60) sí se
reproducen desde los 36.000 registros agregados; ver `analisis/verificar_cifras.py`.
