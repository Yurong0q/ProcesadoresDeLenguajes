package Errores.Lex;

import java.util.List;

import Errores.CodError;
import Errores.Err;
import Errores.TipoError;

public class IntTooLargeErr extends Err {
    public IntTooLargeErr(int linea, int columna, int num, List<String> codigoFuente) {
        super(linea, columna,linea,linea,TipoError.LEXICO, CodError.INT_TOO_LARGE.ordinal(),
              "Entero demasiado grande ("+num+" > 32767).", codigoFuente);
    }
}