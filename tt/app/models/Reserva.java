package models;

import play.db.jpa.Model;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
public class Reserva extends Model {

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public Usuario profesor;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public Usuario alumno;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public Materia materia;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    public Date fechaReserva;

    @Column(nullable = false, unique = true)
    public String codigoSala;

    @Lob
    public String offerSdp;

    @Temporal(TemporalType.TIMESTAMP)
    public Date offerActualizada;

    @Lob
    public String answerSdp;

    @Temporal(TemporalType.TIMESTAMP)
    public Date answerActualizada;

    public Reserva() {
    }

    public Reserva(Usuario profesor, Usuario alumno, Materia materia, Date fechaReserva) {
        this.profesor = profesor;
        this.alumno = alumno;
        this.materia = materia;
        this.fechaReserva = fechaReserva;
        this.codigoSala = generarCodigo();
    }

    public void reiniciarSesion() {
        this.offerSdp = null;
        this.offerActualizada = null;
        this.answerSdp = null;
        this.answerActualizada = null;
    }

    private static String generarCodigo() {
        return UUID.randomUUID().toString();
    }
}
