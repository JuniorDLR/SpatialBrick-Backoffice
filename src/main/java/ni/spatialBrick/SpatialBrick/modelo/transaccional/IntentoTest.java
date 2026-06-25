package ni.spatialBrick.SpatialBrick.modelo.transaccional;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.*;
import ni.spatialBrick.SpatialBrick.modelo.configuracion.*;

import javax.persistence.*;
import org.openxava.annotations.*;
import lombok.*;
import java.util.Date;
import java.util.Collection;

@Entity
@Getter @Setter
public class IntentoTest {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Hidden
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties="nombreCompleto")
    @Required
    Candidato candidato;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties="codigoTest")
    @Required
    TestLadrillosCubos test;

    @ReadOnly
    @DefaultValueCalculator(org.openxava.calculators.CurrentTimestampCalculator.class)
    Date fechaHoraInicio;

    @ReadOnly
    Date fechaHoraFin;

    @ReadOnly
    int puntuacionTotal;

    @ReadOnly
    @Enumerated(EnumType.STRING)
    EstadoIntento estado = EstadoIntento.EN_PROGRESO;

    @javax.validation.constraints.AssertTrue(message = "El intento ha sido invalidado: El tiempo excede el límite configurado para el test.")
    private boolean isTiempoValido() {
        if (fechaHoraInicio == null || fechaHoraFin == null || test == null) return true;
        long diferenciaMilisegundos = Math.abs(fechaHoraFin.getTime() - fechaHoraInicio.getTime());
        long diferenciaSegundos = java.util.concurrent.TimeUnit.SECONDS.convert(diferenciaMilisegundos, java.util.concurrent.TimeUnit.MILLISECONDS);
        return diferenciaSegundos <= test.getTiempoLimiteSegundos();
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
        this.fechaHoraFin = new Date();
        this.estado = EstadoIntento.FINALIZADO;
        calcularPuntuacionFinal();
    }

    private void calcularPuntuacionFinal() {
        int puntaje = 0;
        if (this.respuestas != null && this.test != null) {
            for (RespuestaCandidato respuesta : this.respuestas) {
                EjercicioCubos ejercicio = this.test.obtenerEjercicio(respuesta.getNumeroEjercicio());
                if (respuesta.esAcertada(ejercicio)) {
                    puntaje += ejercicio.getValorAcierto();
                }
            }
        }
        this.puntuacionTotal = puntaje;
    }
}
