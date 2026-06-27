package ni.spatialBrick.SpatialBrick.modelo.transaccional;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.*;
import ni.spatialBrick.SpatialBrick.modelo.configuracion.*;
import javax.persistence.*;
import javax.validation.ValidationException;
import javax.validation.constraints.*;
import org.openxava.annotations.*;
import org.openxava.jpa.XPersistence;
import lombok.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.Date;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.NumberParseException;

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

    private static final int EDAD_MINIMA_BFA = 14;
    private static final String REGION_POR_DEFECTO = "NI";

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Hidden
    int id;

    @File(acceptFileTypes="image/*")
    @Column(length=32)
    String foto;

    @Column(length=50, unique=true)
    @Required(message = "La identificación es obligatoria")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "La identificación solo puede contener letras, números y guiones (ej. Pasaportes, DNI, etc.)")
    String identificacion;

    @Column(length=100)
    @DisplaySize(50)
    @Required
    String nombreCompleto;

    @Enumerated(EnumType.STRING)
    NivelEducativo nivelEducativo;

    @Enumerated(EnumType.STRING)
    Genero genero;

    @Required
    @Temporal(TemporalType.DATE)
    @Past(message = "La fecha de nacimiento debe estar en el pasado")
    Date fechaNacimiento;

    @Required(message = "El correo electrónico es obligatorio para contactar al candidato")
    @Column(length=100, unique=true)
    @Email(message = "Debe ingresar un correo electrónico válido")
    String email;

    @Required(message = "El teléfono es obligatorio")
    @Column(length=20, unique=true)
    @Pattern(regexp = "^[0-9+ -]+$", message = "El teléfono solo puede contener números, espacios, guiones y el símbolo +")
    String telefono;

    @Enumerated(EnumType.STRING)
    Puesto puestoAplica;

    @Required(message = "La profesión es obligatoria")
    @Column(length=100)
    @DisplaySize(50)
    String profesion;

    @Temporal(TemporalType.TIMESTAMP)
    @Stereotype("DATETIME")
    @ReadOnly
    Date fechaRegistro;

    @PrePersist
    @PreUpdate
    void validarYPrepararGuardado() {
        asignarFechaRegistroSiEsNuevo();
        sanitizarDatos();
        validarCamposEnumObligatorios();
        validarTelefonoInternacional();
        validarEdadMinima();
        validarUnicidades();
    }

    @PreRemove
    void validarAntesDeBorrar() {
        Long countIntentos = contarRegistros(
            "select count(i) from IntentoTest i where i.candidato.id = :id",
            "id", this.id
        );
        if (countIntentos > 0) {
            throw new ValidationException(
                "Protección de Auditoría: No se puede eliminar este candidato porque ya tiene " +
                "pruebas psicométricas registradas en su historial. Elimine primero sus pruebas si desea proceder."
            );
        }
    }

    @Depends("fechaNacimiento")
    public int getEdadCalculada() {
        if (fechaNacimiento == null) return 0;
        return calcularEdad();
    }

    public boolean isEdadValida() {
        if (fechaNacimiento == null) return true;
        return calcularEdad() >= EDAD_MINIMA_BFA;
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

    private void asignarFechaRegistroSiEsNuevo() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = new Date();
        }
    }

    private void sanitizarDatos() {
        if (this.identificacion != null) {
            this.identificacion = this.identificacion.trim().toUpperCase();
        }
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }

    private void validarCamposEnumObligatorios() {
        if (this.nivelEducativo == null) {
            throw new ValidationException("El nivel educativo es obligatorio para evaluar los resultados psicométricos.");
        }
        if (this.genero == null) {
            throw new ValidationException("El género es obligatorio.");
        }
        if (this.puestoAplica == null) {
            throw new ValidationException("Debe seleccionar el puesto al que aplica el candidato.");
        }
    }

    private void validarTelefonoInternacional() {
        if (this.telefono == null || this.telefono.trim().isEmpty()) return;

        try {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            PhoneNumber numberProto = phoneUtil.parse(this.telefono, REGION_POR_DEFECTO);

            if (!phoneUtil.isValidNumber(numberProto)) {
                throw new ValidationException("El número de teléfono ingresado no es un número válido real.");
            }
            this.telefono = phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new ValidationException(
                "El formato del número de teléfono es ilegible. Si es de otro país, asegúrese de iniciar con su prefijo (ej. +34)."
            );
        }
    }

    private void validarEdadMinima() {
        if (fechaNacimiento == null) return;
        if (calcularEdad() < EDAD_MINIMA_BFA) {
            throw new ValidationException(
                "El candidato debe tener la edad mínima requerida (" + EDAD_MINIMA_BFA + " años) para aplicar la prueba BFA."
            );
        }
    }

    private void validarUnicidades() {
        validarCampoUnico("identificacion", this.identificacion,
            "Ya existe un candidato registrado en el sistema con este número de identificación.");

        validarCampoUnico("email", this.email,
            "Este correo electrónico ya está en uso por otro candidato.");

        validarCampoUnico("telefono", this.telefono,
            "Este número de teléfono ya está registrado en el sistema.");
    }

    private void validarCampoUnico(String campo, String valor, String mensajeError) {
        if (valor == null || valor.trim().isEmpty()) return;

        Long count = contarRegistros(
            "select count(c) from Candidato c where lower(c." + campo + ") = lower(:valor) and c.id <> :id",
            "valor", valor
        );
        if (count > 0) {
            throw new ValidationException(mensajeError);
        }
    }

    private Long contarRegistros(String jpql, String paramNombre, Object paramValor) {
        return (Long) XPersistence.getManager()
            .createQuery(jpql)
            .setParameter(paramNombre, paramValor)
            .setParameter("id", this.id)
            .getSingleResult();
    }

    private int calcularEdad() {
        LocalDate nacimiento = new java.sql.Date(fechaNacimiento.getTime()).toLocalDate();
        return Period.between(nacimiento, LocalDate.now()).getYears();
    }
}
