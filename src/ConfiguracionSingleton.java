/**
 * Singleton para acceder a la configuración global del sistema.
 * Asegura una única instancia de la clase Configuracion.
 */
public final class ConfiguracionSingleton {

    // Instancia única (final garantiza inmutabilidad de la referencia)
    private static final Configuracion INSTANCE = new Configuracion();

    // Constructor privado para evitar instanciación externa
    private ConfiguracionSingleton() {
        // Previene instanciación
    }

    /**
     * Devuelve la instancia única de Configuracion.
     *
     * @return Configuración compartida en toda la aplicación.
     */
    public static Configuracion getInstance() {
        return INSTANCE;
    }
}
