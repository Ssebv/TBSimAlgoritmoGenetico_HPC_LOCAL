public class ConfiguracionSingleton {
    private static final Configuracion instance = new Configuracion();
    
    public static Configuracion getInstance() {
        return instance;
    }
}
