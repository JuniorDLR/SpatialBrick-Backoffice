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
@Table(indexes = {
    @Index(name = "idx_intento_candidato", columnList = "candidato_id")
})
@View(members =
    "DatosDeEvaluacion [ candidato, test, modalidad ]; " +
    "Tiempos [ fechaPrueba, tiempoConsumido ]; " +
    "Resultados [ estado, puntuacionTotal, percentil ]; " +
    "AuditoriaPsicometrica [ cantidadAciertos, cantidadErrores, cantidadOmisiones ]"
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

    @ReadOnly
    int cantidadAciertos;

    @ReadOnly
    int cantidadErrores;

    @ReadOnly
    int cantidadOmisiones;

    @ReadOnly
    int percentil;

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
        int aciertos = 0;
        int errores = 0;
        int omisiones = 0;

        if (this.respuestas != null && this.test != null) {
            for (RespuestaCandidato respuesta : this.respuestas) {
                if (respuesta.getOpcionElegida() == OpcionRespuesta.OMITIDA) {
                    omisiones++;
                    continue;
                }
                
                EjercicioCubos ejercicio = this.test.obtenerEjercicio(respuesta.getNumeroEjercicio());
                if (respuesta.esAcertada(ejercicio)) {
                    puntaje = puntaje.add(new BigDecimal(ejercicio.getValorAcierto()));
                    aciertos++;
                } else {
                    errores++;
                }
            }
        }
        this.puntuacionTotal = puntaje;
        this.cantidadAciertos = aciertos;
        this.cantidadErrores = errores;
        this.cantidadOmisiones = omisiones;
    }
}
