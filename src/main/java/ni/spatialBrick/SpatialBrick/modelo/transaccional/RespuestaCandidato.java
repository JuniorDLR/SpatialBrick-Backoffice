package ni.spatialBrick.SpatialBrick.modelo.transaccional;

import ni.spatialBrick.SpatialBrick.modelo.configuracion.EjercicioCubos;
import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.*;

import javax.persistence.*;
import org.openxava.annotations.*;
import lombok.*;

@Embeddable
@Getter @Setter
public class RespuestaCandidato {

    @Required
    Integer numeroEjercicio;

    @Required
    @Column(length=10)
    @Enumerated(EnumType.STRING)
    OpcionRespuesta opcionElegida;

    public boolean esAcertada(EjercicioCubos ejercicio) {
        if (ejercicio != null) {
            return ejercicio.verificarRespuesta(this.opcionElegida);
        }
        return false;
    }
}
