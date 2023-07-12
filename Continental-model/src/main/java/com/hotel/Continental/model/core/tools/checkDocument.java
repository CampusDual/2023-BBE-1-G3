package com.hotel.continental.model.core.tools;

public class CheckDocument {

    private CheckDocument() {
        throw new IllegalStateException("Utility Class");
    }

    /**
     * Metodo que comprueba si el documento es valido
     *
     * @param document    Documento
     * @param countryCode Codigo de pais
     * @return true si es valido, false si no lo es
     */
    public static boolean checkDocument(String document, String countryCode) {
        if (countryCode.equals("ES")) {
            String dniRegex = "^\\d{8}[A-HJ-NP-TV-Z]$";
            String nieRegex = "^[XYZ]\\d{7}[A-Z]$";
            String cifRegex = "^([ABCDEFGHJKLMNPQRSUVW])(\\d{7})([0-9A-J])$";
            if (document.matches(dniRegex)) {
                String dniNumbers = document.substring(0, 8);
                String dniLetter = document.substring(8).toUpperCase();

                String validLetters = "TRWAGMYFPDXBNJZSQVHLCKE";
                int dniMod = Integer.parseInt(dniNumbers) % 23;
                char calculatedLetter = validLetters.charAt(dniMod);
                return dniLetter.charAt(0) == calculatedLetter;
            }
            String firstLetter = document.substring(0,1);
            if ((firstLetter.equals("Z") || firstLetter.equals("X") || firstLetter.equals("Y")) && document.matches(nieRegex)) {
                String nieNumbers = document.substring(1, 8);
                String validLetters = "TRWAGMYFPDXBNJZSQVHLCKE";
                String lastLetter = document.substring(8);
                int nieMod = Integer.parseInt(nieNumbers) % 23;
                char calculatedLetter = validLetters.charAt(nieMod);
                return lastLetter.charAt(0) == calculatedLetter;

            }
            return document.matches(cifRegex);
        }
        return true;
    }
}
