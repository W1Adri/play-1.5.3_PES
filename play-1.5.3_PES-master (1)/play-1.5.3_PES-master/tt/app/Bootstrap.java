import play.jobs.Job;
import play.jobs.OnApplicationStart;
import models.*; // Importa TODOS los modelos (Usuario, Rol, Materia, Inscripcion, Mensaje)
import play.libs.Crypto; // Importar Crypto para hashear
import java.util.Calendar;
import java.util.Date;

@OnApplicationStart
public class Bootstrap extends Job {

    public void doJob() {

        // --- 1. Crear Materias ---
        if (Materia.count() == 0) { // 0L no es necesario, '0' funciona
            new Materia("MAT101", "Matemáticas", "Álgebra y geometría.").save();
            new Materia("PROG1", "Programación I", "Introducción a Java.").save();
            new Materia("ENG202", "Inglés Técnico", "Vocabulario profesional.").save();
        }

        // --- 2. Crear Usuarios ---
        if (Usuario.count() == 0) { // <-- Cambiado a Usuario

            // Hasheamos la contraseña "1234" UNA SOLA VEZ
            String passHash = Crypto.passwordHash("1234");

            // Usamos 'new Usuario', el 'passHash' y el 'Enum Rol'
            new Usuario("laura", passHash, "laura@uni.com", "Laura García", Rol.PROFESOR).save();
            new Usuario("carlos", passHash, "carlos@uni.com", "Carlos Ruiz", Rol.PROFESOR).save();
            new Usuario("marta", passHash, "marta@uni.com", "Marta Fernández", Rol.PROFESOR).save();
            new Usuario("roger", passHash, "roger@uni.com", "Roger Vidal", Rol.ALUMNO).save();
        }

        // --- 2b. Crear usuario admin si no existe ---
        if (Usuario.find("byUsername", "admin").first() == null) {
            String adminHash = Crypto.passwordHash("admin");
            new Usuario("admin", adminHash, "admin@clases.com", "Administrador", Rol.PROFESOR).save();
        }

        // --- 3. Crear Inscripción de prueba ---

        // Buscamos los usuarios con el tipo 'Usuario' y la sintaxis limpia
        Usuario alumno = Usuario.find("byUsername", "roger").first();
        Usuario profL = Usuario.find("byUsername", "laura").first();
        Usuario profC = Usuario.find("byUsername", "carlos").first();
        Materia mat101 = Materia.find("byCodigo", "MAT101").first();
        Materia prog1 = Materia.find("byCodigo", "PROG1").first();

        if (alumno != null && profL != null && mat101 != null) {
            // Comprobamos si la inscripción ya existe
            Inscripcion ya = Inscripcion.find("byAlumnoAndMateriaAndProfesor", alumno, mat101, profL).first();
            if (ya == null) {
                new Inscripcion(alumno, profL, mat101).save();
            }
        }

        if (alumno != null && profC != null && prog1 != null) {
            Inscripcion segunda = Inscripcion.find("byAlumnoAndMateriaAndProfesor", alumno, prog1, profC).first();
            if (segunda == null) {
                new Inscripcion(alumno, profC, prog1).save();
            }
        }

        // --- 4. Crear Mensajes de prueba ---
        if (Mensaje.count() == 0) {
            if (alumno != null && profC != null) {
                new Mensaje(alumno, profC, "Hola profe, ¿podemos ver derivadas mañana?").save();
                new Mensaje(profC, alumno, "¡Claro! A las 17:00 te va bien.").save();
            }
        }

        // --- 5. Crear Reservas de prueba ---
        if (Reserva.count() < 2 && alumno != null && profL != null && mat101 != null) {
            Calendar calendario = Calendar.getInstance();
            calendario.add(Calendar.DAY_OF_MONTH, 1);
            calendario.set(Calendar.HOUR_OF_DAY, 10);
            calendario.set(Calendar.MINUTE, 0);
            Date fecha1 = calendario.getTime();

            calendario.add(Calendar.DAY_OF_MONTH, 1);
            calendario.set(Calendar.HOUR_OF_DAY, 16);
            calendario.set(Calendar.MINUTE, 30);
            Date fecha2 = calendario.getTime();

            Reserva primera = Reserva.find("alumno = ?1 AND profesor = ?2 AND materia = ?3", alumno, profL, mat101).first();
            if (primera == null) {
                new Reserva(profL, alumno, mat101, fecha1).save();
            }

            if (profC != null && prog1 != null) {
                Reserva segunda = Reserva.find("alumno = ?1 AND profesor = ?2 AND materia = ?3", alumno, profC, prog1).first();
                if (segunda == null) {
                    new Reserva(profC, alumno, prog1, fecha2).save();
                }
            }
        }
    }
}
