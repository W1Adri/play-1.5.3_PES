package models;

import play.db.jpa.Model;
import javax.persistence.*;
import java.util.*;
import models.Mensaje;
import models.Inscripcion;
import models.Rol; // Importar el nuevo Enum

@Entity
public class Usuario extends Model { // Nombre de clase en Mayúscula

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false)
    public String email;

    @Column(nullable = false)
    public String fullName;

    @Enumerated(EnumType.STRING) // Indicar a JPA que guarde el Enum como String
    @Column(nullable = false)
    public Rol rol; // Campo de tipo Rol

    // 🔹 Relaciones

    @OneToMany(mappedBy = "emisor", cascade = CascadeType.ALL)
    public List<Mensaje> mensajesEnviados = new ArrayList<>();

    // Se quita CascadeType.ALL (no borrar mensajes si se borra el receptor)
    @OneToMany(mappedBy = "receptor")
    public List<Mensaje> mensajesRecibidos = new ArrayList<>();

    @OneToMany(mappedBy = "alumno", cascade = CascadeType.ALL)
    public List<Inscripcion> inscripcionesComoAlumno = new ArrayList<>();

    // Se quita CascadeType.ALL (no borrar matrículas si se borra el profesor)
    @OneToMany(mappedBy = "profesor")
    public List<Inscripcion> inscripcionesComoProfesor = new ArrayList<>();

    // Constructor por defecto (necesario para JPA)
    public Usuario() {}

    // Constructor actualizado
    public Usuario(String username, String passwordHash, String email, String fullName, Rol rol) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.fullName = fullName;
        this.rol = rol;
    }
}