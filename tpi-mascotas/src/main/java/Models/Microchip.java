package Models;

import java.util.Objects;

/**
 * Entidad que representa un microchip (identificación) en el sistema.
 * Hereda de Base para obtener id y eliminado.
 *
 * Relación con Mascota:
 * - Una Mascota puede tener 0 o 1 Microchip
 * - Un Microchip puede estar asociado a múltiples Mascotas (relación N:1 desde Mascota)
 *
 * Tabla BD: microchips
 * Campos:
 * - id: INT AUTO_INCREMENT PRIMARY KEY (heredado de Base)
 * - codigo_chip: VARCHAR(100) NOT NULL
 * - marca: VARCHAR(50) NOT NULL
 * - eliminado: BOOLEAN DEFAULT FALSE (heredado de Base)
 */
public class Microchip extends Base {
    /**
     * Código de identificación del microchip (ej: "900123456789012").
     * Requerido, no puede ser null ni estar vacío.
     */
    private String codigoChip;

    /**
     * Marca del fabricante del microchip.
     * Requerido, no puede ser null ni estar vacío.
     */
    private String marca;

    /**
     * Constructor completo para reconstruir un Microchip desde la base de datos.
     * Usado por MascotaDAO y MicrochipDAO al mapear ResultSet.
     *
     * @param id ID del microchip en la BD
     * @param codigoChip Código de identificación
     * @param marca Marca del fabricante
     */
    public Microchip(int id, String codigoChip, String marca) {
        super(id, false); // Llama al constructor de Base con eliminado=false
        this.codigoChip = codigoChip;
        this.marca = marca;
    }

    /**
     * Constructor por defecto para crear un microchip nuevo.
     * El ID será asignado por la BD al insertar.
     * El flag eliminado se inicializa en false por Base.
     */
    public Microchip() {
        super();
    }

    /**
     * Obtiene el código del microchip.
     * @return Código del microchip
     */
    public String getCodigoChip() {
        return codigoChip;
    }

    /**
     * Establece el código del microchip.
     * Validación: MicrochipServiceImpl verifica que no esté vacío.
     *
     * @param codigoChip Nuevo código del microchip
     */
    public void setCodigoChip(String codigoChip) {
        this.codigoChip = codigoChip;
    }

    /**
     * Obtiene la marca del microchip.
     * @return Marca del microchip
     */
    public String getMarca() {
        return marca;
    }

    /**
     * Establece la marca del microchip.
     * Validación: MicrochipServiceImpl verifica que no esté vacío.
     *
     * @param marca Nueva marca
     */
    public void setMarca(String marca) {
        this.marca = marca;
    }

    /**
     * Representación en texto del microchip.
     * Útil para debugging y logging.
     *
     * @return String con todos los campos del microchip
     */
    @Override
    public String toString() {
        return "Microchip{" +
                "id=" + getId() +
                ", codigoChip='" + codigoChip + '\'' +
                ", marca='" + marca + '\'' +
                ", eliminado=" + isEliminado() +
                '}';
    }

    /**
     * Compara dos microchips por igualdad SEMÁNTICA.
     * Dos microchips son iguales si tienen el mismo código.
     * Nota: NO se compara por ID, permitiendo detectar códigos duplicados.
     *
     * @param o Objeto a comparar
     * @return true si los microchips tienen el mismo código
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Microchip microchip = (Microchip) o;
        return Objects.equals(codigoChip, microchip.codigoChip);
    }

    /**
     * Calcula el hash code basado en el código del chip.
     * Consistente con equals(): microchips con mismo código tienen mismo hash.
     *
     * @return Hash code del microchip
     */
    @Override
    public int hashCode() {
        return Objects.hash(codigoChip);
    }
}