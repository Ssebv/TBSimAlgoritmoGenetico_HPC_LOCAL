#!/bin/bash
# run_program.sh
LOG_FILE="simulacion.log"

cleanup() {
    echo -e "\nProceso interrumpido por el usuario (Ctrl + C). Cerrando de forma segura..."
    exit 0
}

trap cleanup SIGINT SIGTERM

echo "Iniciando la simulación. Si existe, el archivo de log '$LOG_FILE' será eliminado para comenzar fresco."

if [ -f "$LOG_FILE" ]; then
    rm "$LOG_FILE"
    echo "Archivo de log '$LOG_FILE' existente eliminado."
fi

echo "Los logs se guardarán en '$LOG_FILE'. Puedes revisar el progreso en tiempo real utilizando 'tail -f $LOG_FILE' en otra terminal."

CLASSPATH=bin:lib/jenetics-7.2.0.jar:lib/oshi-core.jar:lib/jna-5.12.1.jar:lib/jna-platform-5.12.1.jar:lib/slf4j-api-1.7.36.jar:lib/slf4j-simple-1.7.36.jar
java -classpath "$CLASSPATH" MainJenetics "$@" 2>> "$LOG_FILE"

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
    echo "Simulación finalizada correctamente."
else
    echo "Simulación terminó con errores. Revisa '$LOG_FILE' para más detalles."
fi
exit $EXIT_CODE
