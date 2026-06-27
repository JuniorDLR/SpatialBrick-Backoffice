package ni.spatialBrick.SpatialBrick.modelo.transaccional;

import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.*;
import ni.spatialBrick.SpatialBrick.modelo.configuracion.*;
import javax.persistence.*;
import org.openxava.annotations.*;
import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.*;
import lombok.*;
import java.util.Date;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.NumberParseException;
import javax.validation.constraints.*;

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

    @Lob
    @Stereotype("PHOTO")
    byte[] foto;

    @Column(length=50, unique=true)
    @Required(message = "La identificación es obligatoria")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "La identificación solo puede contener letras, números y guiones (ej. Pasaportes, DNI, etc.)")
    String identificacion;

    @Column(length=100)
    @DisplaySize(50)
    @Required
    String nombreCompleto;

    @Required(message = "El nivel educativo es obligatorio para evaluar los resultados psicométricos")
    @Enumerated(EnumType.STRING)
    NivelEducativo nivelEducativo;

    @Required
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

    @Required
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
        if (this.fechaRegistro == null) {
            this.fechaRegistro = new Date();
        }
        
        if (this.identificacion != null) {
            this.identificacion = this.identificacion.trim().toUpperCase();
        }
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
        if (this.telefono != null && !this.telefono.trim().isEmpty()) {
            try {
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                PhoneNumber numberProto = phoneUtil.parse(this.telefono, "NI");
                
                if (!phoneUtil.isValidNumber(numberProto)) {
                    throw new javax.validation.ValidationException("El número de teléfono ingresado no es un número válido real.");
                }
                this.telefono = phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
            } catch (NumberParseException e) {
                throw new javax.validation.ValidationException("El formato del número de teléfono es ilegible. Si es de otro país, asegúrese de iniciar con su prefijo (ej. +34).");
            }
        }

        if (fechaNacimiento != null) {
            java.time.LocalDate birth = new java.sql.Date(fechaNacimiento.getTime()).toLocalDate();
            java.time.LocalDate now = java.time.LocalDate.now();
            if (java.time.Period.between(birth, now).getYears() < EDAD_MINIMA_BFA) {
                throw new javax.validation.ValidationException("El candidato debe tener la edad mínima requerida (" + EDAD_MINIMA_BFA + " años) para aplicar la prueba BFA.");
            }
        }

        if (this.identificacion != null && !this.identificacion.trim().isEmpty()) {
            Long countId = (Long) org.openxava.jpa.XPersistence.getManager()
                .createQuery("select count(c) from Candidato c where lower(c.identificacion) = lower(:identificacion) and c.id <> :id")
                .setParameter("identificacion", this.identificacion)
                .setParameter("id", this.id)
                .getSingleResult();
            if (countId > 0) {
                throw new javax.validation.ValidationException("Ya existe un candidato registrado en el sistema con este número de identificación.");
            }
        }

        if (this.email != null && !this.email.trim().isEmpty()) {
            Long countEmail = (Long) org.openxava.jpa.XPersistence.getManager()
                .createQuery("select count(c) from Candidato c where lower(c.email) = lower(:email) and c.id <> :id")
                .setParameter("email", this.email)
                .setParameter("id", this.id)
                .getSingleResult();
            if (countEmail > 0) {
                throw new javax.validation.ValidationException("Este correo electrónico ya está en uso por otro candidato.");
            }
        }

        if (this.telefono != null && !this.telefono.trim().isEmpty()) {
            Long countTel = (Long) org.openxava.jpa.XPersistence.getManager()
                .createQuery("select count(c) from Candidato c where c.telefono = :telefono and c.id <> :id")
                .setParameter("telefono", this.telefono)
                .setParameter("id", this.id)
                .getSingleResult();
            if (countTel > 0) {
                throw new javax.validation.ValidationException("Este número de teléfono ya está registrado en el sistema.");
            }
        }
    }

    @PreRemove
    void validarAntesDeBorrar() {
        Long countIntentos = (Long) org.openxava.jpa.XPersistence.getManager()
            .createQuery("select count(i) from IntentoTest i where i.candidato.id = :id")
            .setParameter("id", this.id)
            .getSingleResult();
            
        if (countIntentos > 0) {
            throw new javax.validation.ValidationException("Protección de Auditoría: No se puede eliminar este candidato porque ya tiene pruebas psicométricas (Intento Test) registradas en su historial. Elimine primero sus pruebas si desea proceder.");
        }
    }

    private static final int EDAD_MINIMA_BFA = 14;

    @Depends("fechaNacimiento")
    public int getEdadCalculada() {
        if (fechaNacimiento == null) return 0;
        java.time.LocalDate birth = new java.sql.Date(fechaNacimiento.getTime()).toLocalDate();
        java.time.LocalDate now = java.time.LocalDate.now();
        return java.time.Period.between(birth, now).getYears();
    }

    public boolean isEdadValida() {
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
