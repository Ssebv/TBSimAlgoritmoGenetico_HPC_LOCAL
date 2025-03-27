#!/bin/bash
# run_program.sh

LOG_FILE="simulación.log"

cleanup() {
    echo "Proceso interrumpido por el usuario (Ctrl + C). Cerrando de forma segura..."
    exit 0
}

trap cleanup SIGINT SIGTERM

echo "Iniciando la simulación. Se eliminará '$LOG_FILE' si existe..."
if [ -f "$LOG_FILE" ]; then
    rm "$LOG_FILE"
    echo "Archivo de log '$LOG_FILE' eliminado."
fi

echo "Los logs se guardarán en '$LOG_FILE'. Solo se mostrará en pantalla las líneas que contengan [DEBUG]."

CLASSPATH="bin:lib/jenetics-7.2.0.jar:lib/oshi-core.jar:lib/jna-5.12.1.jar:lib/jna-platform-5.12.1.jar:lib/slf4j-api-1.7.36.jar:lib/slf4j-simple-1.7.36.jar"

# Ejecuta la aplicación redirigiendo toda la salida a simulación.log y filtrando solo las líneas [DEBUG] para mostrarlas en la terminal
java -classpath "$CLASSPATH" MainJenetics "$@" 2>&1 | tee "$LOG_FILE" | grep "\[DEBUG\]"

EXIT_CODE=${PIPESTATUS[0]}
if [ $EXIT_CODE -eq 0 ]; then
    echo "Simulación finalizada correctamente."
else
    echo "Simulación terminó con errores. Revisa '$LOG_FILE' para más detalles."
fi
exit $EXIT_CODE
