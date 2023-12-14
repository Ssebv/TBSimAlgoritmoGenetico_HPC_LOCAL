import pandas as pd
import matplotlib.pyplot as plt

# Carga los datos desde el archivo CSV
datos = pd.read_csv('../ResultadosAlgoritmoGenetico.csv')

# Graficar la aptitud del mejor individuo por generación
plt.figure(figsize=(10, 6))
plt.plot(datos['Generación'], datos['Aptitud Mejor Individuo'], marker='o')
plt.title('Aptitud del Mejor Individuo por Generación')
plt.xlabel('Generación')
plt.ylabel('Aptitud')
plt.grid(True)
plt.savefig('aptitud_por_generacion.png')

# Graficar el tiempo por generación
plt.figure(figsize=(10, 6))
plt.plot(datos['Generación'], datos['Tiempo por generacion'], marker='o', color='r')
plt.title('Tiempo por Generación')
plt.xlabel('Generación')
plt.ylabel('Tiempo (nanosegundos)')
plt.grid(True)
plt.savefig('tiempo_por_generacion.png')

# Graficar el tiempo acumulado por generación
plt.figure(figsize=(10, 6))
plt.plot(datos['Generación'], datos['Tiempo total'], marker='o', color='g')
plt.title('Tiempo Acumulado por Generación')
plt.xlabel('Generación')
plt.ylabel('Tiempo Acumulado (nanosegundos)')
plt.grid(True)
plt.savefig('tiempo_acumulado_por_generacion.png')



