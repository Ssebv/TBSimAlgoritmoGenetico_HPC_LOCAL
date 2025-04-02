#!/bin/bash

LOG_FILE="simulacion.log"

# Manejo de interrupción
cleanup() {
    echo "🛑 Proceso interrumpido. Cerrando..." >> "$LOG_FILE"
    exit 0
}
trap cleanup SIGINT SIGTERM

# Limpieza previa
if [ -f "$LOG_FILE" ]; then
    rm "$LOG_FILE"
fi

echo "📂 Iniciando MainJenetics..." >> "$LOG_FILE"

# Configurar classpath
CLASSPATH="bin:lib/jenetics-7.2.0.jar:lib/oshi-core.jar:lib/jna-5.12.1.jar:lib/jna-platform-5.12.1.jar:lib/slf4j-api-1.7.36.jar:lib/slf4j-simple-1.7.36.jar"

# Verificar si el directorio bin existe
java -classpath "$CLASSPATH" MainJenetics "$@" >> "$LOG_FILE" 2>&1

# Mostrar estado final
EXIT_CODE=${PIPESTATUS[0]}
if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ Simulación finalizada correctamente." >> "$LOG_FILE"
else
    echo "❌ Simulación terminó con errores. Revisa el log." >> "$LOG_FILE"
fi

exit $EXIT_CODE
