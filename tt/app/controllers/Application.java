package controllers;

import models.*;
import play.mvc.Controller;
import play.mvc.Before; // Importar @Before
import play.libs.Crypto; // Importar Crypto para contraseñas
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Application extends Controller {

    // --- FILTRO DE SEGURIDAD ---
    @Before(unless={"index", "login", "register", "logout"})
    static void checkAutenticacion() {
        if (session.get("username") == null) {
            flash.error("Por favor, inicia sesión.");
            index();
            return;
        }
    }

    // --- INICIO ---
    public static void index() {
        if (session.get("username") != null) {
            Usuario u = connected();
            if (u == null) {
                session.clear();
                flash.error("Tu sesión ha expirado, inicia sesión nuevamente.");
            } else if (u.rol == Rol.ALUMNO) {
                panelAlumno();
                return;
            } else if (u.rol == Rol.PROFESOR) {
                panelProfesor();
                return;
            }
        }
        render("Application/index.html");
    }

    // --- REGISTRO ---
    public static void register(String username, String password, String email, String fullName, String rol) {
        if (username == null || username.trim().isEmpty()) renderText("El nombre de usuario no puede estar vacío.");
        if (password == null || password.trim().isEmpty()) renderText("La contraseña no puede estar vacía.");
        if (email == null || email.trim().isEmpty()) renderText("El email no puede estar vacío.");
        if (fullName == null || fullName.trim().isEmpty()) renderText("El nombre completo no puede estar vacío.");

        username = username.trim().toLowerCase();
        email = email.trim().toLowerCase();
        fullName = fullName.trim();

        Rol rolEnum;
        if ("alumno".equalsIgnoreCase(rol)) {
            rolEnum = Rol.ALUMNO;
        } else if ("profesor".equalsIgnoreCase(rol)) {
            rolEnum = Rol.PROFESOR;
        } else {
            renderText("Rol inválido (debe ser 'alumno' o 'profesor').");
            return;
        }

        if (Usuario.find("byUsername", username).first() != null) renderText("El usuario ya existe.");
        if (Usuario.find("byEmail", email).first() != null) renderText("El email ya está en uso.");

        String passwordHash = Crypto.passwordHash(password);

        new Usuario(username, passwordHash, email, fullName, rolEnum).save();
        renderText("Usuario registrado correctamente como " + rol + ": " + username);
    }


    // --- LOGIN ---
    public static void login(String username, String password) {
        if (username == null || password == null) renderText("Faltan credenciales.");
        username = username.trim().toLowerCase();

        String passwordHash = Crypto.passwordHash(password);
        Usuario u = Usuario.find("byUsernameAndPasswordHash", username, passwordHash).first();

        if (u == null) {
            flash.error("Usuario o contraseña incorrectos");
            index();
        }

        session.put("username", u.username);

        if (u.rol == Rol.ALUMNO) panelAlumno();
        else if (u.rol == Rol.PROFESOR) panelProfesor();
        else {
            flash.error("Rol desconocido para este usuario.");
            logout();
        }
    }


    // --- OBTENER USUARIO CONECTADO ---
    public static Usuario connected() {
        String username = session.get("username");
        if (username == null) return null;
        return Usuario.find("byUsername", username).first();
    }

    // --- PANEL ALUMNO ---
    public static void panelAlumno() {
        Usuario u = connected();
        if (u == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }
        if (u.rol != Rol.ALUMNO) {
            flash.error("Acceso no autorizado.");
            logout();
        }

        List<Materia> materias = Materia.findAll();
        List<Materia> inscripciones = new ArrayList<Materia>();
        List<Inscripcion> registros = Inscripcion.find("byAlumno", u).fetch();
        Map<Long, Usuario> profesorPorMateria = new HashMap<Long, Usuario>();

        long totalUsuarios = Usuario.count();
        long totalAlumnos = Usuario.count("rol = ?1", Rol.ALUMNO);
        long totalProfesores = Usuario.count("rol = ?1", Rol.PROFESOR);

        for (Inscripcion i : registros) {
            inscripciones.add(i.materia);
            profesorPorMateria.put(i.materia.id, i.profesor);
        }

        List<Usuario> profesores = Usuario.find("byRol", Rol.PROFESOR).fetch();
        renderTemplate("Application/panelAlumno.html", u, materias, inscripciones, profesores, profesorPorMateria,
                totalUsuarios, totalAlumnos, totalProfesores);
    }

    // --- PANEL PROFESOR ---
    public static void panelProfesor() {
        Usuario profesor = connected();
        if (profesor == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }
        if (profesor.rol != Rol.PROFESOR) {
            flash.error("Acceso no autorizado.");
            logout();
        }
        long totalUsuarios = Usuario.count();
        long totalAlumnos = Usuario.count("rol = ?1", Rol.ALUMNO);
        long totalProfesores = Usuario.count("rol = ?1", Rol.PROFESOR);
        List<Inscripcion> misAlumnos = Inscripcion.find("byProfesor", profesor).fetch();
        renderTemplate("Application/panelProfesor.html", profesor, misAlumnos,
                totalUsuarios, totalAlumnos, totalProfesores);
    }

    // --- RESERVAS DE CLASE ---
    public static void reservas() {
        Usuario yo = connected();
        if (yo == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }

        List<Reserva> reservas;
        List<Inscripcion> opciones;

        if (yo.rol == Rol.ALUMNO) {
            reservas = Reserva.find("alumno = ?1 ORDER BY fechaReserva ASC", yo).fetch();
            opciones = Inscripcion.find("byAlumno", yo).fetch();
        } else if (yo.rol == Rol.PROFESOR) {
            reservas = Reserva.find("profesor = ?1 ORDER BY fechaReserva ASC", yo).fetch();
            opciones = Inscripcion.find("byProfesor", yo).fetch();
        } else {
            flash.error("Rol no soportado para reservas.");
            index();
            return;
        }

        Date ahora = new Date();
        renderTemplate("Application/reservas.html", yo, reservas, opciones, ahora);
    }

    public static void crearReserva(Long inscripcionId, String fecha, String hora) {
        Usuario yo = connected();
        if (yo == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }

        if (inscripcionId == null) {
            flash.error("Selecciona una inscripción válida.");
            reservas();
            return;
        }

        Inscripcion inscripcion = Inscripcion.findById(inscripcionId);
        if (inscripcion == null) {
            flash.error("La inscripción seleccionada no existe.");
            reservas();
            return;
        }

        if (!yo.id.equals(inscripcion.alumno.id) && !yo.id.equals(inscripcion.profesor.id)) {
            flash.error("No tienes permiso para crear reservas con esa inscripción.");
            reservas();
            return;
        }

        if (fecha == null || fecha.trim().isEmpty() || hora == null || hora.trim().isEmpty()) {
            flash.error("Indica la fecha y la hora de la clase.");
            reservas();
            return;
        }

        Date fechaReserva;
        try {
            SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            formato.setLenient(false);
            fechaReserva = formato.parse(fecha.trim() + " " + hora.trim());
        } catch (ParseException e) {
            flash.error("Formato de fecha u hora inválido.");
            reservas();
            return;
        }

        Reserva nueva = new Reserva(inscripcion.profesor, inscripcion.alumno, inscripcion.materia, fechaReserva);
        nueva.save();

        flash.success("Reserva creada para " + inscripcion.materia.nombre + " el " + fecha + " a las " + hora + ".");
        reservas();
    }

    public static void video(Long reservaId) {
        Usuario yo = connected();
        if (yo == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }

        Reserva reserva = Reserva.findById(reservaId);
        if (reserva == null) {
            flash.error("La reserva solicitada no existe.");
            reservas();
            return;
        }

        if (!yo.id.equals(reserva.alumno.id) && !yo.id.equals(reserva.profesor.id)) {
            flash.error("No tienes permiso para acceder a esta sala.");
            reservas();
            return;
        }

        renderTemplate("Application/video.html", yo, reserva);
    }

    public static void resetearSesionVideo(Long reservaId) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        if (reserva == null) return;

        reserva.reiniciarSesion();
        reserva.save();

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "ok");
        renderJSON(resp);
    }

    public static void publicarOffer(Long reservaId, String sdp) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        if (reserva == null) return;

        if (sdp == null || sdp.trim().isEmpty()) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "SDP vacío");
            renderJSON(error);
            return;
        }

        reserva.offerSdp = sdp.trim();
        reserva.offerActualizada = new Date();
        reserva.answerSdp = null;
        reserva.answerActualizada = null;
        reserva.save();

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "ok");
        renderJSON(resp);
    }

    public static void publicarAnswer(Long reservaId, String sdp) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        if (reserva == null) return;

        if (sdp == null || sdp.trim().isEmpty()) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "SDP vacío");
            renderJSON(error);
            return;
        }

        reserva.answerSdp = sdp.trim();
        reserva.answerActualizada = new Date();
        reserva.save();

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "ok");
        renderJSON(resp);
    }

    public static void obtenerOffer(Long reservaId) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        if (reserva == null) return;

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("sdp", reserva.offerSdp);
        resp.put("type", reserva.offerSdp != null ? "offer" : null);
        resp.put("timestamp", reserva.offerActualizada != null ? reserva.offerActualizada.getTime() : null);
        renderJSON(resp);
    }

    public static void obtenerAnswer(Long reservaId) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        if (reserva == null) return;

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("sdp", reserva.answerSdp);
        resp.put("type", reserva.answerSdp != null ? "answer" : null);
        resp.put("timestamp", reserva.answerActualizada != null ? reserva.answerActualizada.getTime() : null);
        renderJSON(resp);
    }

    private static Reserva obtenerReservaAutorizada(Long reservaId) {
        Usuario yo = connected();
        if (yo == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return null;
        }

        Reserva reserva = Reserva.findById(reservaId);
        if (reserva == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "Reserva no encontrada");
            renderJSON(error);
            return null;
        }

        if (!yo.id.equals(reserva.alumno.id) && !yo.id.equals(reserva.profesor.id)) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "No autorizado");
            renderJSON(error);
            return null;
        }

        return reserva;
    }

    // --- CONSULTAS PERSONALIZADAS ---
    public static void consultas(String tipo, Long materiaId) {
        Usuario yo = connected();
        if (yo == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }

        List<Materia> materias = Materia.findAll();
        Materia materiaSeleccionada = null;
        Long resultado = null;
        String descripcion = null;
        String error = null;
        List<Usuario> detalleUsuarios = new ArrayList<Usuario>();

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
            } else {
                error = "Consulta no reconocida.";
            }
        }

        renderTemplate("Application/consultas.html", yo, materias, tipo, materiaSeleccionada,
                resultado, descripcion, detalleUsuarios, error);
    }

    // --- DETALLE DE MATERIA ---
    public static void detalle(Long id) {
        Materia m = Materia.findById(id);
        Usuario u = connected();

        if (u == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }

        if (m == null) {
            flash.error("Materia no encontrada");
            if (u.rol == Rol.ALUMNO) panelAlumno();
            else panelProfesor();
        }

        List<Usuario> profesores = Usuario.find("byRol", Rol.PROFESOR).fetch();
        renderTemplate("Application/detalle.html", m, u, profesores);
    }

    // --- INSCRIPCIÓN ---
    public static void inscribirse(Long id, Long profesorId) {
        Usuario alumno = connected();
        if (alumno.rol != Rol.ALUMNO) {
            flash.error("Solo los alumnos pueden inscribirse.");
            logout();
        }

        Materia m = Materia.findById(id);
        Usuario profesor = Usuario.findById(profesorId);

        if (m != null && profesor != null) {
            Inscripcion existente = Inscripcion.find("byAlumnoAndMateriaAndProfesor", alumno, m, profesor).first();
            if (existente == null) {
                new Inscripcion(alumno, profesor, m).save();
                flash.success("Te has inscrito correctamente en " + m.nombre);
                panelAlumno();
            } else {
                flash.error("Ya estás inscrito en esta materia.");
                panelAlumno();
            }
        } else {
            flash.error("Error al inscribirse (materia o profesor no válidos).");
            panelAlumno();
        }
    }

    // --- VISTA DE CHAT ---
    public static void chat(Long alumnoId, Long profesorId) {
        Usuario alumno = Usuario.findById(alumnoId);
        Usuario profesor = Usuario.findById(profesorId);
        if (alumno == null || profesor == null) renderText("Error: usuario no encontrado");

        Usuario yo = connected();
        if (yo == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }
        if (!yo.id.equals(alumno.id) && !yo.id.equals(profesor.id)) {
            flash.error("No tienes permiso para ver este chat.");
            index();
        }

        List<Mensaje> mensajes = Mensaje.find(
                "(emisor = ?1 AND receptor = ?2) OR (emisor = ?3 AND receptor = ?4) ORDER BY fecha ASC",
                alumno, profesor, profesor, alumno
        ).fetch();

        List<Mensaje> mensajesValidos = new ArrayList<Mensaje>();
        for (Mensaje mensaje : mensajes) {
            if (mensaje != null && mensaje.emisor != null && mensaje.receptor != null) {
                mensajesValidos.add(mensaje);
            }
        }

        mensajes = mensajesValidos;
        render(alumno, profesor, mensajes, yo);
    }


    // --- ENVIAR MENSAJE (API) ---
    public static void enviarMensaje(Long receptorId, String contenido) {
        Usuario emisor = connected();
        Usuario receptor = Usuario.findById(receptorId);

        if (emisor == null || receptor == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "Usuario no encontrado");
            renderJSON(error);
            return;
        }

        if (!((emisor.rol == Rol.ALUMNO && receptor.rol == Rol.PROFESOR) ||
                (emisor.rol == Rol.PROFESOR && receptor.rol == Rol.ALUMNO))) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "Chat no permitido");
            renderJSON(error);
            return;
        }

        if (contenido == null || contenido.trim().isEmpty()) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "Contenido vacío");
            renderJSON(error);
            return;
        }

        Mensaje nuevo = new Mensaje(emisor, receptor, contenido.trim());
        nuevo.save();

        renderJSON(toMensajeDto(nuevo));
    }

    // --- OBTENER MENSAJES (API) ---
    public static void obtenerMensajes(Long alumnoId, Long profesorId, Long lastId) {
        Usuario alumno = Usuario.findById(alumnoId);
        Usuario profesor = Usuario.findById(profesorId);

        if (alumno == null || profesor == null) {
            renderJSON(Collections.emptyList());
            return;
        }

        Usuario yo = connected();
        if (!yo.id.equals(alumno.id) && !yo.id.equals(profesor.id)) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "No autorizado");
            renderJSON(error);
            return;
        }

        String baseQuery = "((emisor = ?1 AND receptor = ?2) OR (emisor = ?3 AND receptor = ?4))";
        List<Mensaje> mensajes;
        if (lastId != null) {
            mensajes = Mensaje.find(baseQuery + " AND id > ?5 ORDER BY fecha ASC",
                    alumno, profesor, profesor, alumno, lastId
            ).fetch();
        } else {
            mensajes = Mensaje.find(baseQuery + " ORDER BY fecha ASC",
                    alumno, profesor, profesor, alumno
            ).fetch();
        }

        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        for (Mensaje mensaje : mensajes) {
            if (mensaje != null && mensaje.emisor != null && mensaje.receptor != null) {
                data.add(toMensajeDto(mensaje));
            }
        }

        renderJSON(data);
    }

    // --- LOGOUT ---
    public static void logout() {
        session.clear();
        flash.success("Has cerrado sesión correctamente.");
        index();
    }

    private static Map<String, Object> toMensajeDto(Mensaje mensaje) {
        Map<String, Object> emisorDto = new HashMap<String, Object>();
        emisorDto.put("id", mensaje.emisor.id);
        emisorDto.put("username", mensaje.emisor.username);

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("id", mensaje.id);
        resp.put("emisor", emisorDto);
        resp.put("contenido", mensaje.contenido);
        resp.put("fecha", mensaje.fecha);

        return resp;
    }
}