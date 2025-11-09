package cl.ufro.dci.naivepayapi.registro.service;

public final class RutUtils {

    private RutUtils() {
    }

    /**
     * Valida un RUT chileno completo en formato String.
     * @param rut El RUT completo (ej: "12.345.678-9").
     * @return true si el RUT es válido, false en caso contrario.
     */
    public static boolean isValid(String rut) {
        if (rut == null || rut.trim().isEmpty()) {
            return false;
        }

        String cleanedRut = rut.trim().toUpperCase().replace(".", "").replace("-", "");

        if (!cleanedRut.matches("^[0-9]+[0-9K]$")) {
            return false;
        }

        String body = cleanedRut.substring(0, cleanedRut.length() - 1);
        char dv = cleanedRut.charAt(cleanedRut.length() - 1);

        try {
            long rutNumber = Long.parseLong(body);
            int sum = 0;
            int multiplier = 2;

            for (; rutNumber > 0; rutNumber /= 10) {
                sum += (rutNumber % 10) * multiplier;
                multiplier++;
                if (multiplier > 7) {
                    multiplier = 2;
                }
            }

            int remainder = sum % 11;
            int result = 11 - remainder;
            char expectedDv;

            if (result == 11) {
                expectedDv = '0';
            } else if (result == 10) {
                expectedDv = 'K';
            } else {
                expectedDv = (char) (result + '0');
            }

            return dv == expectedDv;

        } catch (NumberFormatException e) {
            return false;
        }
    }
}
