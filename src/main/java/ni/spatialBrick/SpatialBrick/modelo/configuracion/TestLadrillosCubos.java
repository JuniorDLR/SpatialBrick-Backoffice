package ni.spatialBrick.SpatialBrick.modelo.configuracion;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.EstadoIntento;
import ni.spatialBrick.SpatialBrick.modelo.transaccional.*;

import javax.persistence.*;
import org.openxava.annotations.*;
import lombok.*;
import java.util.Collection;
import java.math.BigDecimal;

@Entity
@View(members=
    "instrucciones; " +
    "ejercicios"
)
@Getter @Setter
public class TestLadrillosCubos {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Hidden
    int id;

    @Column(length=32)
    @ReadOnly
    String codigoTest;

    @PrePersist
    private void generarCodigo() {
        if (this.codigoTest == null || this.codigoTest.isEmpty()) {
            Integer maxId = (Integer) org.openxava.jpa.XPersistence.getManager()
                .createQuery("select max(t.id) from TestLadrillosCubos t")
                .getSingleResult();
            int nextId = (maxId == null ? 0 : maxId) + 1;
            this.codigoTest = String.format("BFA-%03d", nextId);
        }
    }

    @Stereotype("HTML_TEXT")
    String instrucciones;

    public static final int TIEMPO_LIMITE_DEFECTO = 210;

    int tiempoLimiteSegundos = TIEMPO_LIMITE_DEFECTO;

    @ElementCollection
    @ListProperties("numeroEjercicio, opcionCorrecta, valorAcierto")
    Collection<EjercicioCubos> ejercicios = new java.util.ArrayList<>();

    public EjercicioCubos obtenerEjercicio(int numero) {
        if (this.ejercicios == null) return null;
        for (EjercicioCubos ej : this.ejercicios) {
            if (ej.getNumeroEjercicio() == numero) {
                return ej;
            }
        }
        return null;
    }

    public BigDecimal evaluarIntento(IntentoTest intento) {
        if (intento != null && intento.getEstado() == EstadoIntento.FINALIZADO) {
            return intento.getPuntuacionTotal();
        }
        return BigDecimal.ZERO;
    }
}
