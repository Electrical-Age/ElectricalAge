package li.cil.oc.api.machine;

/**
 * Used to signal that the direct call limit for the current server tick has
 * been reached in {@link li.cil.oc.api.machine.Machine#invoke(String, String, Object[])}.
 */
public class LimitReachedException extends Exception {
}
