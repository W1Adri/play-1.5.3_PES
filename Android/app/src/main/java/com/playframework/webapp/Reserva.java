package com.playframework.webapp;

public class Reserva {
    private final int id;
    private final Materia materia;
    private final User profesor;
    private final User alumno;
    private final String fecha;
    private final String hora;
    private final String codigoSala;

    public Reserva(int id, Materia materia, User profesor, User alumno, String fecha, String hora, String codigoSala) {
        this.id = id;
        this.materia = materia;
        this.profesor = profesor;
        this.alumno = alumno;
        this.fecha = fecha;
        this.hora = hora;
        this.codigoSala = codigoSala;
    }

    public int getId() {
        return id;
    }

    public Materia getMateria() {
        return materia;
    }

    public User getProfesor() {
        return profesor;
    }

    public User getAlumno() {
        return alumno;
    }

    public String getFecha() {
        return fecha;
    }

    public String getHora() {
        return hora;
    }

    public String getCodigoSala() {
        return codigoSala;
    }
}
