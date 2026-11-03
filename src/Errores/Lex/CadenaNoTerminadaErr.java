package Errores.Lex;

import java.util.List;

import Errores.CodError;
import Errores.Err;
import Errores.TipoError;

public class CadenaNoTerminadaErr extends Err {
    public CadenaNoTerminadaErr(int linea, int columna, String lex, List<String> codigoFuente) {
        super(linea, columna,linea,linea, TipoError.LEXICO, CodError.CADENA_NO_TERMINADA.ordinal(),"Cadena no terminada -> " + lex, codigoFuente);
    }
}
