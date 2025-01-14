#!/bin/bash
# run_program.sh
# Script para ejecutar el programa Java con manejo de interrupciones, eliminación previa de logs y redirección de logs

# Definir el nombre del archivo de log
LOG_FILE="simulacion.log"

# Función para manejar la limpieza al recibir una señal de interrupción
cleanup() {
    echo -e "\nProceso interrumpido por el usuario (Ctrl + C). Cerrando de forma segura..."
    # Puedes añadir comandos adicionales aquí si es necesario, como cerrar archivos abiertos
    exit 0
}

# Capturar señales SIGINT (Ctrl+C) y SIGTERM (terminación)
trap cleanup SIGINT SIGTERM

# Informar al usuario que la simulación está comenzando y que el archivo de log será eliminado si existe
echo "Iniciando la simulación. Si existe, el archivo de log '$LOG_FILE' será eliminado para comenzar fresco."

# Eliminar el archivo de log si existe
if [ -f "$LOG_FILE" ]; then
    rm "$LOG_FILE"
    echo "Archivo de log '$LOG_FILE' existente eliminado."
fi

# Informar al usuario sobre la redirección de logs
echo "Los logs se guardarán en '$LOG_FILE'. Puedes revisar el progreso en tiempo real utilizando 'tail -f $LOG_FILE' en otra terminal."

# Ejecutar el programa Java
# - Redirigir stderr (2) al archivo de log
# - Mantener stdout (1) conectado a la terminal para interacciones
java -classpath bin:lib/jenetics-7.2.0.jar MainJenetics "$@" 2>> "$LOG_FILE"

# Capturar el código de salida del programa Java
EXIT_CODE=$?

# Verificar si el programa Java terminó correctamente
if [ $EXIT_CODE -eq 0 ]; then
    echo "Simulación finalizada correctamente."
else
    echo "Simulación terminó con errores. Revisa '$LOG_FILE' para más detalles."
fi

# Salir del script con el mismo código de salida que el programa Java
exit $EXIT_CODE
