package Service;

import Dao.MascotaDAO;
import Models.Mascota;

import java.util.List;

/**
 * Implementación del servicio de negocio para la entidad Mascota.
 * Capa intermedia entre la UI y el DAO que aplica validaciones de negocio complejas.
 *
 * Responsabilidades:
 * - Validar datos de mascota ANTES de persistir (RN-035: nombre, especie, codigo_tag obligatorios)
 * - Garantizar unicidad del CodigoTag en el sistema (RN-001)
 * - COORDINAR operaciones entre Mascota y Microchip (transaccionales)
 * - Proporcionar métodos de búsqueda especializados (por CodigoTag, nombre/especie)
 * - Implementar eliminación SEGURA de microchips (evita FKs huérfanas)
 *
 * Patrón: Service Layer con inyección de dependencias y coordinación de servicios
 */
public class MascotaServiceImpl implements GenericService<Mascota> {
    /**
     * DAO para acceso a datos de mascotas.
     */
    private final MascotaDAO mascotaDAO;

    /**
     * Servicio de microchips para coordinar operaciones transaccionales.
     */
    private final MicrochipServiceImpl microchipServiceImpl;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param mascotaDAO DAO de mascotas
     * @param microchipServiceImpl Servicio de microchips para operaciones coordinadas
     * @throws IllegalArgumentException si alguna dependencia es null
     */
    public MascotaServiceImpl(MascotaDAO mascotaDAO, MicrochipServiceImpl microchipServiceImpl) {
        if (mascotaDAO == null) {
            throw new IllegalArgumentException("MascotaDAO no puede ser null");
        }
        if (microchipServiceImpl == null) {
            throw new IllegalArgumentException("MicrochipServiceImpl no puede ser null");
        }
        this.mascotaDAO = mascotaDAO;
        this.microchipServiceImpl = microchipServiceImpl;
    }

    /**
     * Inserta una nueva mascota en la base de datos.
     *
     * @param mascota Mascota a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla, el CodigoTag está duplicado, o hay error de BD
     */
    @Override
    public void insertar(Mascota mascota) throws Exception {
        validateMascota(mascota);
        validateCodigoTagUnique(mascota.getCodigoTag(), null);

        // Coordinación con MicrochipService (transaccional)
        if (mascota.getMicrochip() != null) {
            if (mascota.getMicrochip().getId() == 0) {
                // Microchip nuevo: insertar primero para obtener ID autogenerado
                microchipServiceImpl.insertar(mascota.getMicrochip());
            } else {
                // Microchip existente: actualizar datos
                microchipServiceImpl.actualizar(mascota.getMicrochip());
            }
        }

        mascotaDAO.insertar(mascota);
    }

    /**
     * Actualiza una mascota existente en la base de datos.
     *
     * @param mascota Mascota con los datos actualizados
     * @throws Exception Si la validación falla, el CodigoTag está duplicado, o la mascota no existe
     */
    @Override
    public void actualizar(Mascota mascota) throws Exception {
        validateMascota(mascota);
        if (mascota.getId() <= 0) {
            throw new IllegalArgumentException("El ID de la mascota debe ser mayor a 0 para actualizar");
        }
        validateCodigoTagUnique(mascota.getCodigoTag(), mascota.getId());
        mascotaDAO.actualizar(mascota);
    }

    /**
     * Elimina lógicamente una mascota (soft delete).
     *
     * ⚠️ IMPORTANTE: Este método NO elimina el microchip asociado (RN-037).
     *
     * @param id ID de la mascota a eliminar
     * @throws Exception Si id <= 0 o no existe la mascota
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        mascotaDAO.eliminar(id);
    }

    /**
     * Obtiene una mascota por su ID.
     * Incluye el microchip asociado mediante LEFT JOIN (MascotaDAO).
     *
     * @param id ID de la mascota a buscar
     * @return Mascota encontrada (con su microchip si tiene), o null si no existe o está eliminada
     * @throws Exception Si id <= 0 o hay error de BD
     */
    @Override
    public Mascota getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return mascotaDAO.getById(id);
    }

    /**
     * Obtiene todas las mascotas activas (eliminado=FALSE).
     * Incluye sus microchips mediante LEFT JOIN (MascotaDAO).
     *
     * @return Lista de mascotas activas con sus microchips (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Mascota> getAll() throws Exception {
        return mascotaDAO.getAll();
    }

    /**
     * Expone el servicio de microchips para que MenuHandler pueda usarlo.
     *
     * @return Instancia de MicrochipServiceImpl inyectada en este servicio
     */
    public MicrochipServiceImpl getMicrochipService() {
        return this.microchipServiceImpl;
    }

    /**
     * Busca mascotas por nombre o especie (búsqueda flexible con LIKE).
     *
     * @param filtro Texto a buscar (no puede estar vacío)
     * @return Lista de mascotas que coinciden con el filtro (puede estar vacía)
     * @throws IllegalArgumentException Si el filtro está vacío
     * @throws Exception Si hay error de BD
     */
    public List<Mascota> buscarPorNombreEspecie(String filtro) throws Exception {
        if (filtro == null || filtro.trim().isEmpty()) {
            throw new IllegalArgumentException("El filtro de búsqueda no puede estar vacío");
        }
        return mascotaDAO.buscarPorNombreEspecie(filtro);
    }

    /**
     * Busca una mascota por CodigoTag exacto.
     *
     * @param codigoTag CodigoTag exacto a buscar (no puede estar vacío)
     * @return Mascota con ese CodigoTag, o null si no existe o está eliminada
     * @throws IllegalArgumentException Si el CodigoTag está vacío
     * @throws Exception Si hay error de BD
     */
    public Mascota buscarPorCodigoTag(String codigoTag) throws Exception {
        if (codigoTag == null || codigoTag.trim().isEmpty()) {
            throw new IllegalArgumentException("El CodigoTag no puede estar vacío");
        }
        return mascotaDAO.buscarPorCodigoTag(codigoTag);
    }

    /**
     * Elimina un microchip de forma SEGURA actualizando primero la FK de la mascota.
     * Este es el método RECOMENDADO para eliminar microchips (RN-029 solucionado).
     *
     * Flujo transaccional SEGURO:
     * 1. Obtiene la mascota por ID y valida que exista
     * 2. Verifica que el microchip pertenezca a esa mascota
     * 3. Desasocia el microchip de la mascota (mascota.microchip = null)
     * 4. Actualiza la mascota en BD (microchip_id = NULL)
     * 5. Elimina el microchip (ahora no hay FKs apuntando a él)
     *
     * @param mascotaId ID de la mascota dueña del microchip
     * @param microchipId ID del microchip a eliminar
     * @throws IllegalArgumentException Si los IDs son <= 0, la mascota no existe, o el microchip no pertenece a la mascota
     * @throws Exception Si hay error de BD
     */
    public void eliminarMicrochipDeMascota(int mascotaId, int microchipId) throws Exception {
        if (mascotaId <= 0 || microchipId <= 0) {
            throw new IllegalArgumentException("Los IDs deben ser mayores a 0");
        }

        Mascota mascota = mascotaDAO.getById(mascotaId);
        if (mascota == null) {
            throw new IllegalArgumentException("Mascota no encontrada con ID: " + mascotaId);
        }

        if (mascota.getMicrochip() == null || mascota.getMicrochip().getId() != microchipId) {
            throw new IllegalArgumentException("El microchip no pertenece a esta mascota");
        }

        // Secuencia transaccional: actualizar FK → eliminar microchip
        mascota.setMicrochip(null);
        mascotaDAO.actualizar(mascota);
        microchipServiceImpl.eliminar(microchipId);
    }

    /**
     * Valida que una mascota tenga datos correctos.
     *
     * @param mascota Mascota a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validateMascota(Mascota mascota) {
        if (mascota == null) {
            throw new IllegalArgumentException("La mascota no puede ser null");
        }
        if (mascota.getNombre() == null || mascota.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (mascota.getEspecie() == null || mascota.getEspecie().trim().isEmpty()) {
            throw new IllegalArgumentException("La especie no puede estar vacía");
        }
        if (mascota.getCodigoTag() == null || mascota.getCodigoTag().trim().isEmpty()) {
            throw new IllegalArgumentException("El CodigoTag no puede estar vacío");
        }
    }

    /**
     * Valida que un CodigoTag sea único en el sistema.
     * Implementa la regla de negocio RN-001: "El CodigoTag debe ser único".
     *
     * @param codigoTag CodigoTag a validar
     * @param mascotaId ID de la mascota (null para INSERT, != null para UPDATE)
     * @throws IllegalArgumentException Si el CodigoTag ya existe y pertenece a otra mascota
     * @throws Exception Si hay error de BD al buscar
     */
    private void validateCodigoTagUnique(String codigoTag, Integer mascotaId) throws Exception {
        Mascota existente = mascotaDAO.buscarPorCodigoTag(codigoTag);
        if (existente != null) {
            if (mascotaId == null || existente.getId() != mascotaId) {
                throw new IllegalArgumentException("Ya existe una mascota con el CodigoTag: " + codigoTag);
            }
        }
    }
}