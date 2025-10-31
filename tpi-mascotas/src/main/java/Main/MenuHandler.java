package Main;

import Models.Microchip;
import Models.Mascota;
import Service.MascotaServiceImpl;

import java.util.List;
import java.util.Scanner;

/**
 * Controlador de las operaciones del menú (Menu Handler).
 * Gestiona toda la lógica de interacción con el usuario para operaciones CRUD.
 *
 * Responsabilidades:
 * - Capturar entrada del usuario desde consola (Scanner)
 * - Validar entrada básica (conversión de tipos, valores vacíos)
 * - Invocar servicios de negocio (MascotaService, MicrochipService)
 * - Mostrar resultados y mensajes de error al usuario
 *
 * IMPORTANTE: Este handler NO contiene lógica de negocio.
 * Todas las validaciones de negocio están en la capa Service.
 */
public class MenuHandler {
    /**
     * Scanner compartido para leer entrada del usuario.
     */
    private final Scanner scanner;

    /**
     * Servicio de mascotas para operaciones CRUD.
     */
    private final MascotaServiceImpl mascotaService;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param scanner Scanner compartido para entrada de usuario
     * @param mascotaService Servicio de mascotas
     * @throws IllegalArgumentException si alguna dependencia es null
     */
    public MenuHandler(Scanner scanner, MascotaServiceImpl mascotaService) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner no puede ser null");
        }
        if (mascotaService == null) {
            throw new IllegalArgumentException("MascotaService no puede ser null");
        }
        this.scanner = scanner;
        this.mascotaService = mascotaService;
    }

    /**
     * Opción 1: Crear nueva mascota (con microchip opcional).
     */
    public void crearMascota() {
        try {
            System.out.print("Nombre: ");
            String nombre = scanner.nextLine().trim();
            System.out.print("Especie (ej: Perro, Gato): ");
            String especie = scanner.nextLine().trim();
            System.out.print("Codigo Tag (Identificador Unico): ");
            String codigoTag = scanner.nextLine().trim();

            Microchip microchip = null;
            System.out.print("¿Desea agregar un microchip? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                microchip = crearMicrochip();
            }

            Mascota mascota = new Mascota(0, nombre, especie, codigoTag);
            mascota.setMicrochip(microchip);
            mascotaService.insertar(mascota);
            System.out.println("Mascota registrada exitosamente con ID: " + mascota.getId());
        } catch (Exception e) {
            System.err.println("Error al crear mascota: " + e.getMessage());
        }
    }

    /**
     * Opción 2: Listar mascotas (todas o filtradas por nombre/especie).
     */
    public void listarMascotas() {
        try {
            System.out.print("¿Desea (1) listar todas o (2) buscar por nombre/especie? Ingrese opcion: ");
            int subopcion = Integer.parseInt(scanner.nextLine());

            List<Mascota> mascotas;
            if (subopcion == 1) {
                mascotas = mascotaService.getAll();
            } else if (subopcion == 2) {
                System.out.print("Ingrese texto a buscar: ");
                String filtro = scanner.nextLine().trim();
                mascotas = mascotaService.buscarPorNombreEspecie(filtro);
            } else {
                System.out.println("Opcion invalida.");
                return;
            }

            if (mascotas.isEmpty()) {
                System.out.println("No se encontraron mascotas.");
                return;
            }

            for (Mascota m : mascotas) {
                System.out.println("ID: " + m.getId() + ", Nombre: " + m.getNombre() +
                        ", Especie: " + m.getEspecie() + ", Codigo Tag: " + m.getCodigoTag());
                if (m.getMicrochip() != null) {
                    System.out.println("   Microchip: " + m.getMicrochip().getCodigoChip() +
                            " (Marca: " + m.getMicrochip().getMarca() + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("Error al listar mascotas: " + e.getMessage());
        }
    }

    /**
     * Opción 3: Actualizar mascota existente.
     */
    public void actualizarMascota() {
        try {
            System.out.print("ID de la mascota a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Mascota m = mascotaService.getById(id);

            if (m == null) {
                System.out.println("Mascota no encontrada.");
                return;
            }

            System.out.print("Nuevo nombre (actual: " + m.getNombre() + ", Enter para mantener): ");
            String nombre = scanner.nextLine().trim();
            if (!nombre.isEmpty()) {
                m.setNombre(nombre);
            }

            System.out.print("Nueva especie (actual: " + m.getEspecie() + ", Enter para mantener): ");
            String especie = scanner.nextLine().trim();
            if (!especie.isEmpty()) {
                m.setEspecie(especie);
            }

            System.out.print("Nuevo Codigo Tag (actual: " + m.getCodigoTag() + ", Enter para mantener): ");
            String codigoTag = scanner.nextLine().trim();
            if (!codigoTag.isEmpty()) {
                m.setCodigoTag(codigoTag);
            }

            actualizarMicrochipDeMascota(m);
            mascotaService.actualizar(m);
            System.out.println("Mascota actualizada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar mascota: " + e.getMessage());
        }
    }

    /**
     * Opción 4: Eliminar mascota (soft delete).
     */
    public void eliminarMascota() {
        try {
            System.out.print("ID de la mascota a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine());
            mascotaService.eliminar(id);
            System.out.println("Mascota eliminada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al eliminar mascota: " + e.getMessage());
        }
    }

    /**
     * Opción 5: Crear microchip independiente (sin asociar a mascota).
     */
    public void crearMicrochipIndependiente() {
        try {
            Microchip microchip = crearMicrochip();
            mascotaService.getMicrochipService().insertar(microchip);
            System.out.println("Microchip creado exitosamente con ID: " + microchip.getId());
        } catch (Exception e) {
            System.err.println("Error al crear microchip: " + e.getMessage());
        }
    }

    /**
     * Opción 6: Listar todos los microchips activos.
     */
    public void listarMicrochips() {
        try {
            List<Microchip> microchips = mascotaService.getMicrochipService().getAll();
            if (microchips.isEmpty()) {
                System.out.println("No se encontraron microchips.");
                return;
            }
            for (Microchip m : microchips) {
                System.out.println("ID: " + m.getId() + ", Codigo: " + m.getCodigoChip() + ", Marca: " + m.getMarca());
            }
        } catch (Exception e) {
            System.err.println("Error al listar microchips: " + e.getMessage());
        }
    }

    /**
     * Opción 7: Actualizar microchip por ID.
     */
    public void actualizarMicrochipPorId() {
        try {
            System.out.print("ID del microchip a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Microchip m = mascotaService.getMicrochipService().getById(id);

            if (m == null) {
                System.out.println("Microchip no encontrado.");
                return;
            }

            System.out.print("Nuevo codigo chip (actual: " + m.getCodigoChip() + ", Enter para mantener): ");
            String codigoChip = scanner.nextLine().trim();
            if (!codigoChip.isEmpty()) {
                m.setCodigoChip(codigoChip);
            }

            System.out.print("Nueva marca (actual: " + m.getMarca() + ", Enter para mantener): ");
            String marca = scanner.nextLine().trim();
            if (!marca.isEmpty()) {
                m.setMarca(marca);
            }

            mascotaService.getMicrochipService().actualizar(m);
            System.out.println("Microchip actualizado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar microchip: " + e.getMessage());
        }
    }

    /**
     * Opción 8: Eliminar microchip por ID (PELIGROSO - soft delete directo).
     */
    public void eliminarMicrochipPorId() {
        try {
            System.out.print("ID del microchip a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine());
            mascotaService.getMicrochipService().eliminar(id);
            System.out.println("Microchip eliminado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al eliminar microchip: " + e.getMessage());
        }
    }

    /**
     * Opción 9: Actualizar microchip de una mascota específica.
     */
    public void actualizarMicrochipPorMascota() {
        try {
            System.out.print("ID de la mascota cuyo microchip desea actualizar: ");
            int mascotaId = Integer.parseInt(scanner.nextLine());
            Mascota m = mascotaService.getById(mascotaId);

            if (m == null) {
                System.out.println("Mascota no encontrada.");
                return;
            }

            if (m.getMicrochip() == null) {
                System.out.println("La mascota no tiene microchip asociado.");
                return;
            }

            Microchip mic = m.getMicrochip();
            System.out.print("Nuevo codigo chip (" + mic.getCodigoChip() + "): ");
            String codigoChip = scanner.nextLine().trim();
            if (!codigoChip.isEmpty()) {
                mic.setCodigoChip(codigoChip);
            }

            System.out.print("Nueva marca (" + mic.getMarca() + "): ");
            String marca = scanner.nextLine().trim();
            if (!marca.isEmpty()) {
                mic.setMarca(marca);
            }

            mascotaService.getMicrochipService().actualizar(mic);
            System.out.println("Microchip actualizado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar microchip: " + e.getMessage());
        }
    }

    /**
     * Opción 10: Eliminar microchip de una mascota (MÉTODO SEGURO).
     */
    public void eliminarMicrochipPorMascota() {
        try {
            System.out.print("ID de la mascota cuyo microchip desea eliminar: ");
            int mascotaId = Integer.parseInt(scanner.nextLine());
            Mascota m = mascotaService.getById(mascotaId);

            if (m == null) {
                System.out.println("Mascota no encontrada.");
                return;
            }

            if (m.getMicrochip() == null) {
                System.out.println("La mascota no tiene microchip asociado.");
                return;
            }

            int microchipId = m.getMicrochip().getId();
            mascotaService.eliminarMicrochipDeMascota(mascotaId, microchipId);
            System.out.println("Microchip eliminado exitosamente y referencia actualizada.");
        } catch (Exception e) {
            System.err.println("Error al eliminar microchip: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar privado: Crea un objeto Microchip capturando datos.
     *
     * @return Microchip nuevo (no persistido, ID=0)
     */
    private Microchip crearMicrochip() {
        System.out.print("Codigo Chip: ");
        String codigoChip = scanner.nextLine().trim();
        System.out.print("Marca: ");
        String marca = scanner.nextLine().trim();
        return new Microchip(0, codigoChip, marca);
    }

    /**
     * Método auxiliar privado: Maneja actualización de microchip dentro de actualizar mascota.
     *
     * @param m Mascota a la que se le actualizará/agregará microchip
     * @throws Exception Si hay error al insertar/actualizar microchip
     */
    private void actualizarMicrochipDeMascota(Mascota m) throws Exception {
        if (m.getMicrochip() != null) {
            System.out.print("¿Desea actualizar el microchip? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                System.out.print("Nuevo codigo chip (" + m.getMicrochip().getCodigoChip() + "): ");
                String codigoChip = scanner.nextLine().trim();
                if (!codigoChip.isEmpty()) {
                    m.getMicrochip().setCodigoChip(codigoChip);
                }

                System.out.print("Nueva marca (" + m.getMicrochip().getMarca() + "): ");
                String marca = scanner.nextLine().trim();
                if (!marca.isEmpty()) {
                    m.getMicrochip().setMarca(marca);
                }

                mascotaService.getMicrochipService().actualizar(m.getMicrochip());
            }
        } else {
            System.out.print("La mascota no tiene microchip. ¿Desea agregar uno? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                Microchip nuevoMic = crearMicrochip();
                mascotaService.getMicrochipService().insertar(nuevoMic);
                m.setMicrochip(nuevoMic);
            }
        }
    }
}