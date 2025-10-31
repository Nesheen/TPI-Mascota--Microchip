package Main;

/**
 * Punto de entrada alternativo de la aplicación.
 * Clase simple que delega inmediatamente a AppMenu.
 */
public class Main {
    /**
     * Punto de entrada alternativo de la aplicación Java.
     * Crea AppMenu y ejecuta el menú principal.
     *
     * @param args Argumentos de línea de comandos (no usados)
     */
    public static void main(String[] args) {
        AppMenu app = new AppMenu();
        app.run();
    }
}