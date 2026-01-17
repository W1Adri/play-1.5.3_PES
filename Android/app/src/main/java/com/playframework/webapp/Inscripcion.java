package com.playframework.webapp;

public class Inscripcion {
    private final int id;
    private final Materia materia;
    private final User alumno;
    private final User profesor;

    public Inscripcion(int id, Materia materia, User alumno, User profesor) {
        this.id = id;
        this.materia = materia;
        this.alumno = alumno;
        this.profesor = profesor;
    }

    public int getId() {
        return id;
    }

    public Materia getMateria() {
        return materia;
    }

    public User getAlumno() {
        return alumno;
    }

    public User getProfesor() {
        return profesor;
    }
}
