package Errores.Lex;

import java.util.List;

import Errores.CodError;
import Errores.Err;
import Errores.TipoError;

public class CadenaTooLargeErr extends Err {
    public CadenaTooLargeErr(int linea, int columna, String lex, int length, List<String> codigoFuente) {
        super(linea, columna,linea,linea, TipoError.LEXICO, CodError.CADENA_TOO_LARGE.ordinal(),
              "Cadena: " + lex + " demasiado grande ("+length+" > 64).", codigoFuente);
    }
}