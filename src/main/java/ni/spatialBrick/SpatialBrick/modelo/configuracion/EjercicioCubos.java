package ni.spatialBrick.SpatialBrick.modelo.configuracion;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.OpcionRespuesta;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import org.openxava.annotations.File;
import org.openxava.annotations.Hidden;
import org.openxava.annotations.Required;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;

@Embeddable
@Getter @Setter
public class EjercicioCubos {

    @Hidden
    int numeroEjercicio;

    @Required(message = "Debe subir la imagen para este ejercicio")
    @File(acceptFileTypes="image/*")
    @Column(length=32)
    String imagenMonton;

    @Required(message = "Debe seleccionar cuál es la opción correcta")
    @Enumerated(EnumType.STRING)
    OpcionRespuesta opcionCorrecta;

    @Min(value = 1, message = "El valor del acierto debe ser mayor a 0")
    int valorAcierto;

    public boolean verificarRespuesta(OpcionRespuesta opcionElegida) {
        return this.opcionCorrecta == opcionElegida;
    }

    @AssertTrue(message = "La opción correcta no puede configurarse como 'OMITIDA'")
    private boolean isOpcionValida() {
        return this.opcionCorrecta != OpcionRespuesta.OMITIDA;
    }
}
