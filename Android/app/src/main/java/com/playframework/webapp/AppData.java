package com.playframework.webapp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class AppData {
    private static final AppData INSTANCE = new AppData();
    private final List<User> users = new ArrayList<>();
    private final List<Materia> materias = new ArrayList<>();
    private final List<Reserva> reservas = new ArrayList<>();
    private final List<Inscripcion> inscripciones = new ArrayList<>();
    private final Map<String, List<Message>> chats = new HashMap<>();
    private final Random random = new Random();
    private int nextUserId = 1;
    private int nextMateriaId = 1;
    private int nextReservaId = 1;
    private int nextInscripcionId = 1;
    private int nextMessageId = 1;
    private User currentUser;

    private AppData() {
        seed();
    }

    public static AppData getInstance() {
        return INSTANCE;
    }

    public List<User> getUsers() {
        return users;
    }

    public List<Materia> getMaterias() {
        return materias;
    }

    public List<Reserva> getReservas() {
        return reservas;
    }

    public List<Inscripcion> getInscripciones() {
        return inscripciones;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public User findUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }

    public User registerUser(String username, String password, String email, String fullName, Role role) {
        User user = new User(nextUserId++, username, password, email, fullName, role);
        users.add(user);
        return user;
    }

    public Materia addMateria(String nombre, String descripcion) {
        Materia materia = new Materia(nextMateriaId++, "MAT-" + nextMateriaId, nombre, descripcion);
        materias.add(materia);
        return materia;
    }

    public Reserva addReserva(Materia materia, User profesor, User alumno, String fecha, String hora) {
        String codigo = "CL-" + (1000 + random.nextInt(9000));
        Reserva reserva = new Reserva(nextReservaId++, materia, profesor, alumno, fecha, hora, codigo);
        reservas.add(reserva);
        return reserva;
    }

    public Inscripcion addInscripcion(Materia materia, User alumno, User profesor) {
        Inscripcion existente = findInscripcion(materia, alumno, profesor);
        if (existente != null) {
            return existente;
        }
        Inscripcion inscripcion = new Inscripcion(nextInscripcionId++, materia, alumno, profesor);
        inscripciones.add(inscripcion);
        return inscripcion;
    }

    public List<Message> getChat(User alumno, User profesor) {
        String key = chatKey(alumno, profesor);
        List<Message> list = chats.get(key);
        if (list == null) {
            list = new ArrayList<>();
            chats.put(key, list);
        }
        return list;
    }

    public Message addMessage(User alumno, User profesor, User author, String text) {
        String timestamp = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(System.currentTimeMillis());
        Message message = new Message(nextMessageId++, author, text, timestamp);
        getChat(alumno, profesor).add(message);
        return message;
    }

    public List<User> getProfesores() {
        List<User> profesores = new ArrayList<>();
        for (User user : users) {
            if (user.getRole() == Role.PROFESOR) {
                profesores.add(user);
            }
        }
        return profesores;
    }

    public List<User> getAlumnos() {
        List<User> alumnos = new ArrayList<>();
        for (User user : users) {
            if (user.getRole() == Role.ALUMNO) {
                alumnos.add(user);
            }
        }
        return alumnos;
    }

    public List<Reserva> getReservasForUser(User user) {
        List<Reserva> result = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if (reserva.getAlumno().equals(user) || reserva.getProfesor().equals(user)) {
                result.add(reserva);
            }
        }
        return result;
    }

    public List<User> getAlumnosForProfesor(User profesor) {
        List<User> alumnos = new ArrayList<>();
        for (Inscripcion inscripcion : inscripciones) {
            if (inscripcion.getProfesor().equals(profesor) && !alumnos.contains(inscripcion.getAlumno())) {
                alumnos.add(inscripcion.getAlumno());
            }
        }
        return alumnos;
    }

    public List<User> getAlumnosForMateria(Materia materia) {
        List<User> alumnos = new ArrayList<>();
        for (Inscripcion inscripcion : inscripciones) {
            if (inscripcion.getMateria().equals(materia) && !alumnos.contains(inscripcion.getAlumno())) {
                alumnos.add(inscripcion.getAlumno());
            }
        }
        return alumnos;
    }

    public List<Inscripcion> getInscripcionesForUser(User user) {
        List<Inscripcion> result = new ArrayList<>();
        for (Inscripcion inscripcion : inscripciones) {
            if (inscripcion.getAlumno().equals(user) || inscripcion.getProfesor().equals(user)) {
                result.add(inscripcion);
            }
        }
        return result;
    }

    public List<User> getProfesoresForMateria(Materia materia) {
        return new ArrayList<>(materia.getProfesores());
    }

    public void removeUser(User user) {
        users.remove(user);
        for (Materia materia : materias) {
            materia.getProfesores().remove(user);
            materia.getAlumnos().remove(user);
        }
        inscripciones.removeIf(inscripcion ->
            inscripcion.getAlumno().equals(user) || inscripcion.getProfesor().equals(user)
        );
    }

    public void removeMateria(Materia materia) {
        materias.remove(materia);
        inscripciones.removeIf(inscripcion -> inscripcion.getMateria().equals(materia));
    }

    public Reserva findReservaById(int id) {
        for (Reserva reserva : reservas) {
            if (reserva.getId() == id) {
                return reserva;
            }
        }
        return null;
    }

    public Materia findMateriaById(int id) {
        for (Materia materia : materias) {
            if (materia.getId() == id) {
                return materia;
            }
        }
        return null;
    }

    public List<Reserva> getReservasByMateria(Materia materia) {
        List<Reserva> result = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if (reserva.getMateria().equals(materia)) {
                result.add(reserva);
            }
        }
        return result;
    }

    public List<User> getTopAlumnos() {
        List<User> alumnos = getAlumnos();
        Collections.sort(alumnos, (a, b) -> Integer.compare(reservasCountForAlumno(b), reservasCountForAlumno(a)));
        return alumnos;
    }

    public List<User> getTopProfesores() {
        List<User> profesores = getProfesores();
        Collections.sort(profesores, (a, b) -> Integer.compare(alumnosCountForProfesor(b), alumnosCountForProfesor(a)));
        return profesores;
    }

    public int reservasCountForAlumno(User alumno) {
        int count = 0;
        for (Reserva reserva : reservas) {
            if (reserva.getAlumno().equals(alumno)) {
                count++;
            }
        }
        return count;
    }

    public int alumnosCountForProfesor(User profesor) {
        int count = 0;
        for (Inscripcion inscripcion : inscripciones) {
            if (inscripcion.getProfesor().equals(profesor)) {
                count++;
            }
        }
        return count;
    }

    public Inscripcion findInscripcion(Materia materia, User alumno, User profesor) {
        for (Inscripcion inscripcion : inscripciones) {
            if (inscripcion.getMateria().equals(materia)
                && inscripcion.getAlumno().equals(alumno)
                && inscripcion.getProfesor().equals(profesor)) {
                return inscripcion;
            }
        }
        return null;
    }

    private String chatKey(User alumno, User profesor) {
        return alumno.getId() + "-" + profesor.getId();
    }

    private void seed() {
        User profMaria = registerUser("maria", "1234", "maria@clases.com", "María Ruiz", Role.PROFESOR);
        User profCarlos = registerUser("carlos", "1234", "carlos@clases.com", "Carlos Vega", Role.PROFESOR);
        User alumNora = registerUser("nora", "1234", "nora@clases.com", "Nora Díaz", Role.ALUMNO);
        User alumLeo = registerUser("leo", "1234", "leo@clases.com", "Leo Ramos", Role.ALUMNO);

        Materia matematicas = new Materia(nextMateriaId++, "MAT-01", "Matemáticas", "Refuerzo de álgebra, cálculo y resolución de problemas.");
        Materia programacion = new Materia(nextMateriaId++, "MAT-02", "Programación", "Clases prácticas de Java, Python y proyectos guiados.");
        Materia ingles = new Materia(nextMateriaId++, "MAT-03", "Inglés", "Speaking clubs, gramática y preparación de exámenes.");

        materias.add(matematicas);
        materias.add(programacion);
        materias.add(ingles);

        matematicas.addProfesor(profMaria);
        programacion.addProfesor(profCarlos);
        programacion.addProfesor(profMaria);
        ingles.addProfesor(profCarlos);

        matematicas.addAlumno(alumNora);
        programacion.addAlumno(alumNora);
        programacion.addAlumno(alumLeo);
        ingles.addAlumno(alumLeo);

        addInscripcion(matematicas, alumNora, profMaria);
        addInscripcion(programacion, alumNora, profCarlos);
        addInscripcion(programacion, alumLeo, profMaria);
        addInscripcion(ingles, alumLeo, profCarlos);

        addReserva(programacion, profCarlos, alumNora, "2024-06-18", "16:30");
        addReserva(ingles, profCarlos, alumLeo, "2024-06-20", "10:00");
        addReserva(matematicas, profMaria, alumNora, "2024-06-22", "12:30");

        addMessage(alumNora, profCarlos, alumNora, "Hola Carlos, ¿podemos repasar listas en Java?");
        addMessage(alumNora, profCarlos, profCarlos, "¡Claro! Preparo algunos ejercicios para hoy.");
    }
}
