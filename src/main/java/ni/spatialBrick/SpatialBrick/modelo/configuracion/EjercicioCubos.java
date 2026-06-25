package ni.spatialBrick.SpatialBrick.modelo.configuracion;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.*;

import javax.persistence.*;
import org.openxava.annotations.*;
import lombok.*;

@Embeddable
@Getter @Setter
public class EjercicioCubos {

    int numeroEjercicio;

    @File
    @Column(length=32)
    String urlImagenMonton;

    @Enumerated(EnumType.STRING)
    OpcionRespuesta opcionCorrecta;

    int valorAcierto;

    public boolean verificarRespuesta(OpcionRespuesta opcionElegida) {
        return this.opcionCorrecta == opcionElegida;
    }
}
