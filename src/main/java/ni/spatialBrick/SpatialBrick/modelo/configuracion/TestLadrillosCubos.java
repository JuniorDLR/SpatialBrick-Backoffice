package ni.spatialBrick.SpatialBrick.modelo.configuracion;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.EstadoIntento;
import ni.spatialBrick.SpatialBrick.modelo.transaccional.*;

import javax.persistence.*;
import org.openxava.annotations.*;
import lombok.*;
import java.util.Collection;

@Entity
@Getter @Setter
public class TestLadrillosCubos {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Hidden
    int id;

    @Column(length=32)
    @Required
    String codigoTest;

    @Stereotype("TEXT_AREA")
    String instrucciones;

    public static final int TIEMPO_LIMITE_DEFECTO = 210;

    int tiempoLimiteSegundos = TIEMPO_LIMITE_DEFECTO;

    @ElementCollection
    @ListProperties("numeroEjercicio, opcionCorrecta, valorAcierto")
    Collection<EjercicioCubos> ejercicios = new java.util.ArrayList<>();

    public EjercicioCubos obtenerEjercicio(int numero) {
        if (this.ejercicios == null) return null;
        for (EjercicioCubos ej : this.ejercicios) {
            if (ej.getNumeroEjercicio() == numero) {
                return ej;
            }
        }
        return null;
    }

    public int evaluarIntento(IntentoTest intento) {
        if (intento != null && intento.getEstado() == EstadoIntento.FINALIZADO) {
            return intento.getPuntuacionTotal();
        }
        return 0;
    }
}
