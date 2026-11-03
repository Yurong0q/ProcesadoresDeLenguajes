package Errores.Lex;

import java.util.List;

import Errores.CodError;
import Errores.Err;
import Errores.TipoError;

public class RealTooLargeErr extends Err {
    public RealTooLargeErr(int linea, int columna, double num, List<String> codigoFuente) {
        super(linea, columna,linea,linea,TipoError.LEXICO, CodError.REAL_ERROR.ordinal(),
              "Número real demasiado grande  (num:" + num + " > 117549436.0).", codigoFuente);
    }
}