package com.playframework.webapp;

import java.util.ArrayList;
import java.util.List;

public class Materia {
    private final int id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private final List<User> profesores = new ArrayList<>();
    private final List<User> alumnos = new ArrayList<>();

    public Materia(int id, String codigo, String nombre, String descripcion) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public int getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<User> getProfesores() {
        return profesores;
    }

    public List<User> getAlumnos() {
        return alumnos;
    }

    public void addProfesor(User profesor) {
        if (!profesores.contains(profesor)) {
            profesores.add(profesor);
        }
    }

    public void addAlumno(User alumno) {
        if (!alumnos.contains(alumno)) {
            alumnos.add(alumno);
        }
    }
}
