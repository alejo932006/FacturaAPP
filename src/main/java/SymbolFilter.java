import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

    /**
     * Un filtro de documento que bloquea la inserción de caracteres específicos.
     */
    public class SymbolFilter extends DocumentFilter {

        private final String blockedSymbols;

        /**
         * Constructor que recibe los símbolos a bloquear.
         * @param blockedSymbols Un String que contiene todos los caracteres no permitidos.
         */
        public SymbolFilter(String blockedSymbols) {
            this.blockedSymbols = blockedSymbols;
        }

        // Este método se llama cuando se inserta texto nuevo (ej. al escribir o pegar)
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (stringContainsBlockedSymbols(string)) {
                return; // Si el texto contiene un símbolo bloqueado, no hacemos nada (se bloquea)
            }
            super.insertString(fb, offset, string, attr); // Si no, se permite la inserción
        }

        // Este método se llama cuando se reemplaza texto existente
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (stringContainsBlockedSymbols(text)) {
                return; // Bloqueamos el reemplazo si contiene símbolos no permitidos
            }
            super.replace(fb, offset, length, text, attrs); // Si no, se permite el reemplazo
        }

        /**
         * Comprueba si un texto contiene cualquiera de los símbolos bloqueados.
         */
        private boolean stringContainsBlockedSymbols(String text) {
            if (text == null) {
                return false;
            }
            for (char c : blockedSymbols.toCharArray()) {
                if (text.indexOf(c) != -1) {
                    return true; // Se encontró un símbolo bloqueado
                }
            }
            return false;
        }
    }