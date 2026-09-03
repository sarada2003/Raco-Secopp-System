package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.entity.Usuario;

/**
 * Propaga el usuario autenticado al virtual thread donde {@link ChatbotService} ejecuta
 * el prompt: {@code SecurityContextHolder} es un {@code ThreadLocal} normal y no viaja
 * solo al hilo del executor, pero la llamada a la herramienta 'buscarContratosSecop'
 * (en {@code SecopAiFunctionConfig}, package distinto) ocurre síncronamente dentro de
 * ese mismo hilo, así que un ThreadLocal propio sí alcanza a leerse ahí.
 */
public final class ChatContext {

    private static final ThreadLocal<Usuario> USUARIO = new ThreadLocal<>();

    private ChatContext() {}

    public static void set(Usuario usuario) {
        USUARIO.set(usuario);
    }

    public static Usuario get() {
        return USUARIO.get();
    }

    public static void clear() {
        USUARIO.remove();
    }
}
