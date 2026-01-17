package models;

import play.db.jpa.Model;
import javax.persistence.*;
import java.util.Date;

@Entity
public class Mensaje extends Model {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "emisor_id", nullable = false)
    public Usuario emisor; // Cambiado de usuari a Usuario

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "receptor_id", nullable = false)
    public Usuario receptor; // Cambiado de usuari a Usuario

    @Lob
    @Column(nullable = false)
    public String contenido;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    public Date fecha = new Date();

    public Mensaje() {}

    public Mensaje(Usuario emisor, Usuario receptor, String contenido) { // Constructor actualizado
        this.emisor = emisor;
        this.receptor = receptor;
        this.contenido = contenido;
    }
}