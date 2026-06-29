package ni.spatialBrick.SpatialBrick.modelo.configuracion;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.EstadoIntento;
import ni.spatialBrick.SpatialBrick.modelo.transaccional.IntentoTest;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.PreRemove;
import javax.persistence.Table;
import javax.persistence.Index;
import javax.persistence.OrderColumn;
import javax.persistence.OneToMany;
import javax.persistence.Cacheable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import org.openxava.annotations.DefaultValueCalculator;
import org.openxava.annotations.Hidden;
import org.openxava.annotations.ListProperties;
import org.openxava.annotations.ReadOnly;
import org.openxava.annotations.SearchKey;
import org.openxava.annotations.Stereotype;
import org.openxava.annotations.View;
import org.openxava.annotations.Required;
import org.openxava.calculators.TrueCalculator;
import org.openxava.jpa.XPersistence;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(indexes = {
    @Index(name = "idx_test_codigo", columnList = "codigoTest", unique = true)
})
@View(members=
    "Configuracion [ activo, tiempoMinutos, tiempoSegundos ]; " +
    "instrucciones; " +
    "ejercicios"
)
@Getter @Setter
public class TestLadrillosCubos {

    private static final int MINUTOS_POR_DEFECTO = 3;
    private static final int SEGUNDOS_POR_DEFECTO = 30;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Hidden
    int id;

    @SearchKey
    @Column(length=32)
    @ReadOnly
    String codigoTest;

    @PrePersist
    @PreUpdate
    private void alGuardar() {
        aplicarTiempoPorDefectoSiEsVacio();
        generarCodigoTest();
    }

    @PreRemove
    private void alBorrar() {
        if (this.ejercicios != null) {
            for (EjercicioCubos ej : this.ejercicios) {
                ej.setTest(null);
            }
        }
    }

    private void aplicarTiempoPorDefectoSiEsVacio() {
        if (this.tiempoMinutos == 0 && this.tiempoSegundos == 0) {
            this.tiempoMinutos = MINUTOS_POR_DEFECTO;
            this.tiempoSegundos = SEGUNDOS_POR_DEFECTO;
        }
    }

    private void generarCodigoTest() {
        if (this.codigoTest == null || this.codigoTest.isEmpty()) {
            this.codigoTest = String.format("BFA-%03d", obtenerSiguienteId());
        }
    }

    private int obtenerSiguienteId() {
        Integer maxId = (Integer) XPersistence.getManager()
            .createQuery("select max(t.id) from TestLadrillosCubos t")
            .getSingleResult();
        return (maxId == null ? 0 : maxId) + 1;
    }

    @Required(message = "Las instrucciones del test son obligatorias")
    @Stereotype("HTML_TEXT")
    @Column(columnDefinition="TEXT")
    String instrucciones;

    @DefaultValueCalculator(TrueCalculator.class)
    @Column(columnDefinition="boolean default true", nullable = false)
    boolean activo = true;

    @Min(value = 0, message = "Los minutos no pueden ser negativos")
    int tiempoMinutos = MINUTOS_POR_DEFECTO;

    @Min(value = 0, message = "Los segundos no pueden ser negativos")
    @Max(value = 59, message = "Los segundos no pueden pasar de 59")
    int tiempoSegundos = SEGUNDOS_POR_DEFECTO;

    @Hidden
    @SuppressWarnings("unused")
    public int getTiempoLimiteSegundos() {
        return (tiempoMinutos * 60) + tiempoSegundos;
    }

    @Size(min = 1, message = "Debe agregar al menos un ejercicio (ladrillo) al test")
    @OneToMany(mappedBy="test", cascade=javax.persistence.CascadeType.MERGE)
    @OrderColumn(name = "orden_ejercicio")
    @ListProperties("opcionCorrecta, valorAcierto, imagenMonton")
    List<EjercicioCubos> ejercicios = new ArrayList<>();

    @SuppressWarnings("unused")
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
