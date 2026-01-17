package controllers;

import models.*;
import play.libs.Crypto;
import play.mvc.Before;
import play.mvc.Controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Api extends Controller {

    @Before(unless = {"login", "register"})
    static void requireAuth() {
        if (session.get("username") == null) {
            response.status = 401;
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("error", "No autorizado");
            renderJSON(error);
        }
    }

    public static void login(String username, String password) {
        if (username == null || password == null) {
            response.status = 400;
            renderJSON(Collections.singletonMap("error", "Faltan credenciales"));
        }
        username = username.trim().toLowerCase();
        Usuario u = Usuario.find("byUsername", username).first();
        if (u == null) {
            response.status = 401;
            renderJSON(Collections.singletonMap("error", "Usuario o contraseña incorrectos"));
        }
        String passwordHash = Crypto.passwordHash(password);
        if (!passwordHash.equals(u.passwordHash)) {
            response.status = 401;
            renderJSON(Collections.singletonMap("error", "Usuario o contraseña incorrectos"));
        }
        session.put("username", u.username);
        renderJSON(Collections.singletonMap("user", toUserDto(u)));
    }

    public static void register(String username, String password, String email, String fullName, String rol) {
        if (username == null || username.trim().isEmpty()) renderBadRequest("El nombre de usuario no puede estar vacío.");
        if (password == null || password.trim().isEmpty()) renderBadRequest("La contraseña no puede estar vacía.");
        if (email == null || email.trim().isEmpty()) renderBadRequest("El email no puede estar vacío.");
        if (fullName == null || fullName.trim().isEmpty()) renderBadRequest("El nombre completo no puede estar vacío.");

        username = username.trim().toLowerCase();
        email = email.trim().toLowerCase();
        fullName = fullName.trim();

        Rol rolEnum;
        if ("alumno".equalsIgnoreCase(rol)) {
            rolEnum = Rol.ALUMNO;
        } else if ("profesor".equalsIgnoreCase(rol)) {
            rolEnum = Rol.PROFESOR;
        } else {
            renderBadRequest("Rol inválido (debe ser 'alumno' o 'profesor').");
            return;
        }

        if (Usuario.find("byUsername", username).first() != null) renderBadRequest("El usuario ya existe.");
        if (Usuario.find("byEmail", email).first() != null) renderBadRequest("El email ya está en uso.");

        String passwordHash = Crypto.passwordHash(password);
        Usuario nuevo = new Usuario(username, passwordHash, email, fullName, rolEnum).save();
        session.put("username", nuevo.username);
        renderJSON(Collections.singletonMap("user", toUserDto(nuevo)));
    }

    public static void me() {
        Usuario u = connected();
        renderJSON(Collections.singletonMap("user", toUserDto(u)));
    }

    public static void panelAlumno() {
        Usuario u = connected();
        if (u.rol != Rol.ALUMNO) {
            renderUnauthorized("Acceso no autorizado");
        }

        List<Materia> materias = Materia.findAll();
        List<Inscripcion> inscripciones = Inscripcion.find("byAlumno", u).fetch();

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("user", toUserDto(u));
        resp.put("totals", totalsDto());
        resp.put("materias", toMateriasDto(materias));
        resp.put("inscripciones", toInscripcionesDto(inscripciones));
        renderJSON(resp);
    }

    public static void panelProfesor() {
        Usuario profesor = connected();
        if (profesor.rol != Rol.PROFESOR) {
            renderUnauthorized("Acceso no autorizado");
        }

        List<Inscripcion> misAlumnos = Inscripcion.find("byProfesor", profesor).fetch();

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("user", toUserDto(profesor));
        resp.put("totals", totalsDto());
        resp.put("alumnos", toInscripcionesDto(misAlumnos));
        renderJSON(resp);
    }

    public static void materias() {
        List<Materia> materias = Materia.findAll();
        renderJSON(Collections.singletonMap("materias", toMateriasDto(materias)));
    }

    public static void materiaDetalle(Long id) {
        Materia materia = Materia.findById(id);
        if (materia == null) {
            renderNotFound("Materia no encontrada");
        }
        List<Inscripcion> inscripciones = Inscripcion.find("byMateria", materia).fetch();
        LinkedHashMap<Long, Usuario> profesores = new LinkedHashMap<Long, Usuario>();
        for (Inscripcion inscripcion : inscripciones) {
            if (inscripcion.profesor != null) {
                profesores.put(inscripcion.profesor.id, inscripcion.profesor);
            }
        }

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("materia", toMateriaDto(materia));
        resp.put("profesores", toUsersDto(new ArrayList<Usuario>(profesores.values())));
        renderJSON(resp);
    }

    public static void inscribirse(Long materiaId, Long profesorId) {
        Usuario alumno = connected();
        if (alumno.rol != Rol.ALUMNO) {
            renderUnauthorized("Solo alumnos pueden inscribirse.");
        }
        Materia materia = Materia.findById(materiaId);
        Usuario profesor = Usuario.findById(profesorId);
        if (materia == null || profesor == null) {
            renderNotFound("Materia o profesor no encontrado.");
        }
        Inscripcion existente = Inscripcion.find("byAlumnoAndMateriaAndProfesor", alumno, materia, profesor).first();
        if (existente == null) {
            new Inscripcion(alumno, profesor, materia).save();
        }
        renderJSON(Collections.singletonMap("status", "ok"));
    }

    public static void reservas() {
        Usuario yo = connected();
        List<Reserva> reservas;
        List<Inscripcion> opciones;

        if (yo.rol == Rol.ALUMNO) {
            reservas = Reserva.find("alumno = ?1 ORDER BY fechaReserva ASC", yo).fetch();
            opciones = Inscripcion.find("byAlumno", yo).fetch();
        } else if (yo.rol == Rol.PROFESOR) {
            reservas = Reserva.find("profesor = ?1 ORDER BY fechaReserva ASC", yo).fetch();
            opciones = Inscripcion.find("byProfesor", yo).fetch();
        } else {
            renderBadRequest("Rol no soportado para reservas.");
            return;
        }

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("reservas", toReservasDto(reservas));
        resp.put("opciones", toInscripcionesDto(opciones));
        renderJSON(resp);
    }

    public static void crearReserva(Long inscripcionId, String fecha, String hora) {
        Usuario yo = connected();
        if (inscripcionId == null) {
            renderBadRequest("Selecciona una inscripción válida.");
        }

        Inscripcion inscripcion = Inscripcion.findById(inscripcionId);
        if (inscripcion == null) {
            renderNotFound("La inscripción seleccionada no existe.");
        }

        if (!yo.id.equals(inscripcion.alumno.id) && !yo.id.equals(inscripcion.profesor.id)) {
            renderUnauthorized("No tienes permiso para crear reservas con esa inscripción.");
        }

        if (fecha == null || fecha.trim().isEmpty() || hora == null || hora.trim().isEmpty()) {
            renderBadRequest("Indica la fecha y la hora de la clase.");
        }

        Date fechaReserva;
        try {
            SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            formato.setLenient(false);
            fechaReserva = formato.parse(fecha.trim() + " " + hora.trim());
        } catch (ParseException e) {
            renderBadRequest("Formato de fecha u hora inválido.");
            return;
        }

        Reserva nueva = new Reserva(inscripcion.profesor, inscripcion.alumno, inscripcion.materia, fechaReserva);
        nueva.save();
        renderJSON(Collections.singletonMap("reserva", toReservaDto(nueva)));
    }

    public static void consultas(String tipo, Long materiaId) {
        Usuario yo = connected();
        List<Materia> materias = Materia.findAll();
        Materia materiaSeleccionada = null;
        Long resultado = null;
        String descripcion = null;
        String error = null;
        List<Usuario> detalleUsuarios = new ArrayList<Usuario>();
        List<Map<String, Object>> ranking = new ArrayList<Map<String, Object>>();

        if (tipo != null && !tipo.trim().isEmpty()) {
            tipo = tipo.trim();
            if ("totalUsuarios".equals(tipo)) {
                resultado = Usuario.count();
                descripcion = "Usuarios registrados";
            } else if ("totalAlumnos".equals(tipo)) {
                resultado = Usuario.count("rol = ?1", Rol.ALUMNO);
                descripcion = "Alumnos registrados";
            } else if ("totalProfesores".equals(tipo)) {
                resultado = Usuario.count("rol = ?1", Rol.PROFESOR);
                descripcion = "Profesores registrados";
            } else if ("profesoresMateria".equals(tipo) || "alumnosMateria".equals(tipo)) {
                if (materiaId == null) {
                    error = "Selecciona una materia para realizar la consulta.";
                } else {
                    materiaSeleccionada = Materia.findById(materiaId);
                    if (materiaSeleccionada == null) {
                        error = "La materia indicada no existe.";
                    } else {
                        List<Inscripcion> inscripciones = Inscripcion.find("byMateria", materiaSeleccionada).fetch();
                        LinkedHashMap<Long, Usuario> usuariosUnicos = new LinkedHashMap<Long, Usuario>();
                        for (Inscripcion inscripcion : inscripciones) {
                            if ("profesoresMateria".equals(tipo) && inscripcion.profesor != null) {
                                usuariosUnicos.put(inscripcion.profesor.id, inscripcion.profesor);
                            }
                            if ("alumnosMateria".equals(tipo) && inscripcion.alumno != null) {
                                usuariosUnicos.put(inscripcion.alumno.id, inscripcion.alumno);
                            }
                        }
                        detalleUsuarios = new ArrayList<Usuario>(usuariosUnicos.values());
                        resultado = Long.valueOf(detalleUsuarios.size());
                        if ("profesoresMateria".equals(tipo)) {
                            descripcion = "Profesores registrados en " + materiaSeleccionada.nombre;
                        } else {
                            descripcion = "Alumnos inscritos en " + materiaSeleccionada.nombre;
                        }
                    }
                }
            } else if ("reservasPorMateria".equals(tipo)) {
                List<Object[]> filas = Reserva.find(
                        "select r.materia.nombre, count(r) from Reserva r group by r.materia.nombre order by count(r) desc"
                ).fetch();
                for (Object[] fila : filas) {
                    Map<String, Object> item = new HashMap<String, Object>();
                    item.put("label", fila[0]);
                    item.put("count", fila[1]);
                    ranking.add(item);
                }
                descripcion = "Reservas totales por materia";
            } else if ("topAlumnos".equals(tipo)) {
                List<Object[]> filas = Reserva.find(
                        "select r.alumno.fullName, r.alumno.username, count(r) " +
                                "from Reserva r group by r.alumno.id, r.alumno.fullName, r.alumno.username order by count(r) desc"
                ).fetch();
                for (Object[] fila : filas) {
                    Map<String, Object> item = new HashMap<String, Object>();
                    item.put("label", fila[0] + " · " + fila[1]);
                    item.put("count", fila[2]);
                    ranking.add(item);
                }
                descripcion = "Alumnos con más reservas";
            } else if ("topProfesores".equals(tipo)) {
                List<Object[]> filas = Inscripcion.find(
                        "select i.profesor.fullName, i.profesor.username, count(i) " +
                                "from Inscripcion i group by i.profesor.id, i.profesor.fullName, i.profesor.username order by count(i) desc"
                ).fetch();
                for (Object[] fila : filas) {
                    Map<String, Object> item = new HashMap<String, Object>();
                    item.put("label", fila[0] + " · " + fila[1]);
                    item.put("count", fila[2]);
                    ranking.add(item);
                }
                descripcion = "Profesores con más alumnos";
            } else {
                error = "Consulta no reconocida.";
            }
        }

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("materias", toMateriasDto(materias));
        resp.put("descripcion", descripcion);
        resp.put("resultado", resultado);
        resp.put("detalleUsuarios", toUsersDto(detalleUsuarios));
        resp.put("ranking", ranking);
        resp.put("error", error);
        resp.put("tipo", tipo);
        renderJSON(resp);
    }

    public static void gestion() {
        Usuario profesor = connected();
        if (profesor.rol != Rol.PROFESOR) {
            renderUnauthorized("Acceso no autorizado");
        }
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("usuarios", toUsersDto(Usuario.findAll()));
        resp.put("materias", toMateriasDto(Materia.findAll()));
        renderJSON(resp);
    }

    public static void crearUsuario(String username, String password, String email, String fullName, String rol) {
        Usuario profesor = connected();
        if (profesor.rol != Rol.PROFESOR) {
            renderUnauthorized("Acceso no autorizado");
        }
        if (username == null || username.trim().isEmpty()) renderBadRequest("El nombre de usuario no puede estar vacío.");
        if (password == null || password.trim().isEmpty()) renderBadRequest("La contraseña no puede estar vacía.");
        if (email == null || email.trim().isEmpty()) renderBadRequest("El email no puede estar vacío.");
        if (fullName == null || fullName.trim().isEmpty()) renderBadRequest("El nombre completo no puede estar vacío.");

        username = username.trim().toLowerCase();
        email = email.trim().toLowerCase();
        fullName = fullName.trim();

        Rol rolEnum;
        if ("alumno".equalsIgnoreCase(rol)) {
            rolEnum = Rol.ALUMNO;
        } else if ("profesor".equalsIgnoreCase(rol)) {
            rolEnum = Rol.PROFESOR;
        } else {
            renderBadRequest("Rol inválido (debe ser 'alumno' o 'profesor').");
            return;
        }

        if (Usuario.find("byUsername", username).first() != null) renderBadRequest("El usuario ya existe.");
        if (Usuario.find("byEmail", email).first() != null) renderBadRequest("El email ya está en uso.");

        String passwordHash = Crypto.passwordHash(password);
        Usuario nuevo = new Usuario(username, passwordHash, email, fullName, rolEnum).save();
        renderJSON(Collections.singletonMap("user", toUserDto(nuevo)));
    }

    public static void actualizarUsuario(Long id, String email, String fullName, String rol) {
        Usuario profesor = connected();
        if (profesor.rol != Rol.PROFESOR) {
            renderUnauthorized("Acceso no autorizado");
        }
        Usuario usuario = Usuario.findById(id);
        if (usuario == null) {
            renderNotFound("Usuario no encontrado");
        }
        usuario.email = email;
        usuario.fullName = fullName;
        usuario.rol = Rol.valueOf(rol.toUpperCase());
        usuario.save();
        renderJSON(Collections.singletonMap("user", toUserDto(usuario)));
    }

    public static void eliminarUsuario(Long id) {
        Usuario profesor = connected();
        if (profesor.rol != Rol.PROFESOR) {
            renderUnauthorized("Acceso no autorizado");
        }
        Usuario usuario = Usuario.findById(id);
        if (usuario == null) {
            renderNotFound("Usuario no encontrado");
        }
        usuario.delete();
        renderJSON(Collections.singletonMap("status", "ok"));
    }

    public static void actualizarMateria(Long id, String codigo, String nombre, String descripcion) {
        Usuario profesor = connected();
        if (profesor.rol != Rol.PROFESOR) {
            renderUnauthorized("Acceso no autorizado");
        }
        Materia materia = Materia.findById(id);
        if (materia == null) {
            renderNotFound("Materia no encontrada");
        }
        materia.codigo = codigo;
        materia.nombre = nombre;
        materia.descripcion = descripcion;
        materia.save();
        renderJSON(Collections.singletonMap("materia", toMateriaDto(materia)));
    }

    public static void eliminarMateria(Long id) {
        Usuario profesor = connected();
        if (profesor.rol != Rol.PROFESOR) {
            renderUnauthorized("Acceso no autorizado");
        }
        Materia materia = Materia.findById(id);
        if (materia == null) {
            renderNotFound("Materia no encontrada");
        }
        materia.delete();
        renderJSON(Collections.singletonMap("status", "ok"));
    }

    public static void crearMateria(String codigo, String nombre, String descripcion) {
        Usuario profesor = connected();
        if (profesor.rol != Rol.PROFESOR) {
            renderUnauthorized("Acceso no autorizado");
        }
        Materia materia = new Materia(codigo, nombre, descripcion).save();
        renderJSON(Collections.singletonMap("materia", toMateriaDto(materia)));
    }

    private static Usuario connected() {
        String username = session.get("username");
        if (username == null) return null;
        return Usuario.find("byUsername", username).first();
    }

    private static Map<String, Object> toUserDto(Usuario u) {
        Map<String, Object> user = new HashMap<String, Object>();
        user.put("id", u.id);
        user.put("username", u.username);
        user.put("email", u.email);
        user.put("fullName", u.fullName);
        user.put("rol", u.rol.toString().toLowerCase());
        return user;
    }

    private static List<Map<String, Object>> toUsersDto(List<Usuario> users) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Usuario u : users) {
            list.add(toUserDto(u));
        }
        return list;
    }

    private static Map<String, Object> toMateriaDto(Materia materia) {
        Map<String, Object> dto = new HashMap<String, Object>();
        dto.put("id", materia.id);
        dto.put("codigo", materia.codigo);
        dto.put("nombre", materia.nombre);
        dto.put("descripcion", materia.descripcion);
        return dto;
    }

    private static List<Map<String, Object>> toMateriasDto(List<Materia> materias) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Materia materia : materias) {
            list.add(toMateriaDto(materia));
        }
        return list;
    }

    private static Map<String, Object> toInscripcionDto(Inscripcion inscripcion) {
        Map<String, Object> dto = new HashMap<String, Object>();
        dto.put("id", inscripcion.id);
        dto.put("materia", toMateriaDto(inscripcion.materia));
        dto.put("alumno", toUserDto(inscripcion.alumno));
        dto.put("profesor", toUserDto(inscripcion.profesor));
        return dto;
    }

    private static List<Map<String, Object>> toInscripcionesDto(List<Inscripcion> inscripciones) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Inscripcion inscripcion : inscripciones) {
            list.add(toInscripcionDto(inscripcion));
        }
        return list;
    }

    private static Map<String, Object> toReservaDto(Reserva reserva) {
        Map<String, Object> dto = new HashMap<String, Object>();
        dto.put("id", reserva.id);
        dto.put("materia", toMateriaDto(reserva.materia));
        dto.put("alumno", toUserDto(reserva.alumno));
        dto.put("profesor", toUserDto(reserva.profesor));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        dto.put("fecha", dateFormat.format(reserva.fechaReserva));
        dto.put("hora", timeFormat.format(reserva.fechaReserva));
        dto.put("codigoSala", reserva.codigoSala);
        return dto;
    }

    private static List<Map<String, Object>> toReservasDto(List<Reserva> reservas) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Reserva reserva : reservas) {
            list.add(toReservaDto(reserva));
        }
        return list;
    }

    private static Map<String, Object> totalsDto() {
        Map<String, Object> totals = new HashMap<String, Object>();
        totals.put("usuarios", Usuario.count());
        totals.put("alumnos", Usuario.count("rol = ?1", Rol.ALUMNO));
        totals.put("profesores", Usuario.count("rol = ?1", Rol.PROFESOR));
        return totals;
    }

    private static void renderBadRequest(String message) {
        response.status = 400;
        renderJSON(Collections.singletonMap("error", message));
    }

    private static void renderNotFound(String message) {
        response.status = 404;
        renderJSON(Collections.singletonMap("error", message));
    }

    private static void renderUnauthorized(String message) {
        response.status = 401;
        renderJSON(Collections.singletonMap("error", message));
    }
}
