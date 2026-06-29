package ni.spatialBrick.SpatialBrick.modelo.transaccional;

import ni.spatialBrick.SpatialBrick.modelo.configuracion.EjercicioCubos;
import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.*;
import javax.persistence.*;
import org.openxava.annotations.*;
import lombok.*;

@Embeddable
@Getter @Setter
public class RespuestaCandidato {

    @ManyToOne(fetch = FetchType.LAZY)
    @ReadOnly
    EjercicioCubos ejercicio;

    @Required
    @Column(length=10)
    @Enumerated(EnumType.STRING)
    OpcionRespuesta opcionElegida;
}
