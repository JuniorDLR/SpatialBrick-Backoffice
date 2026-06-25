package ni.spatialBrick.SpatialBrick.modelo.transaccional;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.*;
import ni.spatialBrick.SpatialBrick.modelo.configuracion.*;

import javax.persistence.*;
import org.openxava.annotations.*;
import lombok.*;
import java.util.Date;

@Entity
@Getter @Setter
public class Candidato {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Hidden
    int id;

    @Column(length=50)
    @Required
    String identificacion;

    @Column(length=100)
    @Required
    String nombreCompleto;

    @Column(length=50)
    @Required
    String nivelEducativo;

    @Required
    @Enumerated(EnumType.STRING)
    Genero genero;

    @Required
    Date fechaNacimiento;

    private static final int EDAD_MINIMA_BFA = 14;

    @javax.validation.constraints.AssertTrue(message = "El candidato debe tener la edad mínima requerida para aplicar la prueba BFA")
    private boolean isEdadValida() {
        if (fechaNacimiento == null) return true;
        java.time.LocalDate birth = new java.sql.Date(fechaNacimiento.getTime()).toLocalDate();
        java.time.LocalDate now = java.time.LocalDate.now();
        return java.time.Period.between(birth, now).getYears() >= EDAD_MINIMA_BFA;
    }

    public IntentoTest iniciarTest(TestLadrillosCubos test) {
        if (!isEdadValida()) {
            throw new IllegalStateException("El candidato no cumple la edad mínima para iniciar la prueba.");
        }
        IntentoTest intento = new IntentoTest();
        intento.setCandidato(this);
        intento.setTest(test);
        intento.setEstado(EstadoIntento.EN_PROGRESO);
        intento.setFechaHoraInicio(new Date());
        return intento;
    }
}
