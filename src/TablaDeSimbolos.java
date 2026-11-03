import tslib.TS_Gestor;
import tslib.TS_Gestor.DescripcionAtributo;
import tslib.TS_Gestor.TipoDatoAtributo;

public class TablaDeSimbolos {
    private TS_Gestor gestorTs;

    public TablaDeSimbolos (TS_Gestor gestor){
        this.gestorTs = gestor;
    }

    public void iniciar(){
        gestorTs.createTPalabrasReservadas();
        gestorTs.addEntradaTPalabrasReservadas("let");
        gestorTs.addEntradaTPalabrasReservadas("int");
        gestorTs.addEntradaTPalabrasReservadas("float");
        gestorTs.addEntradaTPalabrasReservadas("boolean");
        gestorTs.addEntradaTPalabrasReservadas("string");
        gestorTs.addEntradaTPalabrasReservadas("void");
        gestorTs.addEntradaTPalabrasReservadas("for");
        gestorTs.addEntradaTPalabrasReservadas("function");
        gestorTs.addEntradaTPalabrasReservadas("return");
        gestorTs.addEntradaTPalabrasReservadas("if");
        gestorTs.addEntradaTPalabrasReservadas("read");
        gestorTs.addEntradaTPalabrasReservadas("write");
    }

    public void crearTablaGlobal(){
        gestorTs.createAtributo("desplazamiento", DescripcionAtributo.DIR, TipoDatoAtributo.ENTERO);
        gestorTs.createAtributo("parámetro", TS_Gestor.DescripcionAtributo.PARAM, TS_Gestor.TipoDatoAtributo.ENTERO);
        gestorTs.createAtributo("número de parámetros", TS_Gestor.DescripcionAtributo.NUM_PARAM, TS_Gestor.TipoDatoAtributo.ENTERO);
        gestorTs.createAtributo("tipo de parámetros", TS_Gestor.DescripcionAtributo.TIPO_PARAM, TS_Gestor.TipoDatoAtributo.LISTA);
        gestorTs.createAtributo("tipo de retorno", TS_Gestor.DescripcionAtributo.TIPO_RET, TS_Gestor.TipoDatoAtributo.CADENA);
        gestorTs.createAtributo("etiqueta", TS_Gestor.DescripcionAtributo.ETIQUETA, TS_Gestor.TipoDatoAtributo.CADENA);
        gestorTs.createTSGlobal();
    }

}
