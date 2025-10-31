package Main;

/**
 * Clase utilitaria para mostrar el menú de la aplicación.
 * Solo contiene métodos estáticos de visualización (no tiene estado).
 *
 * Responsabilidades:
 * - Mostrar el menú principal con todas las opciones disponibles
 * - Formatear la salida de forma consistente
 *
 * Patrón: Utility class (solo métodos estáticos, no instanciable)
 */
public class MenuDisplay {
    /**
     * Muestra el menú principal con todas las opciones CRUD.
     */
    public static void mostrarMenuPrincipal() {
        System.out.println("\n========= MENU MASCOTAS Y MICROCHIPS =========");
        System.out.println("1. Registrar mascota");
        System.out.println("2. Listar mascotas");
        System.out.println("3. Actualizar mascota");
        System.out.println("4. Eliminar mascota (soft delete)");
        System.out.println("5. Registrar microchip");
        System.out.println("6. Listar microchips");
        System.out.println("7. Actualizar microchip por ID");
        System.out.println("8. Eliminar microchip por ID (Peligroso)");
        System.out.println("9. Actualizar microchip por ID de mascota");
        System.out.println("10. Eliminar microchip por ID de mascota (Seguro)");
        System.out.println("0. Salir");
        System.out.print("Ingrese una opcion: ");
    }
}