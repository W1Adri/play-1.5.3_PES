package models;

import play.db.jpa.Model;
import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class usuari extends Model {

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false)
    public String email;

    @Column(nullable = false)
    public String fullName;

    @Column(nullable = false)
    public String rol; // "alumno" o "profesor"

    public usuari() {}

    public usuari(String username, String passwordHash, String email, String fullName, String rol) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.fullName = fullName;
        this.rol = rol;
    }
}
