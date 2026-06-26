package ni.spatialBrick.SpatialBrick.modelo.transaccional;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.*;
import ni.spatialBrick.SpatialBrick.modelo.configuracion.*;

import javax.persistence.*;
import org.openxava.annotations.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Collection;

@Entity
@View(members =
    "DatosDeEvaluacion [ candidato, test ]; " +
    "Tiempos [ fechaPrueba, tiempoConsumido ]; " +
    "Resultados [ estado, puntuacionTotal ]"
)
@Getter @Setter
public class IntentoTest {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Hidden
    int id;

    @Hidden
    @Enumerated(EnumType.STRING)
    ModalidadTest modalidad = ModalidadTest.PAPEL;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties="nombreCompleto")
    @Required
    Candidato candidato;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties="codigoTest")
    @Required
    TestLadrillosCubos test;

    @Temporal(TemporalType.TIMESTAMP)
    @DefaultValueCalculator(org.openxava.calculators.CurrentDateCalculator.class)
    Date fechaPrueba;

    @Enumerated(EnumType.STRING)
    DuracionMinutos tiempoConsumido;

    BigDecimal puntuacionTotal;

    @Enumerated(EnumType.STRING)
    EstadoIntento estado = EstadoIntento.FINALIZADO;

    @javax.validation.constraints.AssertTrue(message = "El intento ha sido invalidado: El tiempo consumido excede el límite configurado para el test.")
    private boolean isTiempoValido() {
        if (modalidad == ModalidadTest.PAPEL) return true; // Flexible para entrada manual
        if (tiempoConsumido == null || test == null) return true;
        return (tiempoConsumido.getValor() * 60) <= test.getTiempoLimiteSegundos();
    }

    @ElementCollection
    @ListProperties("numeroEjercicio, opcionElegida")
    Collection<RespuestaCandidato> respuestas = new java.util.ArrayList<>();

    public void agregarRespuesta(RespuestaCandidato respuesta) {
        if (this.estado == EstadoIntento.FINALIZADO) {
            throw new IllegalStateException("No se pueden agregar respuestas a un test finalizado.");
        }
        this.respuestas.add(respuesta);
    }

    public void finalizarTest() {
        if (this.estado == EstadoIntento.FINALIZADO) {
            throw new IllegalStateException("El test ya se encuentra finalizado.");
        }
        this.estado = EstadoIntento.FINALIZADO;
        calcularPuntuacionFinal();
    }

    private void calcularPuntuacionFinal() {
        BigDecimal puntaje = BigDecimal.ZERO;
        if (this.respuestas != null && this.test != null) {
            for (RespuestaCandidato respuesta : this.respuestas) {
                EjercicioCubos ejercicio = this.test.obtenerEjercicio(respuesta.getNumeroEjercicio());
                if (respuesta.esAcertada(ejercicio)) {
                    puntaje = puntaje.add(new BigDecimal(ejercicio.getValorAcierto()));
                }
            }
        }
        this.puntuacionTotal = puntaje;
    }
}
