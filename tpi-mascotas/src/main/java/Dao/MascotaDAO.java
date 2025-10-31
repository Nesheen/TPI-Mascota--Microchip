package Dao;

import Config.DatabaseConnection;
import Models.Microchip;
import Models.Mascota;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Mascota.
 * Gestiona todas las operaciones de persistencia de mascotas en la base de datos.
 *
 * Características:
 * - Implementa GenericDAO<Mascota> para operaciones CRUD estándar
 * - Usa PreparedStatements en TODAS las consultas (protección contra SQL injection)
 * - Maneja LEFT JOIN con microchips para cargar la relación de forma eager
 * - Implementa soft delete (eliminado=TRUE, no DELETE físico)
 * - Proporciona búsquedas especializadas (por CodigoTag exacto, por nombre/especie con LIKE)
 * - Soporta transacciones mediante insertTx() (recibe Connection externa)
 *
 * Patrón: DAO con try-with-resources para manejo automático de recursos JDBC
 */
public class MascotaDAO implements GenericDAO<Mascota> {
    /**
     * Query de inserción de mascota.
     * Inserta nombre, especie, codigo_tag y FK microchip_id.
     */
    private static final String INSERT_SQL = "INSERT INTO mascotas (nombre, especie, codigo_tag, microchip_id) VALUES (?, ?, ?, ?)";

    /**
     * Query de actualización de mascota.
     * Actualiza nombre, especie, codigo_tag y FK microchip_id por id.
     */
    private static final String UPDATE_SQL = "UPDATE mascotas SET nombre = ?, especie = ?, codigo_tag = ?, microchip_id = ? WHERE id = ?";

    /**
     * Query de soft delete.
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     */
    private static final String DELETE_SQL = "UPDATE mascotas SET eliminado = TRUE WHERE id = ?";

    /**
     * Query para obtener mascota por ID.
     * LEFT JOIN con microchips para cargar la relación de forma eager.
     * Solo retorna mascotas activas (eliminado=FALSE).
     */
    private static final String SELECT_BY_ID_SQL = "SELECT m.id, m.nombre, m.especie, m.codigo_tag, m.microchip_id, " +
            "c.id AS mic_id, c.codigo_chip, c.marca " +
            "FROM mascotas m LEFT JOIN microchips c ON m.microchip_id = c.id " +
            "WHERE m.id = ? AND m.eliminado = FALSE";

    /**
     * Query para obtener todas las mascotas activas.
     * LEFT JOIN con microchips para cargar relaciones.
     * Filtra por eliminado=FALSE (solo mascotas activas).
     */
    private static final String SELECT_ALL_SQL = "SELECT m.id, m.nombre, m.especie, m.codigo_tag, m.microchip_id, " +
            "c.id AS mic_id, c.codigo_chip, c.marca " +
            "FROM mascotas m LEFT JOIN microchips c ON m.microchip_id = c.id " +
            "WHERE m.eliminado = FALSE";

    /**
     * Query de búsqueda por nombre o especie con LIKE.
     * Permite búsqueda flexible: "boby" encuentra "Boby", "Bob", etc.
     * Usa % antes y después del filtro: LIKE '%filtro%'
     * Solo mascotas activas (eliminado=FALSE).
     */
    private static final String SEARCH_BY_NAME_SQL = "SELECT m.id, m.nombre, m.especie, m.codigo_tag, m.microchip_id, " +
            "c.id AS mic_id, c.codigo_chip, c.marca " +
            "FROM mascotas m LEFT JOIN microchips c ON m.microchip_id = c.id " +
            "WHERE m.eliminado = FALSE AND (m.nombre LIKE ? OR m.especie LIKE ?)";

    /**
     * Query de búsqueda exacta por CodigoTag.
     * Usa comparación exacta (=) porque el CodigoTag es único (RN-001).
     * Usado por MascotaServiceImpl.validateCodigoTagUnique() para verificar unicidad.
     * Solo mascotas activas (eliminado=FALSE).
     */
    private static final String SEARCH_BY_TAG_SQL = "SELECT m.id, m.nombre, m.especie, m.codigo_tag, m.microchip_id, " +
            "c.id AS mic_id, c.codigo_chip, c.marca " +
            "FROM mascotas m LEFT JOIN microchips c ON m.microchip_id = c.id " +
            "WHERE m.eliminado = FALSE AND m.codigo_tag = ?";

    /**
     * DAO de microchips (actualmente no usado, pero disponible para operaciones futuras).
     */
    private final MicrochipDAO microchipDAO;

    /**
     * Constructor con inyección de MicrochipDAO.
     *
     * @param microchipDAO DAO de microchips
     * @throws IllegalArgumentException si microchipDAO es null
     */
    public MascotaDAO(MicrochipDAO microchipDAO) {
        if (microchipDAO == null) {
            throw new IllegalArgumentException("MicrochipDAO no puede ser null");
        }
        this.microchipDAO = microchipDAO;
    }

    /**
     * Inserta una mascota en la base de datos (versión sin transacción).
     *
     * @param mascota Mascota a insertar (id será ignorado y regenerado)
     * @throws Exception Si falla la inserción o no se obtiene ID generado
     */
    @Override
    public void insertar(Mascota mascota) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setMascotaParameters(stmt, mascota);
            stmt.executeUpdate();
            setGeneratedId(stmt, mascota);
        }
    }

    /**
     * Inserta una mascota dentro de una transacción existente.
     *
     * @param mascota Mascota a insertar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la inserción
     */
    @Override
    public void insertTx(Mascota mascota, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setMascotaParameters(stmt, mascota);
            stmt.executeUpdate();
            setGeneratedId(stmt, mascota);
        }
    }

    /**
     * Actualiza una mascota existente en la base de datos.
     *
     * @param mascota Mascota con los datos actualizados (id debe ser > 0)
     * @throws SQLException Si la mascota no existe o hay error de BD
     */
    @Override
    public void actualizar(Mascota mascota) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, mascota.getNombre());
            stmt.setString(2, mascota.getEspecie());
            stmt.setString(3, mascota.getCodigoTag());
            setMicrochipId(stmt, 4, mascota.getMicrochip());
            stmt.setInt(5, mascota.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar la mascota con ID: " + mascota.getId());
            }
        }
    }

    /**
     * Elimina lógicamente una mascota (soft delete).
     *
     * @param id ID de la mascota a eliminar
     * @throws SQLException Si la mascota no existe o hay error de BD
     */
    @Override
    public void eliminar(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró mascota con ID: " + id);
            }
        }
    }

    /**
     * Obtiene una mascota por su ID.
     * Incluye su microchip asociado mediante LEFT JOIN.
     *
     * @param id ID de la mascota a buscar
     * @return Mascota encontrada con su microchip, o null si no existe o está eliminada
     * @throws Exception Si hay error de BD
     */
    @Override
    public Mascota getById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMascota(rs);
                }
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener mascota por ID: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Obtiene todas las mascotas activas (eliminado=FALSE).
     * Incluye sus microchips mediante LEFT JOIN.
     *
     * @return Lista de mascotas activas con sus microchips (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Mascota> getAll() throws Exception {
        List<Mascota> mascotas = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                mascotas.add(mapResultSetToMascota(rs));
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener todas las mascotas: " + e.getMessage(), e);
        }
        return mascotas;
    }

    /**
     * Busca mascotas por nombre o especie con búsqueda flexible (LIKE).
     *
     * @param filtro Texto a buscar (no puede estar vacío)
     * @return Lista de mascotas que coinciden con el filtro (puede estar vacía)
     * @throws IllegalArgumentException Si el filtro está vacío
     * @throws SQLException Si hay error de BD
     */
    public List<Mascota> buscarPorNombreEspecie(String filtro) throws SQLException {
        if (filtro == null || filtro.trim().isEmpty()) {
            throw new IllegalArgumentException("El filtro de búsqueda no puede estar vacío");
        }

        List<Mascota> mascotas = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_NAME_SQL)) {

            String searchPattern = "%" + filtro + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mascotas.add(mapResultSetToMascota(rs));
                }
            }
        }
        return mascotas;
    }

    /**
     * Busca una mascota por CodigoTag exacto.
     *
     * @param codigoTag CodigoTag exacto a buscar (se aplica trim automáticamente)
     * @return Mascota con ese CodigoTag, o null si no existe o está eliminada
     * @throws IllegalArgumentException Si el CodigoTag está vacío
     * @throws SQLException Si hay error de BD
     */
    public Mascota buscarPorCodigoTag(String codigoTag) throws SQLException {
        if (codigoTag == null || codigoTag.trim().isEmpty()) {
            throw new IllegalArgumentException("El CodigoTag no puede estar vacío");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_TAG_SQL)) {

            stmt.setString(1, codigoTag.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMascota(rs);
                }
            }
        }
        return null;
    }

    /**
     * Setea los parámetros de mascota en un PreparedStatement.
     *
     * @param stmt PreparedStatement con INSERT_SQL
     * @param mascota Mascota con los datos a insertar
     * @throws SQLException Si hay error al setear parámetros
     */
    private void setMascotaParameters(PreparedStatement stmt, Mascota mascota) throws SQLException {
        stmt.setString(1, mascota.getNombre());
        stmt.setString(2, mascota.getEspecie());
        stmt.setString(3, mascota.getCodigoTag());
        setMicrochipId(stmt, 4, mascota.getMicrochip());
    }

    /**
     * Setea la FK microchip_id en un PreparedStatement.
     * Maneja correctamente el caso NULL (mascota sin microchip).
     *
     * @param stmt PreparedStatement
     * @param parameterIndex Índice del parámetro (1-based)
     * @param microchip Microchip asociado (puede ser null)
     * @throws SQLException Si hay error al setear el parámetro
     */
    private void setMicrochipId(PreparedStatement stmt, int parameterIndex, Microchip microchip) throws SQLException {
        if (microchip != null && microchip.getId() > 0) {
            stmt.setInt(parameterIndex, microchip.getId());
        } else {
            stmt.setNull(parameterIndex, Types.INTEGER);
        }
    }

    /**
     * Obtiene el ID autogenerado por la BD después de un INSERT.
     * Asigna el ID generado al objeto mascota.
     *
     * @param stmt PreparedStatement que ejecutó el INSERT con RETURN_GENERATED_KEYS
     * @param mascota Objeto mascota a actualizar con el ID generado
     * @throws SQLException Si no se pudo obtener el ID generado
     */
    private void setGeneratedId(PreparedStatement stmt, Mascota mascota) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                mascota.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("La inserción de la mascota falló, no se obtuvo ID generado");
            }
        }
    }

    /**
     * Mapea un ResultSet a un objeto Mascota.
     * Reconstruye la relación con Microchip usando LEFT JOIN.
     *
     * @param rs ResultSet posicionado en una fila con datos de mascota y microchip
     * @return Mascota reconstruida con su microchip (si tiene)
     * @throws SQLException Si hay error al leer columnas del ResultSet
     */
    private Mascota mapResultSetToMascota(ResultSet rs) throws SQLException {
        Mascota mascota = new Mascota();
        mascota.setId(rs.getInt("id"));
        mascota.setNombre(rs.getString("nombre"));
        mascota.setEspecie(rs.getString("especie"));
        mascota.setCodigoTag(rs.getString("codigo_tag"));

        // Manejo correcto de LEFT JOIN: verificar si microchip_id es NULL
        int microchipId = rs.getInt("microchip_id");
        if (microchipId > 0 && !rs.wasNull()) {
            Microchip microchip = new Microchip();
            microchip.setId(rs.getInt("mic_id"));
            microchip.setCodigoChip(rs.getString("codigo_chip"));
            microchip.setMarca(rs.getString("marca"));
            mascota.setMicrochip(microchip);
        }

        return mascota;
    }
}