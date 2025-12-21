// Archivo nuevo: TextUtil.java
import java.text.Normalizer;

public class TextUtil {

    /**
     * Elimina las tildes y otros diacríticos de un texto.
     * Por ejemplo: "POLÍTICAS DE GARANTÍA" se convierte en "POLITICAS DE GARANTIA".
     */
    public static String quitarTildes(String texto) {
        if (texto == null) {
            return null;
        }
        // Normaliza el texto para descomponer los caracteres con tilde
        String textoNormalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        // Elimina los caracteres diacríticos (las tildes) usando una expresión regular
        return textoNormalizado.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}