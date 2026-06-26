package ni.spatialBrick.SpatialBrick.modelo.transaccional;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.*;
import ni.spatialBrick.SpatialBrick.modelo.configuracion.*;

import javax.persistence.*;
import org.openxava.annotations.*;
import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.*;
import lombok.*;
import java.util.Date;

@Entity
@Table(indexes = {
    @Index(name = "idx_candidato_identificacion", columnList = "identificacion", unique = true)
})
@View(members = 
    "General [ identificacion; nombreCompleto; nivelEducativo; genero, fechaNacimiento; foto ]; " +
    "PerfilProfesional [ puestoAplica, profesion ]; " +
    "Contacto [ email, telefono ]"
)
@Tab(properties="identificacion, nombreCompleto, edadCalculada, email, fechaRegistro")
@Getter @Setter
public class Candidato {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Hidden
    int id;

    @Stereotype("PHOTO")
    byte[] foto;

    @Column(length=50, unique=true)
    @Required
    String identificacion;

    @Column(length=100)
    @DisplaySize(50)
    @Required
    String nombreCompleto;

    @Column(length=50)
    @Required
    String nivelEducativo;

    @Required
    @Enumerated(EnumType.STRING)
    Genero genero;

    @Required
    @javax.validation.constraints.Past(message = "La fecha de nacimiento debe estar en el pasado")
    Date fechaNacimiento;

    @Column(length=100)
    @javax.validation.constraints.Email(message = "Debe ingresar un correo electrónico válido")
    String email;

    @Column(length=20)
    String telefono;

    @Required
    @Enumerated(EnumType.STRING)
    Puesto puestoAplica;

    @Column(length=100)
    @DisplaySize(50)
    String profesion;

    @ReadOnly
    @DefaultValueCalculator(org.openxava.calculators.CurrentTimestampCalculator.class)
    Date fechaRegistro;

    @Depends("fechaNacimiento")
    public int getEdadCalculada() {
        if (fechaNacimiento == null) return 0;
        java.time.LocalDate birth = new java.sql.Date(fechaNacimiento.getTime()).toLocalDate();
        java.time.LocalDate now = java.time.LocalDate.now();
        return java.time.Period.between(birth, now).getYears();
    }

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
        intento.setFechaPrueba(new Date());
        return intento;
    }
}
