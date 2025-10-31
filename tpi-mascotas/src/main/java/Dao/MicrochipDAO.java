package Dao;

import Config.DatabaseConnection;
import Models.Microchip;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Microchip.
 * Gestiona todas las operaciones de persistencia de microchips en la base de datos.
 *
 * Características:
 * - Implementa GenericDAO<Microchip> para operaciones CRUD estándar
 * - Usa PreparedStatements en TODAS las consultas (protección contra SQL injection)
 * - Implementa soft delete (eliminado=TRUE, no DELETE físico)
 * - Soporta transacciones mediante insertTx() (recibe Connection externa)
 *
 * Patrón: DAO con try-with-resources para manejo automático de recursos JDBC
 */
public class MicrochipDAO implements GenericDAO<Microchip> {
    /**
     * Query de inserción de microchip.
     * Inserta codigo_chip y marca.
     * El id es AUTO_INCREMENT y se obtiene con RETURN_GENERATED_KEYS.
     */
    private static final String INSERT_SQL = "INSERT INTO microchips (codigo_chip, marca) VALUES (?, ?)";

    /**
     * Query de actualización de microchip.
     * Actualiza codigo_chip y marca por id.
     *
     * ⚠️ IMPORTANTE: Si varias mascotas comparten este microchip,
     * la actualización los afectará a TODAS (RN-040).
     */
    private static final String UPDATE_SQL = "UPDATE microchips SET codigo_chip = ?, marca = ? WHERE id = ?";

    /**
     * Query de soft delete.
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     *
     * ⚠️ PELIGRO: Este método NO verifica si hay mascotas asociadas.
     * Puede dejar FKs huérfanas (mascotas.microchip_id apuntando a microchip eliminado).
     * ALTERNATIVA SEGURA: MascotaServiceImpl.eliminarMicrochipDeMascota()
     */
    private static final String DELETE_SQL = "UPDATE microchips SET eliminado = TRUE WHERE id = ?";

    /**
     * Query para obtener microchip por ID.
     * Solo retorna microchips activos (eliminado=FALSE).
     */
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM microchips WHERE id = ? AND eliminado = FALSE";

    /**
     * Query para obtener todos los microchips activos.
     * Filtra por eliminado=FALSE (solo microchips activos).
     */
    private static final String SELECT_ALL_SQL = "SELECT * FROM microchips WHERE eliminado = FALSE";

    /**
     * Inserta un microchip en la base de datos (versión sin transacción).
     * Crea su propia conexión y la cierra automáticamente.
     *
     * @param microchip Microchip a insertar (id será ignorado y regenerado)
     * @throws SQLException Si falla la inserción o no se obtiene ID generado
     */
    @Override
    public void insertar(Microchip microchip) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setMicrochipParameters(stmt, microchip);
            stmt.executeUpdate();

            setGeneratedId(stmt, microchip);
        }
    }

    /**
     * Inserta un microchip dentro de una transacción existente.
     * NO crea nueva conexión, recibe una Connection externa.
     * NO cierra la conexión (responsabilidad del caller con TransactionManager).
     *
     * @param microchip Microchip a insertar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la inserción
     */
    @Override
    public void insertTx(Microchip microchip, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setMicrochipParameters(stmt, microchip);
            stmt.executeUpdate();
            setGeneratedId(stmt, microchip);
        }
    }

    /**
     * Actualiza un microchip existente en la base de datos.
     *
     * ⚠️ IMPORTANTE: Si varias mascotas comparten este microchip,
     * la actualización los afectará a TODAS (RN-040).
     *
     * @param microchip Microchip con los datos actualizados (id debe ser > 0)
     * @throws SQLException Si el microchip no existe o hay error de BD
     */
    @Override
    public void actualizar(Microchip microchip) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, microchip.getCodigoChip());
            stmt.setString(2, microchip.getMarca());
            stmt.setInt(3, microchip.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el microchip con ID: " + microchip.getId());
            }
        }
    }

    /**
     * Elimina lógicamente un microchip (soft delete).
     *
     * ⚠️ PELIGRO: Este método NO verifica si hay mascotas asociadas (RN-029).
     * Si hay mascotas con mascotas.microchip_id apuntando a este microchip,
     * quedarán con FK huérfana (apuntando a un microchip eliminado).
     *
     * ALTERNATIVA SEGURA: MascotaServiceImpl.eliminarMicrochipDeMascota()
     *
     * @param id ID del microchip a eliminar
     * @throws SQLException Si el microchip no existe o hay error de BD
     */
    @Override
    public void eliminar(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró microchip con ID: " + id);
            }
        }
    }

    /**
     * Obtiene un microchip por su ID.
     * Solo retorna microchips activos (eliminado=FALSE).
     *
     * @param id ID del microchip a buscar
     * @return Microchip encontrado, o null si no existe o está eliminado
     * @throws SQLException Si hay error de BD
     */
    @Override
    public Microchip getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMicrochip(rs);
                }
            }
        }
        return null;
    }

    /**
     * Obtiene todos los microchips activos (eliminado=FALSE).
     *
     * @return Lista de microchips activos (puede estar vacía)
     * @throws SQLException Si hay error de BD
     */
    @Override
    public List<Microchip> getAll() throws SQLException {
        List<Microchip> microchips = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                microchips.add(mapResultSetToMicrochip(rs));
            }
        }

        return microchips;
    }

    /**
     * Setea los parámetros de microchip en un PreparedStatement.
     *
     * @param stmt PreparedStatement con INSERT_SQL
     * @param microchip Microchip con los datos a insertar
     * @throws SQLException Si hay error al setear parámetros
     */
    private void setMicrochipParameters(PreparedStatement stmt, Microchip microchip) throws SQLException {
        stmt.setString(1, microchip.getCodigoChip());
        stmt.setString(2, microchip.getMarca());
    }

    /**
     * Obtiene el ID autogenerado por la BD después de un INSERT.
     * Asigna el ID generado al objeto microchip.
     *
     * @param stmt PreparedStatement que ejecutó el INSERT con RETURN_GENERATED_KEYS
     * @param microchip Objeto microchip a actualizar con el ID generado
     * @throws SQLException Si no se pudo obtener el ID generado
     */
    private void setGeneratedId(PreparedStatement stmt, Microchip microchip) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                microchip.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("La inserción del microchip falló, no se obtuvo ID generado");
            }
        }
    }

    /**
     * Mapea un ResultSet a un objeto Microchip.
     *
     * @param rs ResultSet posicionado en una fila con datos de microchip
     * @return Microchip reconstruido
     * @throws SQLException Si hay error al leer columnas del ResultSet
     */
    private Microchip mapResultSetToMicrochip(ResultSet rs) throws SQLException {
        return new Microchip(
                rs.getInt("id"),
                rs.getString("codigo_chip"),
                rs.getString("marca")
        );
    }
}