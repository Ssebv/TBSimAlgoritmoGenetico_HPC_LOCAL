import pandas as pd
import matplotlib.pyplot as plt

# Carga los datos desde el archivo CSV
datos = pd.read_csv('../ResultadosAlgoritmoGenetico.csv')


# Ajustes para los gráficos
fig_size = (12, 8)  # Tamaño de la figura aumentado
alpha = 0.7  # Transparencia

# Graficar la aptitud del mejor individuo por generación
plt.figure(figsize=fig_size)
plt.plot(datos['Generacion'], datos['Aptitud Mejor Individuo'], alpha=alpha)
plt.title('Aptitud del Mejor Individuo por Generación')
plt.xlabel('Generación')
plt.ylabel('Aptitud')
plt.grid(True)

# Graficar el tiempo por generación con suavizado
plt.figure(figsize=fig_size)
plt.plot(datos['Generacion'], datos['Tiempo por generacion'].rolling(window=5).mean(), color='r', alpha=alpha)
plt.title('Tiempo por Generación (Promedio Móvil)')
plt.xlabel('Generación')
plt.ylabel('Tiempo (milisegundos)')
plt.grid(True)

# Graficar el tiempo total acumulado por generación
plt.figure(figsize=fig_size)
plt.plot(datos['Generacion'], datos['Tiempo total'], color='g', alpha=alpha)
plt.title('Tiempo Acumulado por Generación')
plt.xlabel('Generación')
plt.ylabel('Tiempo Acumulado (milisegundos)')
plt.grid(True)

# Gráfico de dispersión para el uso de CPU
plt.figure(figsize=fig_size)
plt.scatter(datos['Generacion'], datos['Uso CPU'], color='y', alpha=alpha)
plt.title('Uso de CPU por Generación')
plt.xlabel('Generación')
plt.ylabel('Uso de CPU (%)')
plt.grid(True)




