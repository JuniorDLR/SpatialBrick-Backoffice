package ni.spatialBrick.SpatialBrick.modelo.configuracion;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.OpcionRespuesta;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.ManyToOne;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import org.openxava.annotations.File;
import org.openxava.annotations.Hidden;
import org.openxava.annotations.Required;
import lombok.Getter;
import lombok.Setter;

import org.openxava.annotations.View;
import org.openxava.annotations.Tab;

@Entity
@View(members = "imagenMonton; opcionCorrecta")
@Tab(properties = "imagenMonton, opcionCorrecta")
@Getter @Setter
public class EjercicioCubos {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Hidden
    int id;

    @ManyToOne
    TestLadrillosCubos test;

    @Required(message = "Debe subir la imagen para este ejercicio")
    @File(acceptFileTypes=".png, .jpg, .jpeg, image/png, image/jpeg")
    @Column(length=32)
    String imagenMonton;

    @Required(message = "Debe seleccionar cuál es la opción correcta")
    @Column(length=10)
    @Enumerated(EnumType.STRING)
    OpcionRespuesta opcionCorrecta;


    @Hidden
    int valorAcierto = 1;

    public boolean verificarRespuesta(OpcionRespuesta opcionElegida) {
        return this.opcionCorrecta == opcionElegida;
    }

}

