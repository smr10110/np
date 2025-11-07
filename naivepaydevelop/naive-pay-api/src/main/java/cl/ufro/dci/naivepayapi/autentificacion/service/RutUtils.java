package cl.ufro.dci.naivepayapi.autentificacion.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RutUtils {
    private static final Pattern RUT_DV = Pattern.compile("^\\s*([0-9]{1,12})-([0-9kK])\\s*$");

    public static boolean isEmail(String s) {
        return s != null && s.contains("@");
    }

    public static Optional<Rut> parseRut(String s) {
        if (s == null) return Optional.empty();
        Matcher m = RUT_DV.matcher(s);
        if (!m.matches()) return Optional.empty();
        String rut = m.group(1);
        char dv = Character.toUpperCase(m.group(2).charAt(0));
        return Optional.of(new Rut(rut, dv));
    }

    public record Rut(String rut, char dv) {}
}
