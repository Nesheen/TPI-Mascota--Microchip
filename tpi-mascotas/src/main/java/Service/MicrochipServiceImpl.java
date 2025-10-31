package Service;

import Dao.GenericDAO;
import Models.Microchip;

import java.util.List;

/**
 * Implementación del servicio de negocio para la entidad Microchip.
 * Capa intermedia entre la UI y el DAO que aplica validaciones de negocio.
 *
 * Responsabilidades:
 * - Validar que los datos del microchip sean correctos ANTES de persistir
 * - Aplicar reglas de negocio (RN-023: codigo_chip y marca obligatorios)
 * - Delegar operaciones de BD al DAO
 * - Transformar excepciones técnicas en errores de negocio comprensibles
 *
 * Patrón: Service Layer con inyección de dependencias
 */
public class MicrochipServiceImpl implements GenericService<Microchip> {
    /**
     * DAO para acceso a datos de microchips.
     * Inyectado en el constructor (Dependency Injection).
     * Usa GenericDAO para permitir testing con mocks.
     */
    private final GenericDAO<Microchip> microchipDAO;

    /**
     * Constructor con inyección de dependencias.
     * Valida que el DAO no sea null (fail-fast).
     *
     * @param microchipDAO DAO de microchips (normalmente MicrochipDAO)
     * @throws IllegalArgumentException si microchipDAO es null
     */
    public MicrochipServiceImpl(GenericDAO<Microchip> microchipDAO) {
        if (microchipDAO == null) {
            throw new IllegalArgumentException("MicrochipDAO no puede ser null");
        }
        this.microchipDAO = microchipDAO;
    }

    /**
     * Inserta un nuevo microchip en la base de datos.
     *
     * @param microchip Microchip a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla o hay error de BD
     */
    @Override
    public void insertar(Microchip microchip) throws Exception {
        validateMicrochip(microchip);
        microchipDAO.insertar(microchip);
    }

    /**
     * Actualiza un microchip existente en la base de datos.
     *
     * @param microchip Microchip con los datos actualizados
     * @throws Exception Si la validación falla o el microchip no existe
     */
    @Override
    public void actualizar(Microchip microchip) throws Exception {
        validateMicrochip(microchip);
        if (microchip.getId() <= 0) {
            throw new IllegalArgumentException("El ID del microchip debe ser mayor a 0 para actualizar");
        }
        microchipDAO.actualizar(microchip);
    }

    /**
     * Elimina lógicamente un microchip (soft delete).
     *
     * ⚠️ ADVERTENCIA: Este método NO verifica si hay mascotas asociadas.
     * Puede dejar referencias huérfanas en mascotas.microchip_id (RN-029).
     *
     * ALTERNATIVA SEGURA: Usar MascotaServiceImpl.eliminarMicrochipDeMascota()
     *
     * @param id ID del microchip a eliminar
     * @throws Exception Si id <= 0 o no existe el microchip
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        microchipDAO.eliminar(id);
    }

    /**
     * Obtiene un microchip por su ID.
     *
     * @param id ID del microchip a buscar
     * @return Microchip encontrado, o null si no existe o está eliminado
     * @throws Exception Si id <= 0 o hay error de BD
     */
    @Override
    public Microchip getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return microchipDAO.getById(id);
    }

    /**
     * Obtiene todos los microchips activos (eliminado=FALSE).
     *
     * @return Lista de microchips activos (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Microchip> getAll() throws Exception {
        return microchipDAO.getAll();
    }

    /**
     * Valida que un microchip tenga datos correctos.
     *
     * Reglas de negocio aplicadas:
     * - RN-023: codigo_chip y marca son obligatorios
     *
     * @param microchip Microchip a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validateMicrochip(Microchip microchip) {
        if (microchip == null) {
            throw new IllegalArgumentException("El microchip no puede ser null");
        }
        if (microchip.getCodigoChip() == null || microchip.getCodigoChip().trim().isEmpty()) {
            throw new IllegalArgumentException("El código del chip no puede estar vacío");
        }
        if (microchip.getMarca() == null || microchip.getMarca().trim().isEmpty()) {
            throw new IllegalArgumentException("La marca no puede estar vacía");
        }
    }
}