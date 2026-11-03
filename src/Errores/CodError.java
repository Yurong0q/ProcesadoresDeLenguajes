package Errores;
public enum CodError{
    //Analizador lexico
    CADENA_TOO_LARGE,
    INT_TOO_LARGE,
    REAL_ERROR,
    INVALID_CARACTER,
    CADENA_NO_TERMINADA,
    COMENTARIO_NO_TERMINADO,
    FALTA_DECIMAL,

    //Analizador Sintactico
    TOKEN_INESPERADO,

    //Analizador Semantico
    ERR_SEMANTICO

}