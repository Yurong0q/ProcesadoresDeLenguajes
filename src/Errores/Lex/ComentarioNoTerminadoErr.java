package Errores.Lex;

import java.util.List;

import Errores.CodError;
import Errores.Err;
import Errores.TipoError;

public class ComentarioNoTerminadoErr extends Err{
    public ComentarioNoTerminadoErr(int linea, int columna, List<String> codigoFuente) {
        super(linea, columna,linea,linea,TipoError.LEXICO, CodError.COMENTARIO_NO_TERMINADO.ordinal(), "Comentario no terminado.",codigoFuente);
    }
}
