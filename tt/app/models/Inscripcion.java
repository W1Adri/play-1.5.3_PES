package models;

import play.db.jpa.Model;
import javax.persistence.*;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"alumno_id", "materia_id", "profesor_id"})
        }
)
public class Inscripcion extends Model {

    @ManyToOne(optional = false)
    @JoinColumn(name = "alumno_id", nullable = false)
    public Usuario alumno;   // Cambiado de usuari a Usuario

    @ManyToOne(optional = false)
    @JoinColumn(name = "profesor_id", nullable = false)
    public Usuario profesor; // Cambiado de usuari a Usuario

    @ManyToOne(optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    public Materia materia;

    public Inscripcion() {}

    public Inscripcion(Usuario alumno, Usuario profesor, Materia materia) { // Constructor actualizado
        this.alumno = alumno;
        this.profesor = profesor;
        this.materia = materia;
    }
}