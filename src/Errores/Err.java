package Errores;

import java.util.List;

public abstract class Err extends RuntimeException {

    public Err(int lineaAntigua, int columnaAntigua, int lineaSiguiente, int lineaS, TipoError tipoError, int codError, String descrip, List<String> codigoFuente) {
        
        super(constructorMensaje(lineaAntigua, columnaAntigua,lineaSiguiente, lineaS, tipoError, codError, descrip, codigoFuente));
        
    }

    private static String constructorMensaje (int lineaAntigua, int columnaAntigua, int lineaSiguiente, int lineaS, TipoError tipoError, int codError, String descrip, List<String> codigoFuente){
        
        String mensaje = "\n------ ERROR " + tipoError + " ------\n";
        mensaje += "(" + lineaAntigua + "," + columnaAntigua + ") " +"ERR-" +codError + ": "; //linea y columna de token ya consumido (ultimo bueno)
        mensaje += descrip + "\n";
        mensaje += "\nCONTEXTO DEL ERROR:\n";
        // Asegurar rangos válidos
        int inicio = Math.max(1, lineaS);
        int fin = Math.min(codigoFuente.size(), lineaSiguiente);  
    
        // Imprimir todas las líneas desde S hasta Siguiente (Contexto del error)
        for (int linea = inicio; linea <= fin; linea++) {

            String texto = codigoFuente.get(linea - 1);

            //Imprimimos linea de forma: " xx| texto" (Espero que no haya más de 99 líneas :) )
            mensaje += String.format("%2d| %s\n", linea, texto);
        }
        mensaje += "--------------------------\n";
        return mensaje;
    }  
}