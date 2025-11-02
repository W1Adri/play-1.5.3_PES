package models;

import play.db.jpa.Model;
import javax.persistence.*;
import java.util.*;

@Entity
public class Materia extends Model {

    @Column(nullable = false, unique = true)
    public String codigo;

    @Column(nullable = false)
    public String nombre;

    @Column(length = 2000)
    public String descripcion;

    // Inverso de Inscripcion.materia  (bidireccional)
    @OneToMany(mappedBy = "materia", cascade = CascadeType.ALL)
    public List<Inscripcion> inscripciones = new ArrayList<>();

    public Materia() {}

    public Materia(String codigo, String nombre, String descripcion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
}