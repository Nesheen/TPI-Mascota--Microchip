package Models;

import java.util.Objects;

/**
 * Entidad que representa una mascota en el sistema.
 * Hereda de Base para obtener id y eliminado.
 *
 * Relación con Microchip:
 * - Una Mascota puede tener 0 o 1 Microchip (relación opcional)
 * - Se relaciona mediante FK microchip_id en la tabla mascotas
 *
 * Tabla BD: mascotas
 * Campos:
 * - id: INT AUTO_INCREMENT PRIMARY KEY (heredado de Base)
 * - nombre: VARCHAR(50) NOT NULL
 * - especie: VARCHAR(50) NOT NULL
 * - codigo_tag: VARCHAR(20) NOT NULL UNIQUE (análogo a DNI, RN-001)
 * - microchip_id: INT NULL (FK a microchips)
 * - eliminado: BOOLEAN DEFAULT FALSE (heredado de Base)
 */
public class Mascota extends Base {
    /** Nombre de la mascota. Requerido, no puede ser null ni vacío. */
    private String nombre;

    /** Especie de la mascota (ej: "Perro", "Gato"). Requerido. */
    private String especie;

    /**
     * Código de Tag/Chapa (identificador único de la mascota).
     * Requerido, no puede ser null ni vacío.
     * ÚNICO en el sistema (validado en BD y en MascotaServiceImpl.validateCodigoTagUnique()).
     */
    private String codigoTag;

    /**
     * Microchip asociado a la mascota.
     * Puede ser null (mascota sin microchip).
     * Se carga mediante LEFT JOIN en MascotaDAO.
     */
    private Microchip microchip;

    /**
     * Constructor completo para reconstruir una Mascota desde la BD.
     * Usado por MascotaDAO al mapear ResultSet.
     * El microchip se asigna posteriormente con setMicrochip().
     */
    public Mascota(int id, String nombre, String especie, String codigoTag) {
        super(id, false);
        this.nombre = nombre;
        this.especie = especie;
        this.codigoTag = codigoTag;
    }

    /** Constructor por defecto para crear una mascota nueva sin ID. */
    public Mascota() {
        super();
    }

    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre de la mascota.
     * Validación: MascotaServiceImpl verifica que no esté vacío.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEspecie() {
        return especie;
    }

    /**
     * Establece la especie de la mascota.
     * Validación: MascotaServiceImpl verifica que no esté vacío.
     */
    public void setEspecie(String especie) {
        this.especie = especie;
    }

    public String getCodigoTag() {
        return codigoTag;
    }

    /**
     * Establece el Código de Tag de la mascota.
     * Validación: MascotaServiceImpl verifica que sea único en insert/update.
     */
    public void setCodigoTag(String codigoTag) {
        this.codigoTag = codigoTag;
    }

    public Microchip getMicrochip() {
        return microchip;
    }

    /**
     * Asocia o desasocia un microchip a la mascota.
     * Si microchip es null, la FK microchip_id será NULL en la BD.
     */
    public void setMicrochip(Microchip microchip) {
        this.microchip = microchip;
    }

    @Override
    public String toString() {
        return "Mascota{" +
                "id=" + getId() +
                ", nombre='" + nombre + '\'' +
                ", especie='" + especie + '\'' +
                ", codigoTag='" + codigoTag + '\'' +
                ", microchip=" + microchip +
                ", eliminado=" + isEliminado() +
                '}';
    }

    /**
     * Compara dos mascotas por codigoTag (identificador único).
     * Dos mascotas son iguales si tienen el mismo codigoTag.
     * Correcto porque codigoTag es único en el sistema.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mascota mascota = (Mascota) o;
        return Objects.equals(codigoTag, mascota.codigoTag);
    }

    /**
     * Hash code basado en codigoTag.
     * Consistente con equals(): mascotas con mismo codigoTag tienen mismo hash.
     */
    @Override
    public int hashCode() {
        return Objects.hash(codigoTag);
    }
}