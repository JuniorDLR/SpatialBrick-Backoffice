package ni.spatialBrick.SpatialBrick.modelo.transaccional;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.EstadoIntento;
import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.ModalidadTest;
import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.OpcionRespuesta;
import ni.spatialBrick.SpatialBrick.modelo.configuracion.EjercicioCubos;
import ni.spatialBrick.SpatialBrick.modelo.configuracion.TestLadrillosCubos;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.NoResultException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.persistence.OrderColumn;
import org.openxava.validators.ValidationException;

import org.openxava.annotations.DefaultValueCalculator;
import org.openxava.annotations.OnChange;
import org.openxava.annotations.DescriptionsList;
import org.openxava.annotations.Hidden;
import org.openxava.annotations.ListProperties;
import org.openxava.annotations.ReadOnly;
import org.openxava.annotations.Required;
import org.openxava.annotations.Stereotype;
import org.openxava.annotations.View;
import org.openxava.annotations.Tab;
import org.openxava.annotations.EditOnly;
import org.openxava.calculators.CurrentTimestampCalculator;
import org.openxava.jpa.XPersistence;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "intento_test", indexes = {
    @Index(name = "idx_intento_candidato", columnList = "candidato_id"),
    @Index(name = "idx_intento_test", columnList = "test_id"),
    @Index(name = "idx_intento_estado", columnList = "estado")
})
@View(members =
    "DatosDeEvaluacion [ candidato, test ]; " +
    "Tiempos [ fechaPrueba, minutosConsumidos, segundosConsumidos ]; " +
    "Resultados [ estado, puntuacionTotal, percentil ]; " +
    "AuditoriaPsicometrica [ cantidadAciertos, cantidadErrores, cantidadOmisiones ]; " +
    "respuestas"
)
@Tab(properties="candidato.nombreCompleto, test.codigoTest, puntuacionTotal, percentil, fechaPrueba, estado")
@Getter @Setter
public class IntentoTest {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Hidden
    int id;

    @Hidden
    @Column(length=10)
    @Enumerated(EnumType.STRING)
    ModalidadTest modalidad = ModalidadTest.DIGITAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties="nombreCompleto")
    @Required
    Candidato candidato;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties="codigoTest")
    @OnChange(ni.spatialBrick.SpatialBrick.acciones.CargarRespuestasDeTestAccion.class)
    @Required
    TestLadrillosCubos test;

    @Stereotype("DATETIME")
    @DefaultValueCalculator(CurrentTimestampCalculator.class)
    Date fechaPrueba;

    @Stereotype("DATETIME")
    @Hidden
    Date ultimaModificacion;

    @Min(value = 0, message = "Los minutos no pueden ser negativos")
    int minutosConsumidos = 0;

    @Min(value = 0, message = "Los segundos no pueden ser negativos")
    @Max(value = 59, message = "Los segundos no pueden pasar de 59")
    int segundosConsumidos = 0;

    @ReadOnly
    BigDecimal puntuacionTotal;

    @ReadOnly
    int cantidadAciertos;

    @ReadOnly
    int cantidadErrores;

    @ReadOnly
    int cantidadOmisiones;

    @ReadOnly
    int percentil;

    @Required(message = "El estado del intento es obligatorio (EN_PROGRESO o FINALIZADO)")
    @Column(length=15)
    @Enumerated(EnumType.STRING)
    EstadoIntento estado = EstadoIntento.EN_PROGRESO;

    @PrePersist
    @PreUpdate
    private void validarYActualizarFechas() {
        this.ultimaModificacion = new Date();
        
        if (this.test != null) {
            int tiempoLimite = this.test.getTiempoLimiteSegundos();
            int tiempoUsado = (this.minutosConsumidos * 60) + this.segundosConsumidos;
            if (tiempoUsado > tiempoLimite) {
                throw new ValidationException(
                    "El tiempo consumido no puede ser mayor al tiempo límite del test (" + 
                    this.test.getTiempoMinutos() + " min " + this.test.getTiempoSegundos() + " seg)."
                );
            }
        }

        if (this.estado == EstadoIntento.FINALIZADO) {
            if (this.respuestas == null || this.respuestas.isEmpty()) {
                throw new ValidationException("No se puede guardar un intento FINALIZADO si no tiene respuestas ingresadas.");
            }
        }
    }

    @ElementCollection
    @ListProperties("ejercicio.imagenMonton, opcionElegida")
    @EditOnly
    @OrderColumn(name = "orden")
    List<RespuestaCandidato> respuestas = new ArrayList<>();

    @SuppressWarnings("unused")
    public void agregarRespuesta(RespuestaCandidato respuesta) {
        if (this.estado == EstadoIntento.FINALIZADO) {
            throw new IllegalStateException("No se pueden agregar respuestas a un test finalizado.");
        }
        this.respuestas.add(respuesta);
    }

    public void calcularPuntuacionFinal() {
        if (this.estado != EstadoIntento.FINALIZADO) {
            reiniciarResultados();
            return;
        }

        contarYCalificarRespuestas();

        int totalEjercicios = (this.test != null && this.test.getEjercicios() != null)
                ? this.test.getEjercicios().size()
                : 0;
        int puntajeReal = (totalEjercicios > 0)
                ? (int) Math.round((this.cantidadAciertos * 27.0) / totalEjercicios)
                : 0;

        this.puntuacionTotal = new BigDecimal(puntajeReal);
        this.percentil = consultarPercentil(puntajeReal);
    }

    private void reiniciarResultados() {
        this.puntuacionTotal = BigDecimal.ZERO;
        this.cantidadAciertos = 0;
        this.cantidadErrores = 0;
        this.cantidadOmisiones = 0;
        this.percentil = 0;
    }

    private void contarYCalificarRespuestas() {
        int aciertos = 0;
        int errores = 0;
        int omisiones = 0;

        if (this.respuestas != null) {
            for (RespuestaCandidato respuesta : this.respuestas) {
                if (respuesta.getOpcionElegida() == OpcionRespuesta.OMITIDA) {
                    omisiones++;
                    continue;
                }

                EjercicioCubos ejercicio = respuesta.getEjercicio();
                if (ejercicio != null && ejercicio.verificarRespuesta(respuesta.getOpcionElegida())) {
                    aciertos++;
                } else if (ejercicio != null) {
                    errores++;
                }
            }
        }

        // puntuacionTotal se asigna en calcularPuntuacionFinal() tras aplicar la fórmula
        this.cantidadAciertos = aciertos;
        this.cantidadErrores = errores;
        this.cantidadOmisiones = omisiones;
    }

    private int consultarPercentil(int aciertos) {
        try {
            Integer resultado = (Integer) XPersistence.getManager()
                .createQuery(
                    "select b.percentil from BaremoLadrillosCubos b " +
                    "where :aciertos between b.puntuacionMinima and b.puntuacionMaxima"
                )
                .setParameter("aciertos", aciertos)
                .setMaxResults(1)
                .getSingleResult();
            return (resultado != null) ? resultado : 0;
        } catch (NoResultException e) {
            return 0;
        }
    }
}
