package Errores.Lex;

import java.util.List;

import Errores.CodError;
import Errores.Err;
import Errores.TipoError;

public class InvalidCaracterErr extends Err {
    public InvalidCaracterErr(int linea, int columna, Character car, List<String> codigoFuente) {
        super(linea, columna,linea,linea, TipoError.LEXICO, CodError.INVALID_CARACTER.ordinal(),
              "Carácter inválido: " + "\'" + car + "\' .", codigoFuente);
    }
}