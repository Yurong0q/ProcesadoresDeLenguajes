package Errores.Lex;

import java.util.List;

import Errores.CodError;
import Errores.Err;
import Errores.TipoError;

public class FaltaDecimal extends Err {
    public FaltaDecimal(int linea, int columna, List<String> codigoFuente) {
        super(linea, columna,linea,linea,TipoError.LEXICO, CodError.FALTA_DECIMAL.ordinal(),
              "Se detecto una ausencia de decimales.", codigoFuente);
    }
}