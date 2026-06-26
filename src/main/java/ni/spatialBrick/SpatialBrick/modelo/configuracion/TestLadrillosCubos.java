package ni.spatialBrick.SpatialBrick.modelo.configuracion;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.EstadoIntento;
import ni.spatialBrick.SpatialBrick.modelo.transaccional.*;

import javax.persistence.*;
import org.openxava.annotations.*;
import lombok.*;
import java.util.List;
import java.math.BigDecimal;

@Entity
@View(members=
    "Configuracion [ activo, tiempoMinutos, tiempoSegundos ]; " +
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
    @PreUpdate
    private void alGuardar() {

        if (this.getTiempoLimiteSegundos() <= 0) {
            this.tiempoMinutos = 3;
            this.tiempoSegundos = 30;
        }

        if (this.codigoTest == null || this.codigoTest.isEmpty()) {
            Integer maxId = (Integer) org.openxava.jpa.XPersistence.getManager()
                .createQuery("select max(t.id) from TestLadrillosCubos t")
                .getSingleResult();
            int nextId = (maxId == null ? 0 : maxId) + 1;
            this.codigoTest = String.format("BFA-%03d", nextId);
        }

        // Auto-incrementar el número de ejercicio garantizando el orden
        if (this.ejercicios != null) {
            int contador = 1;
            for (EjercicioCubos ej : this.ejercicios) {
                ej.setNumeroEjercicio(contador++);
            }
        }
    }

    @Required(message = "Las instrucciones del test son obligatorias")
    @Stereotype("HTML_TEXT")
    @Column(columnDefinition="TEXT")
    String instrucciones;

    @DefaultValueCalculator(org.openxava.calculators.TrueCalculator.class)
    @Column(columnDefinition="boolean default true", nullable = false)
    boolean activo = true;

    @javax.validation.constraints.Min(value = 0, message = "Los minutos no pueden ser negativos")
    int tiempoMinutos = 3;

    @javax.validation.constraints.Min(value = 0, message = "Los segundos no pueden ser negativos")
    @javax.validation.constraints.Max(value = 59, message = "Los segundos no pueden pasar de 59")
    int tiempoSegundos = 30;

    @Hidden
    public int getTiempoLimiteSegundos() {
        return (tiempoMinutos * 60) + tiempoSegundos;
    }

    @javax.validation.constraints.Size(min = 1, message = "Debe agregar al menos un ejercicio (ladrillo) al test")
    @ElementCollection
    @OrderColumn(name="orden_ejercicio")
    @ListProperties("opcionCorrecta, valorAcierto, urlImagenMonton")
    List<EjercicioCubos> ejercicios = new java.util.ArrayList<>();

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
    @Override
    public String toString() {
        return codigoTest != null ? "Prueba " + codigoTest : "Prueba Espacial";
    }
}
