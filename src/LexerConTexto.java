import es.upm.aedlib.Pair;
import java.io.FileWriter;
import java.io.IOException;

//LexerConTexto es una clase intermedia para pasarle tokens al sint-sem mientras imprimo
//el token ya en un archivo .txt
public class LexerConTexto {

    private Analizador_Lexico aLex;
    private FileWriter writer;

    public LexerConTexto(Analizador_Lexico aLex, String archivoTokens) throws IOException {
        this.aLex = aLex;
        this.writer = new FileWriter(archivoTokens);
    }

    /**
     * Devuelve el siguiente token junto con su posición (linea, columna) mientras
     * escribe en
     * archivo
     */
    public Pair<Pair<Token,Integer>, Pair<Integer, Integer>> nextTokenWithPos() {

        Pair<Pair<Token,Integer>, Pair<Integer, Integer>> par = aLex.iniciar();
        return par;
    }

    /**
     * Imprime token
     * 
     * @param token token a imprimir
     */
    public void imprimirToken(Token token) {
        try {
            if (token != null) {
                escribirToken(token); // Escribe en archivo
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Escritura del token en el archivo (deberia ser igual al que teniamos antes)
     */
    private void escribirToken(Token token) throws IOException {
        if (token == null)
            return;

        String salida = "<" + token.getTipo().ordinal() + ", ";

        switch (token.getTipo()) {
            case TYPE_CADENA:
                salida += "\"" + token.getLex() + "\"";
                break;
            case TYPE_REAL:
                salida += token.getNum();
                break;
            case TYPE_ENTERO:
                salida += (int) token.getAtributo();
                break;
            default:
                // Si tiene atributo (ej: posición en TS) lo imprime; si no, deja vacío
                if (token.getAtributo() != null)
                    salida += token.getAtributo();
                break;
        }

        salida += ">\n";
        writer.write(salida);
    }

    /**
     * Cierra el archivo de tokens
     */
    public void close() {
        try {
            if (writer != null)
                writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
