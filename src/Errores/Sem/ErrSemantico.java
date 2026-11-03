package Errores.Sem;

import java.util.List;

import Errores.CodError;
import Errores.Err;
import Errores.TipoError;

public class ErrSemantico extends Err{
    
    public ErrSemantico (int lineaAnterior, int columnaAnterior,int lineaSiguiente, int lineaS, String contexto, String desc, List<String> codigoFuente){
        super(lineaAnterior, columnaAnterior,lineaSiguiente,lineaS,TipoError.SEMANTICO,CodError.ERR_SEMANTICO.ordinal(),
       contexto+"\n"+desc, codigoFuente);
    }

}
