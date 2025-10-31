package Main;

import Dao.MicrochipDAO;
import Dao.MascotaDAO;
import Service.MicrochipServiceImpl;
import Service.MascotaServiceImpl;

import java.util.Scanner;

/**
 * Orquestador principal del menú de la aplicación.
 * Gestiona el ciclo de vida del menú y coordina todas las dependencias.
 *
 * Responsabilidades:
 * - Crear y gestionar el Scanner único
 * - Inicializar toda la cadena de dependencias (DAOs → Services → Handler)
 * - Ejecutar el loop principal del menú
 * - Manejar la selección de opciones y delegarlas a MenuHandler
 * - Cerrar recursos al salir (Scanner)
 *
 * Patrón: Application Controller + Dependency Injection manual
 */
public class AppMenu {
    /**
     * Scanner único compartido por toda la aplicación.
     */
    private final Scanner scanner;

    /**
     * Handler que ejecuta las operaciones del menú.
     */
    private final MenuHandler menuHandler;

    /**
     * Flag que controla el loop principal del menú.
     */
    private boolean running;

    /**
     * Constructor que inicializa la aplicación.
     *
     * Flujo de inicialización (Inyección de Dependencias manual):
     * 1. Crea Scanner
     * 2. Crea MicrochipDAO
     * 3. Crea MascotaDAO (depende de MicrochipDAO)
     * 4. Crea MicrochipServiceImpl (depende de MicrochipDAO)
     * 5. Crea MascotaServiceImpl (depende de MascotaDAO y MicrochipServiceImpl)
     * 6. Crea MenuHandler (depende de Scanner y MascotaServiceImpl)
     */
    public AppMenu() {
        this.scanner = new Scanner(System.in);
        MascotaServiceImpl mascotaService = createMascotaService();
        this.menuHandler = new MenuHandler(scanner, mascotaService);
        this.running = true;
    }

    /**
     * Punto de entrada de la aplicación Java.
     *
     * @param args Argumentos de línea de comandos (no usados)
     */
    public static void main(String[] args) {
        AppMenu app = new AppMenu();
        app.run();
    }

    /**
     * Loop principal del menú.
     */
    public void run() {
        while (running) {
            try {
                MenuDisplay.mostrarMenuPrincipal();
                int opcion = Integer.parseInt(scanner.nextLine());
                processOption(opcion);
            } catch (NumberFormatException e) {
                System.out.println("Entrada invalida. Por favor, ingrese un numero.");
            }
        }
        scanner.close();
    }

    /**
     * Procesa la opción seleccionada por el usuario y delega a MenuHandler.
     *
     * @param opcion Número de opción ingresado por el usuario
     */
    private void processOption(int opcion) {
        switch (opcion) {
            case 1 -> menuHandler.crearMascota();
            case 2 -> menuHandler.listarMascotas();
            case 3 -> menuHandler.actualizarMascota();
            case 4 -> menuHandler.eliminarMascota();
            case 5 -> menuHandler.crearMicrochipIndependiente();
            case 6 -> menuHandler.listarMicrochips();
            case 7 -> menuHandler.actualizarMicrochipPorId();
            case 8 -> menuHandler.eliminarMicrochipPorId();
            case 9 -> menuHandler.actualizarMicrochipPorMascota();
            case 10 -> menuHandler.eliminarMicrochipPorMascota();
            case 0 -> {
                System.out.println("Saliendo...");
                running = false;
            }
            default -> System.out.println("Opcion no valida.");
        }
    }

    /**
     * Factory method que crea la cadena de dependencias de servicios.
     * Implementa inyección de dependencias manual.
     *
     * @return MascotaServiceImpl completamente inicializado
     */
    private MascotaServiceImpl createMascotaService() {
        MicrochipDAO microchipDAO = new MicrochipDAO();
        MascotaDAO mascotaDAO = new MascotaDAO(microchipDAO);
        MicrochipServiceImpl microchipService = new MicrochipServiceImpl(microchipDAO);
        return new MascotaServiceImpl(mascotaDAO, microchipService);
    }
}