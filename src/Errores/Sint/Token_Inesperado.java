package Errores.Sint;

import java.util.List;

import Enumeraciones.TipoToken;
import Errores.CodError;
import Errores.Err;
import Errores.TipoError;

public class Token_Inesperado extends Err{
    
    public Token_Inesperado (int lineaAnterior, int columnaAnterior,int lineaSiguiente, int lineaS, String contexto, 
     TipoToken encontrado, String esperado, List<String> codigoFuente, String lexTk, Integer atrEncontrado, Integer atrEsperado){
        super(lineaAnterior, columnaAnterior,lineaSiguiente,lineaS,TipoError.SINTACTICO,CodError.TOKEN_INESPERADO.ordinal(),
        ConstructorMensaje(contexto, encontrado, esperado, lexTk, atrEncontrado, atrEsperado), codigoFuente);
    }

    public Token_Inesperado (int lineaAnterior, int columnaAnterior,int lineaSiguiente, int lineaS, String contexto, 
     TipoToken encontrado, TipoToken esperado, List<String> codigoFuente, String lexTk, Integer atrEncontrado, Integer atrEsperado){
        super(lineaAnterior, columnaAnterior,lineaSiguiente,lineaS,TipoError.SINTACTICO,CodError.TOKEN_INESPERADO.ordinal(),
        ConstructorMensaje(contexto, encontrado, normalizarMensajeEsp(esperado, lexTk, atrEsperado), lexTk, atrEncontrado, atrEsperado), codigoFuente);
    }

    private static String normalizarMensajeEsp(TipoToken tt, String lexTk, Integer atributo){
        String res;
        switch (tt) {

            case TYPE_IDENTIFICADOR:  
                res = "un identificador";
                break;

            case TYPE_ENTERO:
                res = "un numero entero";
                break;

            case TYPE_REAL:
                res = "un numero real";
                break;

            case TYPE_CADENA:
                res = "una cadena";
                break;

            case TYPE_ARITMETICO:
                if (atributo == 0)
                    res = "el operador aritmetico '+'";
                else
                    res = "el operador aritmetico '-'";
                break;

            case TYPE_NEGACION:
                res = "el operador de negacion '!'";
                break;

            case TYPE_RELACION:
                if (atributo == 0)
                    res = "el operador relacional '>'";
                else
                    res = "el operador relacional '<'";
                break;

            case TYPE_AND:
                res = "el operador logico '&&'";
                break; 

            case TYPE_ASIGNACION:
                if (atributo == 0)
                    res = "el simbolo de asignacion '='";
                else
                    res = "el simbolo de asignacion '|='";
                break;

            case TYPE_PARENTESIS:
                if (atributo == 0)
                    res = "un parentesis abierto '('";
                else
                    res = "un parentesis cerrado ')'";
                break;

            case TYPE_LLAVE:
                if (atributo == 0)
                    res = "una llave abierta '{'";
                else
                    res = "una llave cerrada '}'";
                break;

            case TYPE_PUNTO_Y_COMA:
                res = "un punto y coma ';'";
                break;

            case TYPE_PUNTO:
                res = "un punto '.'";
                break;

            case TYPE_EOF:
                res = "el fin de fichero";
                break;

            case TYPE_LET:
                res = "la palabra reservada 'let'";
                break;

            case TYPE_INT:
                res = "la palabra reservada 'int'";
                break;

            case TYPE_FLOAT:
                res = "la palabra reservada 'float'";
                break;

            case TYPE_BOOLEAN:
                res = "la palabra reservada 'boolean'";
                break;

            case TYPE_STRING:
                res = "la palabra reservada 'string'";
                break;

            case TYPE_VOID:
                res = "la palabra reservada 'void'";
                break;

            case TYPE_FOR:
                res = "el inicio de un bucle 'for'";
                break;

            case TYPE_FUNCTION:
                res = "el inicio de una declaracion de funcion 'function'";
                break;

            case TYPE_IF:
                res = "el inicio de una sentencia condicional 'if'";
                break;

            case TYPE_RETURN:
                res = "la palabra reservada 'return'";
                break;

            case TYPE_READ:
                res = "la palabra reservada 'read'";
                break;

            case TYPE_WRITE:
                res = "la palabra reservada 'write'";
                break;

            case TYPE_COMA:
                res = "una coma ','";
                break;

            default:
                res = "un token valido";
                break;
        }
        return res;
    }


    private static String normalizarMensaje(TipoToken tt, String lexTk, Integer atributo){
        String res = "";
        switch (tt) {

            case TYPE_IDENTIFICADOR:  
                res = "Se ha encontrado el identificador '" + lexTk + "'";
                break;

            case TYPE_ENTERO:
                res = "Se ha encontrado un numero entero";
                break;

            case TYPE_REAL:
                res = "Se ha encontrado un numero real";
                break;

            case TYPE_CADENA:
                res = "Se ha encontrado una cadena";
                break;

            case TYPE_ARITMETICO:
                if (atributo == 0)
                    res = "Se ha encontrado el operador aritmetico '+'";
                else
                    res = "Se ha encontrado el operador aritmetico '-'";
                break;

            case TYPE_NEGACION:
                res = "Se ha encontrado el operador de negacion '!'";
                break;

            case TYPE_RELACION:
                if (atributo == 0)
                    res = "Se ha encontrado el operador relacional '>'";
                else
                    res = "Se ha encontrado el operador relacional '<'";
                break;

            case TYPE_AND:
                res = "Se ha encontrado el operador logico '&&'";
                break; 

            case TYPE_ASIGNACION:
                if (atributo == 0)
                    res = "Se ha encontrado el simbolo de asignacion '='";
                else
                    res = "Se ha encontrado el simbolo de asignacion '|='";
                break;

            case TYPE_PARENTESIS:
                if (atributo == 0)
                    res = "Se ha encontrado el parentesis abierto '('";
                else
                    res = "Se ha encontrado el parentesis cerrado ')'";
                break;

            case TYPE_LLAVE:
                if (atributo == 0)
                    res = "Se ha encontrado la llave abierta '{'";
                else
                    res = "Se ha encontrado la llave cerrada '}'";
                break;

            case TYPE_PUNTO_Y_COMA:
                res = "Se ha encontrado un punto y coma ';'";
                break;

            case TYPE_PUNTO:
                res = "Se ha encontrado un punto '.'";
                break;

            case TYPE_EOF:
                res = "Se ha acabado el fichero";
                break;

            case TYPE_LET:
                res = "Se ha encontrado la palabra reservada 'let'";
                break;

            case TYPE_INT:
                res = "Se ha encontrado la palabra reservada 'int'";
                break;

            case TYPE_FLOAT:
                res = "Se ha encontrado la palabra reservada 'float'";
                break;

            case TYPE_BOOLEAN:
                res = "Se ha encontrado la palabra reservada 'boolean'";
                break;

            case TYPE_STRING:
                res = "Se ha encontrado la palabra reservada 'string'";
                break;

            case TYPE_VOID:
                res = "Se ha encontrado la palabra reservada 'void'";
                break;

            case TYPE_FOR:
                res = "Se ha encontrado el inicio de un bucle 'for'";
                break;

            case TYPE_FUNCTION:
                res = "Se ha encontrado el inicio de una declaracion de funcion 'function'";
                break;

            case TYPE_IF:
                res = "Se ha encontrado el inicio de un condicional 'if'";
                break;

            case TYPE_RETURN:
                res = "Se ha encontrado la palabra reservada 'return'";
                break;

            case TYPE_READ:
                res = "Se ha encontrado la palabra reservada 'read'";
                break;

            case TYPE_WRITE:
                res = "Se ha encontrado la palabra reservada 'write'";
                break;

            case TYPE_COMA:
                res = "Se ha encontrado una coma ','";
                break;

            default:
                res = "token desconocido";
                break;
        }
            return res;
    }

    //Contructor para el mensaje a pasar como descrip
    private static String ConstructorMensaje (String contexto, TipoToken encontrado, String esperado, 
        String lexTk, Integer atrEncontrado, Integer atrEsperado){
        return contexto + "\n"+ normalizarMensaje(encontrado,lexTk, atrEncontrado) + 
            "\nSe esperaba: "+esperado;
    }
}