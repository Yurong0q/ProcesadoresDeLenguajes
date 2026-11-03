import Enumeraciones.*;
import Errores.Sint.Token_Inesperado;
import Errores.Sem.ErrSemantico;

import es.upm.aedlib.Pair;
import es.upm.aedlib.positionlist.*;
import tslib.TS_Gestor;

import java.util.List;


//NOTAS PARA RETURN
// - "tipo_ok" -> no hubo returns (ausencia de returns)
// - "vacio" -> hubo "return;" = sin expr (vacio representa literalmente la nada != void)
// - "entero"/"real"/... -> hubo return de expresion con ese tipo

public class Analizador_Sint_Sem {

    private Token token;
    private LexerConTexto aLex;
    private String parse;
    private TipoToken type;
    private int linea;
    private int columna;
    private int lineaAnterior;
    private int columnaAnterior;
    private int lineaSF;
    private List<String> codigoFuente;

    // Variable para la analizador semantico
    private TS_Gestor gestorTs;
    private int despGlob;
    private int despLoc;
    private int contadorEtiquetas;
    private boolean enFunción;
    private Token tokenSig;
    private int lineaSig;
    private int columnaSig;

    // Atributos para la tabla de simbolos -> deben coincidir con los asignados en
    // TablaDeSimbolos
    private static final String ATR_DIR = "desplazamiento";
    private static final String ATR_PARAM = "parámetro";
    private static final String ATR_NUM_PARAM = "número de parámetros";
    private static final String ATR_TIPO_PARAM = "tipo de parámetros";
    private static final String ATR_TIPO_RET = "tipo de retorno";
    private static final String ATR_ETIQUETA = "etiqueta";

    // METODOS DE AYUDA PARA ERRORES SEMANTICOS
    // -------------------------------------------------------------------------------

    // Marcas que usaremos al lanzar el error semantico
    private int errLin;
    private int errCol;

    /**
     * Metodo para marcar inicio de sentencia -> imprimir CONTEXTO
     */
    private void marcaInicioSentencia() {
        lineaSF = linea;
    }

    /**
     * Metodo para marcar token (se tiene que lanzar antes de consumirlo)
     * 
     * @param len longitud del token (puedes usar idlex.length)
     */
    private void marcaErrToken(int len) {
        errLin = linea;
        errCol = Math.max(1, columna - len); //voy a principio de token
    }

    /**
     * Metodo para marcar token pero con su lexema (hecho para ID)
     */
    private void marcaErrLexema() {
        String lx = token.getLex();
        marcaErrToken(lx == null || lx.isEmpty() ? 1 : lx.length());
    }

    /**
     * Metodo para marcar palabra clave fijas
     * (write/read/return/if/for/let/function)
     * 
     * @param pal palabra clave
     */
    private void marcaPal(String pal) {
        marcaErrToken(pal.length());
    }

    /**
     * Metodo para lanzar error sem. con las marcas guardadas anteriormente
     * 
     * @param contexto contexto ó titulo de error
     * @param desc     descripcion con mas detalles del error
     */
    private void errSem(String contexto, String desc) {
        throw new ErrSemantico(
                errLin, errCol, // (x,y) -> indicara la linea y columna del error
                errLin,
                lineaSF, // checkpoint para el CONTEXTO del error
                contexto,
                desc,
                codigoFuente);
    }

    // -------------------------------------------------------------------------------

    // METODOS Y FUNCIóNES AUXILIARES PARA A.SEMANTICO
    // -------------------------------------------------------------------------------
    /**
     * Función auxiliar para generar nuevas etiquetas.
     * 
     * @return nueva etiqueta (String)
     */
    private String nuevaEtiqueta() {
        return "E" + (contadorEtiquetas++);
    }

    /**
     * Función auxiliar para normalizar los tipos de entrada y salida de TS (entra lógico, sale lógico, imprime logico -> vaya  tsLib)
     * @param tipo tipo que imprime TS
     * @param entrada si es para setTipo o getTipo
     * @return String normalizado
     */
    private String normalizarTipo(String tipo, boolean entrada){
        String res = tipo;
        if(entrada && "logico".equals(res)) res = "lógico";
        if(!entrada && "lógico".equals(res)) res = "logico";
        return res;
    }

    /**
     * Función auxiliar para generar mensaje de cadena de parametros -> lo uso para
     * imprimir una lista de parametros
     * 
     * @param paramType lista de parametros
     * @return mensaje de cadena de parametros
     */
    private String paramCadena(String[] paramType) {
        String res = "";
        for (int i = 0; i < paramType.length; i++) {
            res += paramType[i];
            if (i + 1 != paramType.length)
                res += ", ";
        }
        return res;
    }


    /**
     * función auxiliar para comprobar variable o crearla implicitamente en TSGlobal
     * si no existe.
     * 
     * @param lex
     * @return posicion en TS (int) de la variable (puede ya existir o ser creada
     *         implicitamente)
     */
    private int aseguraVarImpl(String lex) {
        // si existe local, usarla
        int posL = gestorTs.getEntradaTSLocal(lex);
        if (posL != 0)
            return posL; // negativo

        // si existe global, usarla
        int posG = gestorTs.getEntradaTSGlobal(lex);
        if (posG != 0 && normalizarTipo(gestorTs.getTipo(posG),false) != null)
            return posG; // positivo

        // si no existe, crear global entera implícita
        if (posG == 0)
            posG = gestorTs.addEntradaTSGlobal(lex);
        if (normalizarTipo(gestorTs.getTipo(posG),false) == null)
            gestorTs.setTipo(posG, "entero");
        gestorTs.setValorAtributoEnt(posG, ATR_DIR, despGlob);
        gestorTs.setValorAtributoEnt(posG, ATR_PARAM, 0);
        despGlob += 1; // ancho entero
        return posG;
    }

    // -------------------------------------------------------------------------------

    /**
     * Constructor del analizador sintactico.
     * 
     * @param aLex         Analizador léxico inicializado y preparado para escribir.
     * @param codigoFuente Código fuente original (lista de líneas).
     * @param gestorTs     Gestor de la tabla de símbolos.
     */
    public Analizador_Sint_Sem(LexerConTexto aLex, List<String> codigoFuente, TS_Gestor gestorTs) {
        this.aLex = aLex;
        parse = "D ";
        this.codigoFuente = codigoFuente;
        this.gestorTs = gestorTs;
    }

    /**
     * Metodo para avanzar al siguiente token del analizador lexico.
     */
    private void nextToken() {
        // guardo el anterior (para errores)
        lineaAnterior = linea;
        columnaAnterior = columna;

        // el siguiente pasa a ser el actual
        token = tokenSig;
        linea = lineaSig;
        columna = columnaSig;

        // leo un nuevo "siguiente"
        Pair<Pair<Token,Integer>, Pair<Integer, Integer>> par = aLex.nextTokenWithPos();
        tokenSig = par.getLeft().getLeft();
        lineaSig = par.getRight().getLeft();
        columnaSig = par.getRight().getRight();

        type = token.getTipo();

    }

    /**
     * Metodo para iniciar el analisis sintactico.
     * 
     * @return cadena con la secuencia de producciones aplicadas.
     */
    public String iniciar() {

        // Inicio del analisis sintactico
        // Inicializamos TS Global
        despGlob = 0;
        enFunción = false;

        // Precarga de 2 tokens (necesario para tactica magica de looking ahead)
        Pair<Pair<Token,Integer>, Pair<Integer, Integer>> p1 = aLex.nextTokenWithPos();
        token = p1.getLeft().getLeft();
        linea = p1.getRight().getLeft();
        columna = p1.getRight().getRight();

        Pair<Pair<Token,Integer>, Pair<Integer, Integer>> p2 = aLex.nextTokenWithPos();
        tokenSig = p2.getLeft().getLeft();
        lineaSig = p2.getRight().getLeft();
        columnaSig = p2.getRight().getRight();

        type = token.getTipo();
        I();

        // Si no hemos consumido todo el codigo fuente, error
        if (token.getTipo() != TipoToken.TYPE_EOF) {//calculoCol(codigoFuente.get(linea != lineaSig?lineaSig-1:linea-1), columna, 8)
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF, "I",
                    token.getTipo(), "EOF", codigoFuente, token.getLex(), token.getAtributo(), null);
        }
        return parse;
    }

    /**
     * Metodo para comprobar si el terminal leido es correcto. Si lo es, avanza al
     * siguiente token. Si no lo es, lanza error.
     */
    private void equipara(Token t, String contexto) {
        // En ID,INT,FLOAT,STRING imponemos una restriccion mas suave (no miramos su
        // atributo)
        if (token.getTipo() == TipoToken.TYPE_IDENTIFICADOR && TipoToken.TYPE_IDENTIFICADOR == t.getTipo() ||
                token.getTipo() == TipoToken.TYPE_ENTERO && TipoToken.TYPE_ENTERO == t.getTipo() ||
                token.getTipo() == TipoToken.TYPE_REAL && TipoToken.TYPE_REAL == t.getTipo() ||
                token.getTipo() == TipoToken.TYPE_CADENA && TipoToken.TYPE_CADENA == t.getTipo() ||
                token.equals(t)) {

            // escribo token y avanzo
            aLex.imprimirToken(token);
            nextToken();
        } else {
            // Token_Inesperado gestiona internamente el mensaje a imprimir segun si debe
            // comprobar atributo o no
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    contexto, token.getTipo(), t.getTipo(), codigoFuente, token.getLex(),token.getAtributo(), t.getAtributo());
        }
    }

    // Gramatica aumentada para acciones semanticas
    private void I() {
        // 1. I -> P
        if (type == TipoToken.TYPE_IDENTIFICADOR || type == TipoToken.TYPE_WRITE ||
                type == TipoToken.TYPE_READ || type == TipoToken.TYPE_RETURN ||
                type == TipoToken.TYPE_LET || type == TipoToken.TYPE_IF || type == TipoToken.TYPE_FOR ||
                type == TipoToken.TYPE_FUNCTION || type == TipoToken.TYPE_EOF) {

            parse = parse.concat("1 ");
            P();
        }
    };

    // AXIOMA
    private void P() {
        // 2. P -> S P
        if (type == TipoToken.TYPE_IDENTIFICADOR || type == TipoToken.TYPE_WRITE ||
                type == TipoToken.TYPE_READ || type == TipoToken.TYPE_RETURN ||
                type == TipoToken.TYPE_LET || type == TipoToken.TYPE_IF || type == TipoToken.TYPE_FOR) {
            parse = parse.concat("2 ");
            S();
            P();
        } // 3. P -> F P
        else if (type == TipoToken.TYPE_FUNCTION) {
            parse = parse.concat("3 ");
            F();
            P();
        } // 4. P -> EOF
        else if (type == TipoToken.TYPE_EOF) {
            parse = parse.concat("4 ");
            equipara(new Token(TipoToken.TYPE_EOF, (Integer) null),"Archivo no terminado");
        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Se esperaba una sentencia, una declaración de función o el fin del archivo", token.getTipo(),
                    "'identificador', sentencia escribir 'write', sentencia leer 'read', sentencia de retorno de funcion 'return', declaracion de variable 'let', sentencia condicional 'if', sentencia de bucle 'for', declaracion de funcion 'function', EOF", 
                    codigoFuente, token.getLex(), token.getAtributo(),null);
    }

    /**
     * Función para el analisis de la produccion S.
     * 
     * @return Pair<String, String> donde el primer String es el tipo de la
     *         sentencia y el segundo String es el tipo de retorno (si lo hay).
     */
    private Pair<String, String> S() {

        // 24. S -> S_SIMPLE
        String tipo;
        String ret;
        Pair<String, String> par;
        if (type == TipoToken.TYPE_IDENTIFICADOR || type == TipoToken.TYPE_WRITE ||
                type == TipoToken.TYPE_READ || type == TipoToken.TYPE_RETURN) {
            parse = parse.concat("24 ");
            par = S_SIMPLE();
        }

        // 25. S -> S_COMPUESTA
        else if (type == TipoToken.TYPE_LET || type == TipoToken.TYPE_IF || type == TipoToken.TYPE_FOR) {
            parse = parse.concat("25 ");
            par = S_COMPUESTA();
        }

        else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Sentencia incorrecta", token.getTipo(), "'identificador', sentencia escribir 'write', sentencia leer 'read', sentencia de retorno de funcion 'return', declaracion de variable 'let', sentencia condicional 'if', sentencia de bucle 'for'",
                    codigoFuente, token.getLex(), token.getAtributo(),null);

        tipo = par.getLeft();
        ret = par.getRight();
        return new Pair<String, String>(tipo, ret);
    }

    /**
     * Metodo para el analisis de la produccion F.
     */
    private void F() {
        String H_tipo;
        String idLex;
        int idPos;
        marcaInicioSentencia(); // Checkpoint para CONTEXTO del error

        // 53. F -> function H id ( B ) { C }
        parse = parse.concat("53 ");
        marcaPal("function");
        equipara(new Token(TipoToken.TYPE_FUNCTION, (Integer) null),"Declaracion de funcion incorrecta");
        H_tipo = H(); // tipo de retorno ("entero", "real", "logico", "cadena", "vacio")
        idLex = token.getLex();
        idPos = gestorTs.getEntradaTSGlobal(idLex);
        // Miro que no esté ya declarada la variable
        if (normalizarTipo(gestorTs.getTipo(idPos),false) != null) {
            errSem("Error de redeclaración de variable",
                    "La variable " + idLex + " ya esta declarado como " + normalizarTipo(gestorTs.getTipo(idPos),false)
                            + " y no admite redeclaracion a tipo función");
        }
        // ids a la TSGlobal bien (comprobacion innecesaria ahora, pero para
        // modificaciones futuras puede ser util)
        if (idPos == 0)
            idPos = gestorTs.addEntradaTSGlobal(idLex);

        // Registrar función en TS y guardar atributos
        gestorTs.setTipo(idPos, "función");
        gestorTs.setValorAtributoCad(idPos, ATR_TIPO_RET, H_tipo);
        gestorTs.setValorAtributoCad(idPos, ATR_ETIQUETA, nuevaEtiqueta());

        token.setAtributo(idPos);
        token.setPos(idPos);
        equipara(new Token(TipoToken.TYPE_IDENTIFICADOR, (Integer) null), "Declaracion de funcion incorrecta");

        // Crear TS local para la función y desplazamiento local a 0
        gestorTs.createTSLocal();
        enFunción = true;
        despLoc = 0;

        // --Parametros--
        equipara(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 0), "Declaracion de funcion incorrecta");
        NodePositionList<String> B_tipo = B();
        equipara(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 1), "Declaracion de funcion incorrecta");

        // Registrar tipos de parametros en TS
        String[] tiposParam;
        int numParam;
        // Si solo tiene el elemento "vacio" significará que no recibe parametros
        if (B_tipo.size() == 1 && B_tipo.first().element().equals("vacio")) {
            tiposParam = new String[0];
            numParam = 0;
        } else {
            tiposParam = B_tipo.toArray(new String[0]);
            numParam = tiposParam.length;
        }
        gestorTs.setValorAtributoEnt(idPos, ATR_NUM_PARAM, numParam);
        gestorTs.setValorAtributoLista(idPos, ATR_TIPO_PARAM, tiposParam);

        // --Cuerpo--
        equipara(new Token(TipoToken.TYPE_LLAVE, (Integer) 0), "Declaracion de funcion incorrecta");
        Pair<String, String> C_par = C();
        equipara(new Token(TipoToken.TYPE_LLAVE, (Integer) 1), "Declaracion de funcion incorrecta");
        // imprimo tabla local
        int res = gestorTs.write(TS_Gestor.Tabla.LOCAL);
        if (res != 0) {
            System.out.println("Error al intentar escribir la tabla local en el fichero");
        }
        // destruir TS local + volver global + estamos fuera de función
        gestorTs.destroy(TS_Gestor.Tabla.LOCAL);
        enFunción = false;

        String C_ret = C_par.getRight();

        // Miro si ha retornado algo
        if (!"tipo_ok".equals(C_ret)) {
            // SI returnea, miro si mi función era void
            if ("vacio".equals(H_tipo)) {
                // función void: solo puede retornar vacio (o nada)
                if (!"vacio".equals(C_ret)) {
                    errSem("Return en metodo (función void)",
                            "La función es tipo void y se ha returnado tipo " + C_ret);
                }
            } else {
                // función no void: return expr debe coincidir con H_tipo
                if (!H_tipo.equals(C_ret)) {
                    errSem("Return de un tipo incorrecto en una funcion",
                            "La función " + idLex + " devuelve un tipo " + H_tipo + " y se ha retornado un tipo " + C_ret);
                }
            }
        }
    }

    /**
     * Función para el analisis de la produccion E.
     * 
     * @return E.tipo
     */
    private String E() {

        String E_tipo;
        String E1_tipo;
        String E_AND_tipo;

        // 5. E -> E1 E_AND
        if (type == TipoToken.TYPE_NEGACION || type == TipoToken.TYPE_IDENTIFICADOR ||
                type == TipoToken.TYPE_PARENTESIS || type == TipoToken.TYPE_CADENA ||
                type == TipoToken.TYPE_ENTERO || type == TipoToken.TYPE_REAL) {
            parse = parse.concat("5 ");
            E1_tipo = E1();
            E_AND_tipo = E_AND();

            // Accion Semantica
            if (E_AND_tipo.equals("tipo_ok")) {
                E_tipo = E1_tipo;
            } else if (E1_tipo.equals("logico") && E1_tipo.equals(E_AND_tipo)) {
                E_tipo = "logico";    
            } else E_tipo = "tipo_error";
                
            if(E_tipo.equals("tipo_error")){    
                errSem("Error en expresion logica '&&' ",
                "Solo se puede comparar 2 lógicos y se ha introducido " + E1_tipo + ", " + E_AND_tipo);
            }

            
        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Se esperaba el inicio de una expresión", token.getTipo(),
                    "operador de negacion '!', 'identificador', parentesis abierto '(', cadena, constante entera, constante real", codigoFuente, token.getLex(), token.getAtributo(), null);

        return E_tipo;
    }

    /**
     * Función para el analisis de la produccion E_AND.
     * 
     * @return E_AND.tipo
     */
    private String E_AND() {

        String E_AND_tipo;
        String E1_tipo;
        String E_AND1_tipo;

        // 6. E_AND -> && E1 E_AND
        if (type == TipoToken.TYPE_AND) {
            parse = parse.concat("6 ");
            marcaErrToken(2); // &&
            equipara(new Token(TipoToken.TYPE_AND, (Integer) null), "Comparando");
            E1_tipo = E1();
            E_AND1_tipo = E_AND();

            // Accion Semantica
            if(E_AND1_tipo.equals("tipo_ok")){
                E_AND_tipo = "logico";
            } else if (E_AND1_tipo.equals("logico") && E_AND1_tipo.equals(E1_tipo)){
                E_AND_tipo = "logico";
            } else E_AND_tipo = "tipo_error";

            if(E_AND_tipo.equals("tipo_error"))
                errSem("Error en expresion logica '&&' ",
                "Solo se puede comparar 2 lógicos y se ha introducido " + E1_tipo + ", " + E_AND1_tipo);


        }
        // 7. E_AND -> lambda | FOLLOW(E_AND) = { ")", ",", ";" }
        else if (type == TipoToken.TYPE_PUNTO_Y_COMA || type == TipoToken.TYPE_PARENTESIS ||
                type == TipoToken.TYPE_COMA) {
            parse = parse.concat("7 ");

            // Accion Semantica
            E_AND_tipo = "tipo_ok";

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Expresion no terminada", token.getTipo(), 
                    "operador logico '&&', ';', parentesis cerrado ')', ','",
                    codigoFuente, token.getLex(), token.getAtributo(), null);

        return E_AND_tipo;
    }

    /**
     * Función para el analisis de la produccion E1.
     * 
     * @return E1.tipo
     */
    private String E1() {
        String E1_tipo;
        String E2_tipo;
        String E1_REL_tipo;

        // 8. E1 -> E2 E1_REL
        if (type == TipoToken.TYPE_NEGACION || type == TipoToken.TYPE_IDENTIFICADOR ||
                type == TipoToken.TYPE_PARENTESIS || type == TipoToken.TYPE_CADENA ||
                type == TipoToken.TYPE_ENTERO || type == TipoToken.TYPE_REAL) {
            parse = parse.concat("8 ");
            E2_tipo = E2();
            E1_REL_tipo = E1_REL();

            // Accion Semantico
            if (E1_REL_tipo.equals("tipo_ok"))
                E1_tipo = E2_tipo;
            else if ((E2_tipo.equals("real") || E2_tipo.equals("entero")) &&  E2_tipo.equals(E1_REL_tipo))
                E1_tipo = "logico";
            else E1_tipo = "tipo_error";

            if(E1_tipo.equals("tipo_error")){
                errSem("Expresion relacional incorrecta", 
                "Solo se pueden operar 2 reales ó 2 enteros y se introdujo " + E2_tipo + ", " + E1_REL_tipo);
            }
            
        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Expresión incompleta después de operador logico", token.getTipo(),
                    "operador de negacion '!', 'identificador', parentesis abierto '(', cadena, constante entera, constante real",
                    codigoFuente, token.getLex(), token.getAtributo(), null);

        return E1_tipo;
    }

    /**
     * Función para el analisis de la produccion E1_REL.
     * 
     * @return E1_REL.tipo
     */
    private String E1_REL() {
        String E1_REL_tipo;
        String E2_tipo;
        String E1_REL1_tipo;

        // 9. E1_REL -> > E2 E1_REL
        if (token.equals(new Token(TipoToken.TYPE_RELACION, (Integer) 0))) {
            parse = parse.concat("9 ");
            marcaErrToken(1);// >
            equipara(new Token(TipoToken.TYPE_RELACION, (Integer) 0), "Expresion incorrecta");
            E2_tipo = E2();
            E1_REL1_tipo = E1_REL();

            if (E1_REL1_tipo.equals("tipo_ok")){
                E1_REL_tipo = E2_tipo;
            }
            else if((E2_tipo.equals("entero") || E2_tipo.equals("real")) && (E2_tipo.equals(E1_REL1_tipo))){
                E1_REL_tipo = "logico";
            } else E1_REL_tipo = "tipo_error";

            if(E1_REL_tipo.equals("tipo_error")){
                errSem("Expresion relacional incorrecta", 
                "Solo se pueden operar 2 reales ó 2 enteros y se introdujo "+ E2_tipo + ", " + E1_REL1_tipo);
            }

        }
        // 10. E1_REL -> < E2 E1_REL
        else if (token.equals(new Token(TipoToken.TYPE_RELACION, (Integer) 1))) {
            parse = parse.concat("10 ");
            marcaErrToken(1);// <
            equipara(new Token(TipoToken.TYPE_RELACION, (Integer) 1), "Expresion incorrecta");
            E2_tipo = E2();
            E1_REL1_tipo = E1_REL();

            // Accion Semantica
            if (E1_REL1_tipo.equals("tipo_ok")){
                E1_REL_tipo = E2_tipo;
            }
            else if((E2_tipo.equals("entero") || E2_tipo.equals("real")) && (E2_tipo.equals(E1_REL1_tipo))){
                E1_REL_tipo = "logico";
            } else E1_REL_tipo = "tipo_error";

            if(E1_REL_tipo.equals("tipo_error")){
                errSem("Expresion relacional incorrecta", 
                "Solo se pueden operar 2 reales ó 2 enteros y se introdujo "+ E2_tipo + ", " + E1_REL1_tipo);
            }

        }
        // 11. E1_REL -> lambda | FOLLOW(E1_REL) = { "&&", ")", ",", ";" }
        else if (type == TipoToken.TYPE_AND || type == TipoToken.TYPE_PARENTESIS ||
                type == TipoToken.TYPE_COMA || type == TipoToken.TYPE_PUNTO_Y_COMA) {
            parse = parse.concat("11 ");

            // Accion Semantica
            E1_REL_tipo = "tipo_ok";
        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Expresion no terminada", token.getTipo(), ">, <, operador logico '&&', ) , ',', ';'",
                    codigoFuente, token.getLex(), token.getAtributo(), null);

        return E1_REL_tipo;
    }

    /**
     * Función para el analisis de la produccion E2.
     * 
     * @return E2.tipo
     */
    private String E2() {
        String E2_tipo;
        String E3_tipo;
        String E2_ARIT_tipo;

        // 12. E2 -> E3 E2_ARIT
        if (type == TipoToken.TYPE_NEGACION || type == TipoToken.TYPE_IDENTIFICADOR ||
                type == TipoToken.TYPE_PARENTESIS || type == TipoToken.TYPE_CADENA ||
                type == TipoToken.TYPE_ENTERO || type == TipoToken.TYPE_REAL) {
            parse = parse.concat("12 ");
            E3_tipo = E3();
            E2_ARIT_tipo = E2_ARIT();

            // Accion Semantica
            if (E2_ARIT_tipo.equals("tipo_ok")){
                E2_tipo = E3_tipo;
            }
            else if((E3_tipo.equals("entero") || E3_tipo.equals("real")) && (E3_tipo.equals(E2_ARIT_tipo))){
                E2_tipo = E3_tipo;
            } else E2_tipo = "tipo_error";

            if(E2_tipo.equals("tipo_error")){
                errSem("Expresion aritmetica incorrecta", 
                "Solo se pueden operar 2 reales ó 2 enteros y se introdujo "+ E3_tipo + " , " + E2_ARIT_tipo);
            }

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Expresión aritmética incompleta", token.getTipo(),
                    "operador de negacion '!', 'identificador', parentesis abierto '(', cadena, constante entera, constante real", codigoFuente, token.getLex(), token.getAtributo(), null);

        return E2_tipo;
    }

    /**
     * Función para el analisis de la produccion E2_ARIT.
     * 
     * @return E2_ARIT.tipo
     */
    private String E2_ARIT() {
        String E2_ARIT_tipo;
        String E3_tipo;
        String E2_ARIT1_tipo;

        // 13. E2_ARIT -> + E3 E2_ARIT
        if (token.equals(new Token(TipoToken.TYPE_ARITMETICO, (Integer) 0))) {
            parse = parse.concat("13 ");
            marcaErrToken(1); // +
            equipara(new Token(TipoToken.TYPE_ARITMETICO, (Integer) 0), "Expresion incorrecta");
            E3_tipo = E3();
            E2_ARIT1_tipo = E2_ARIT();

            // Accion Semantica
            if(E2_ARIT1_tipo.equals("tipo_ok")){
                E2_ARIT_tipo = E3_tipo;      
            }
            else if((E3_tipo.equals("entero") || E3_tipo.equals("real")) && (E3_tipo.equals(E2_ARIT1_tipo))){
                E2_ARIT_tipo = E3_tipo;
            } else E2_ARIT_tipo = "tipo_error";

            if(E2_ARIT_tipo.equals("tipo_error")){
                errSem("Expresion aritmetica incorrecta", 
                "Solo se pueden operar 2 reales ó 2 enteros y se introdujo "+ E3_tipo + " , " + E2_ARIT1_tipo);
            }

        }
        // 14. E2_ARIT -> - E3 E2_ARIT
        else if (token.equals(new Token(TipoToken.TYPE_ARITMETICO, (Integer) 1))) {
            parse = parse.concat("14 ");
            marcaErrToken(1); // -
            equipara(new Token(TipoToken.TYPE_ARITMETICO, (Integer) 1), "Expresion incorrecta");
            E3_tipo = E3();
            E2_ARIT1_tipo = E2_ARIT();

            // Accion Semantica
            if(E2_ARIT1_tipo.equals("tipo_ok")){
                E2_ARIT_tipo = E3_tipo;      
            }
            else if((E3_tipo.equals("entero") || E3_tipo.equals("real"))&&(E3_tipo.equals(E2_ARIT1_tipo))){
                E2_ARIT_tipo = E3_tipo;
            } else E2_ARIT_tipo = "tipo_error";

            if(E2_ARIT_tipo.equals("tipo_error")){
                errSem("Expresion aritmetica incorrecta", 
                "Solo se pueden operar 2 reales ó 2 enteros y se introdujo "+ E3_tipo + " + " + E2_ARIT1_tipo);
            }

        }
        // 15. E2_ARIT -> lambda | FOLLOW(E2_ARIT) = { ">", "<", "&&", ")", ",", ";" }
        else if (type == TipoToken.TYPE_AND || type == TipoToken.TYPE_PARENTESIS ||
                type == TipoToken.TYPE_COMA || type == TipoToken.TYPE_PUNTO_Y_COMA ||
                type == TipoToken.TYPE_RELACION) {
            parse = parse.concat("15 ");

            // Accion Semantica
            E2_ARIT_tipo = "tipo_ok";
        } else{
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Expresion no terminada", token.getTipo(),
                    "operador aritmetico '+' o '-', operador logico '&&', parentesis cerrado ')', ',', ';', operador relacional '<' o '>'", codigoFuente, token.getLex(), token.getAtributo(), null);
            }
        return E2_ARIT_tipo;
    }

    /**
     * Función para el analisis de la produccion E3.
     * 
     * @return E3.tipo
     */
    private String E3() {
        String E3_tipo = null;
        String E31_tipo;
        String E_tipo;
        NodePositionList<String> E4_tipo;
        String idLex;
        int idPos;
        String idTipo;

        // 16. E3 -> ! E3
        if (type == TipoToken.TYPE_NEGACION) {
            parse = parse.concat("16 ");
            marcaErrToken(1); // !
            equipara(new Token(TipoToken.TYPE_NEGACION, (Integer) null), "Expresion incorrecta");
            E31_tipo = E3();

            // Accion Semantica
            if (E31_tipo.equals("logico")) E3_tipo = "logico";
            else E3_tipo = "tipo_error";
            
            if(E3_tipo.equals("tipo_error")) 
                errSem("Negación incorrecta",
                    "Solo se puede negar una expresión lógica (se esta intentando negar una expresión de tipo " + E31_tipo+")");

        }

        // 17. E3 -> id E4
        else if (type == TipoToken.TYPE_IDENTIFICADOR) {
            parse = parse.concat("17 ");

            idLex = token.getLex(); //Se guarda el lexema para imprimirla en el error
            
            marcaErrLexema();

            equipara(new Token(TipoToken.TYPE_IDENTIFICADOR, (Integer) null), "Expresion incorrecta");
            E4_tipo = E4();

            idPos = aseguraVarImpl(idLex);           
            idTipo = normalizarTipo(gestorTs.getTipo(idPos),false);
           
            if(idTipo.equals("función")){

                String[] E4_tipoArr = E4_tipo.toArray(new String[0]);
                String[] paramTypes = gestorTs.getValorAtributoLista(idPos, ATR_TIPO_PARAM);

                // Compruebo lista de parametros
                boolean ok = paramTypes.length == E4_tipoArr.length;
                if (!ok) errSem("Número de parametros incorrectos",
                                "La función " + idLex + " pide " + paramTypes.length + " y se han introducido " + E4_tipoArr.length);

                int i;
                for (i = 0; i < E4_tipoArr.length && ok; i++)
                    if (!E4_tipoArr[i].equals(paramTypes[i])) ok = false;

                if (!ok)
                    errSem("Parametros de función "+ idLex +" incorrectos",
                            "Parametros introducidos: " + paramCadena(E4_tipoArr) + "\nParametros pedidos: "
                            + paramCadena(paramTypes) + "\nPrimera no coincidencia: " + i);
                    
                E3_tipo = gestorTs.getValorAtributoCad(idPos, ATR_TIPO_RET);

            } else if (E4_tipo.size() == 0 || !E4_tipo.first().element().equals("vacio")){
                errSem("Llamada a una funcion implicitamente o no declarada",
                "La función " + idLex  + " no es una funcion declarada previamente y no se puede llamar implicitamente");
            } else E3_tipo = idTipo;
        }

        // 18. E3 -> ( E )
        else if (token.equals(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 0))) {
            parse = parse.concat("18 ");
            equipara(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 0), "Expresion incorrecta");
            E_tipo = E();

            // Accion Semantica
            E3_tipo = E_tipo;

            equipara(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 1), "Expresion incorrecta");
        }
        // 19. E3 -> cadena
        else if (type == TipoToken.TYPE_CADENA) {
            parse = parse.concat("19 ");
            equipara(new Token(TipoToken.TYPE_CADENA, (Integer) null), "Expresion incorrecta");

            // Accion Semantica
            E3_tipo = "cadena";

        }
        // 20. E3 -> entero
        else if (type == TipoToken.TYPE_ENTERO) {
            parse = parse.concat("20 ");
            equipara(new Token(TipoToken.TYPE_ENTERO, (Integer) null), "Expresion incorrecta");

            // Accion Semantica
            E3_tipo = "entero";

        }
        // 21. E3 -> real
        else if (type == TipoToken.TYPE_REAL) {
            parse = parse.concat("21 ");
            equipara(new Token(TipoToken.TYPE_REAL, (Integer) null), "Expresion incorrecta");

            // Accion Semantica
            E3_tipo = "real";

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Expresión no válida o incompleta.", token.getTipo(),
                    "operador de negacion '!', 'identificador', parentesis abierto '(', cadena, constante entera, constante real", 
                    codigoFuente, token.getLex(), token.getAtributo(), null);

        return E3_tipo;
    }

    /**
     * Función para el analisis de la produccion E4.
     * 
     * @return Lista de tipos
     */
    private NodePositionList<String> E4() {

        NodePositionList<String> E4_tipo;
        NodePositionList<String> L_tipo;

        // 22. E4 -> ( L )
        if (token.equals(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 0))) {
            parse = parse.concat("22 ");
            marcaErrToken(1); // (
            equipara(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 0), "Expresion incorrecta");
            L_tipo = L();

            // Accion Semantica
            E4_tipo = new NodePositionList<>(L_tipo);

            equipara(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 1), "Expresion incorrecta");
        }
        // 23. E4 -> lambda | FOLLOW(E4) = { "+", "-", ">", "<", "&&", ")", ",", ";" }
        else if (type == TipoToken.TYPE_AND || token.equals(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 1)) ||
                type == TipoToken.TYPE_COMA || type == TipoToken.TYPE_PUNTO_Y_COMA ||
                type == TipoToken.TYPE_RELACION || type == TipoToken.TYPE_ARITMETICO) {
            parse = parse.concat("23 ");

            // Accion Semantica
            E4_tipo = new NodePositionList<>();
            E4_tipo.addLast("vacio"); // Para simbolizar la no llamada a función, pongo un solo elemento "vacio"

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Después del identificador se esperaba los parametros de una funcion '(...)' o fin de la expresion", token.getTipo(),
                    "parentesis abierto '(', parentesis cerrado ')', operador logico '&&', ',', ';', operador relacional '<' o '>', operador aritmetico '+' o '-'", codigoFuente, token.getLex(), token.getAtributo(), null);

        return E4_tipo;
    }

    /**
     * Función para el analisis de la producion S_SIMPLE.
     * 
     * @return S_SIMPLE.tipo y S_SIMPLE.ret (ambos Strings)
    */
   private Pair<String, String> S_SIMPLE() {
       String S_SIMPLE_tipo;
       NodePositionList<String> ASIGFUN_tipo;
       String S_SIMPLE_ret;
       String idTipo;
       int idPos = 0;
       String E_tipo;
       String X_tipo;
       
       marcaInicioSentencia(); // Checkpoint para CONTEXTO del error
       
       // 26. S_SIMPLE -> id ASIGFUN
        if (type == TipoToken.TYPE_IDENTIFICADOR) {
            parse = parse.concat("26 ");

            marcaErrLexema(); // id
            String idLex = token.getLex();
            
            equipara(new Token(TipoToken.TYPE_IDENTIFICADOR, (Integer) null), "Llamada a funcion o asignacion incorrecta");
            ASIGFUN_tipo = ASIGFUN();
            
            idPos = aseguraVarImpl(idLex);
            idTipo = normalizarTipo(gestorTs.getTipo(idPos),false);
            
            if(idTipo.equals("función")){
                // Compruebo lista de parametros
                String[] paramTypes = gestorTs.getValorAtributoLista(idPos, ATR_TIPO_PARAM);
                ASIGFUN_tipo.remove(ASIGFUN_tipo.first());
                String[] args = ASIGFUN_tipo.toArray(new String[0]);
                boolean ok = paramTypes.length == args.length;
                if (!ok){
                    errSem("Número de parametros incorrectos",
                    "La función pide " + paramTypes.length + " y se han introducido " + (args.length -1));
                }
                int i;
                for (i = 0; i < args.length && ok; i++)
                    if (!args[i].equals(paramTypes[i])) ok = false;
                
                if (!ok){
                    errSem("Parametros de función "+ idLex + " incorrectos",
                    "Parametros introducidos: " + paramCadena(args) + "\nParametros pedidos: "
                    + paramCadena(paramTypes) + "\nPrimera no coincidencia es el param. num. " + i);
                }
                S_SIMPLE_tipo = "tipo_ok";
            
            } else if (ASIGFUN_tipo.size() == 1 || ASIGFUN_tipo.first().element().equals("true")){
                errSem("Llamada a una funcion implicitamente o no declarada",
                "La función " + idLex + " no es una funcion declarada previamente y no se puede llamar implicitamente");

            } else if (!ASIGFUN_tipo.next(ASIGFUN_tipo.first()).element().equals(idTipo)){
                    errSem("Asignacion incorrecta",
                    "El tipo de " + idLex + " (" + idTipo + ") es incompatible con el tipo de asignacion " + "(" + ASIGFUN_tipo.next(ASIGFUN_tipo.first()).element() + ")");
            }
            
            S_SIMPLE_tipo = "tipo_ok";
            S_SIMPLE_ret = "tipo_ok";
        }
        // 27. S_SIMPLE -> write E ;
        else if (type == TipoToken.TYPE_WRITE) {
            parse = parse.concat("27 ");
            marcaPal("write");
            equipara(new Token(TipoToken.TYPE_WRITE, (Integer) null), "Comparando");
            E_tipo = E();

            equipara(new Token(TipoToken.TYPE_PUNTO_Y_COMA, (Integer) null), "Sentencia no terminada");

            // Accion Semantica
            if (!("real".equals(E_tipo) ||
                    "entero".equals(E_tipo) ||
                    "cadena".equals(E_tipo)))
                errSem("Error en write",
                        "Write solo acepta real, entero o cadena (has introducido tipo " + E_tipo + ")");

            S_SIMPLE_tipo = "tipo_ok";
            S_SIMPLE_ret = "tipo_ok";

        } // 28. S_SIMPLE -> read id ;
        else if (type == TipoToken.TYPE_READ) {
            parse = parse.concat("28 ");
            equipara(new Token(TipoToken.TYPE_READ, (Integer) null), "Comparando");
            String idLex = token.getLex(); // capturo datos de id

            marcaErrLexema();
            // la variable puede ser implicita
            idPos = aseguraVarImpl(idLex);
            idTipo = normalizarTipo(gestorTs.getTipo(idPos),false);

            if (!("real".equals(idTipo) ||
                    "entero".equals(idTipo) ||
                    "cadena".equals(idTipo)))
                errSem("Error en read", "Read solo acepta real, entero o cadena (has introducido tipo " + idTipo + ")");

            token.setAtributo(idPos);
            token.setPos(idPos);
            equipara(new Token(TipoToken.TYPE_IDENTIFICADOR, (Integer) null), "Sentencia de lectura incorrecta");
            equipara(new Token(TipoToken.TYPE_PUNTO_Y_COMA, (Integer) null), "Sentencia no terminada");

            S_SIMPLE_tipo = "tipo_ok";
            S_SIMPLE_ret = "tipo_ok";
        } // 29. S_SIMPLE -> return X ;
        else if (type == TipoToken.TYPE_RETURN) {
            parse = parse.concat("29 ");
            marcaPal("return");
            equipara(new Token(TipoToken.TYPE_RETURN, (Integer) null), "Comparando");
            X_tipo = X();
            equipara(new Token(TipoToken.TYPE_PUNTO_Y_COMA, (Integer) null), "Sentencia no terminada");

            // Accion Semantica
            S_SIMPLE_tipo = "tipo_ok";
            S_SIMPLE_ret = X_tipo;

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Sentencia simple incorrecta", token.getTipo(), "'identificador', sentencia escribir 'write', sentencia leer 'read', sentencia de retorno de funcion 'return'", codigoFuente,
                    token.getLex(), token.getAtributo(), null);

        return new Pair<String, String>(S_SIMPLE_tipo, S_SIMPLE_ret);
    }

    /**
     * Función para el analisis de la produccion S_COMPUESTA.
     * 
     * @return S_COMPUESTA.tipo y S_COMPUESTA.ret
     */
    private Pair<String, String> S_COMPUESTA() {
        String S_COMPUESTA_tipo;
        String S_COMPUESTA_ret;
        String T_tipo;
        String E_tipo;
        String S_SIMPLE_tipo;
        String S_SIMPLE_ret;
        String C_tipo;
        String C_ret;

        marcaInicioSentencia(); // Checkpoint para CONTEXTO del error

        // 40. S_COMPUESTA -> let T id ;
        if (type == TipoToken.TYPE_LET) {
            parse = parse.concat("40 ");
            equipara(new Token(TipoToken.TYPE_LET, (Integer) null), "Comparando");

            Pair<String, Integer> tPar = T(); // Obtenemos tipo y anchura de tipo

            T_tipo = tPar.getLeft();
            int T_anchura = tPar.getRight();
            String idLex = token.getLex();
            int idPos;
            marcaErrLexema();

            // meto variable en TS
            if (enFunción) {
                // Compruebo si ya existe en local
                int posLocal = gestorTs.getEntradaTSLocal(idLex);
                if (posLocal != 0) {
                    errSem("Error de redeclaración de variable",
                            "La variable " + idLex + " ya esta declarado como " + normalizarTipo(gestorTs.getTipo(posLocal),false)
                                    + " y no admite redeclaracion a tipo "+T_tipo);
                }

                posLocal = gestorTs.addEntradaTSLocal(idLex); // NEGATIVO
                gestorTs.setTipo(posLocal, normalizarTipo(T_tipo,true));
                gestorTs.setValorAtributoEnt(posLocal, ATR_DIR, despLoc);
                gestorTs.setValorAtributoEnt(posLocal, ATR_PARAM, 0);
                despLoc += T_anchura;
                idPos = posLocal;
            } else {
                // Compruebo si ya existe en global
                int posG = gestorTs.getEntradaTSGlobal(idLex);
                if (posG != 0 && normalizarTipo(gestorTs.getTipo(posG),false) != null) {
                    errSem("Error de redeclaración de variable",
                            "La variable " + idLex + " ya esta declarado como " + normalizarTipo(gestorTs.getTipo(posG),false)
                                    + " y no admite redeclaracion a tipo "+T_tipo);
                }

                // id a la TSGlobal bien (posible comprobacion innecesaria)
                if (posG == 0) posG = gestorTs.addEntradaTSGlobal(idLex);

                gestorTs.setTipo(posG, normalizarTipo(T_tipo,true));
                gestorTs.setValorAtributoEnt(posG, ATR_DIR, despGlob);
                gestorTs.setValorAtributoEnt(posG, ATR_PARAM, 0);
                despGlob += T_anchura;
                idPos = posG;
            }
            token.setAtributo(idPos);
            token.setPos(idPos);
            equipara(new Token(TipoToken.TYPE_IDENTIFICADOR, (Integer) null), "Declaracion incorrecta");
            equipara(new Token(TipoToken.TYPE_PUNTO_Y_COMA, (Integer) null), "Sentencia no terminada");

            S_COMPUESTA_tipo = "tipo_ok";
            S_COMPUESTA_ret = "tipo_ok";
        }
        // 41. S_COMPUESTA -> if ( E ) S_SIMPLE
        else if (type == TipoToken.TYPE_IF) {
            parse = parse.concat("41 ");
            marcaPal("if");
            equipara(new Token(TipoToken.TYPE_IF, (Integer) null), "Comparando");
            equipara(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 0), "Sentencia condicional incorrecta");
            E_tipo = E();
            equipara(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 1), "Sentencia condicional incorrecta");
            Pair<String, String> par = S_SIMPLE();
            S_SIMPLE_tipo = par.getLeft();
            S_SIMPLE_ret = par.getRight();

            // Accion Semantica
            if (!E_tipo.equals("logico"))
                errSem("Ausencia lógica en sentencia condicional 'if'",
                        "El tipo " + E_tipo + " no sirve como condicion para el if (solo acepta logico)");

            S_COMPUESTA_tipo = S_SIMPLE_tipo;
            S_COMPUESTA_ret = S_SIMPLE_ret;

        }
        // 42. S_COMPUESTA -> for ( INI ; E ; ACTU ) { C }
        else if (type == TipoToken.TYPE_FOR) {
            parse = parse.concat("42 ");
            marcaPal("for");
            equipara(new Token(TipoToken.TYPE_FOR, (Integer) null), "Comparando");
            equipara(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 0), "Sentencia de bucle 'for' incorrecta");
            INI();
            equipara(new Token(TipoToken.TYPE_PUNTO_Y_COMA, (Integer) null), "Sentencia de bucle 'for' incorrecta");
            marcaErrToken(1);
            E_tipo = E();
            if(!E_tipo.equals("logico"))
                errSem("Condición del sentencia de bucle 'for' incorrecto", 
                        "La expresión como condicion en el sentencia de bucle 'for' tiene que ser logico (se introdujo tipo "+E_tipo+")");
            equipara(new Token(TipoToken.TYPE_PUNTO_Y_COMA, (Integer) null), "Sentencia de bucle 'for' incorrecta");
            ACTU();
            equipara(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 1), "Sentencia de bucle 'for' incorrecta");
            equipara(new Token(TipoToken.TYPE_LLAVE, (Integer) 0), "Sentencia de bucle 'for' incorrecta");
            Pair<String, String> par = C();
            C_tipo = par.getLeft();
            C_ret = par.getRight();
            equipara(new Token(TipoToken.TYPE_LLAVE, (Integer) 1), "Sentencia de bucle 'for' incorrecta");

            // Accion Semantica
            S_COMPUESTA_tipo = C_tipo;

            S_COMPUESTA_ret = C_ret;

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Sentencia compuesta incorrecta", token.getTipo(), "declaracion de variable 'let', sentencia condicional 'if', sentencia de bucle 'for'",
                    codigoFuente, token.getLex(), token.getAtributo(), null);

        return new Pair<String, String>(S_COMPUESTA_tipo, S_COMPUESTA_ret);
    }

    /**
     * Función para el analisis de la produccion ASIGFUN.
     * 
     * @return Lista de tipos (si es asignacion -> un solo tipo)
     */
    private NodePositionList<String> ASIGFUN() {
        NodePositionList<String> ASIGFUN_tipo;
        NodePositionList<String> L_tipo;
        String A_tipo;
        String E_tipo;

        // 30. ASIGFUN -> A E ;
        if (token.getTipo() == TipoToken.TYPE_ASIGNACION) {
            parse = parse.concat("30 ");
            A_tipo = A();
            E_tipo = E();
            equipara(new Token(TipoToken.TYPE_PUNTO_Y_COMA, (Integer) null), "Sentencia no terminada");

            // Accion Semantica
            ASIGFUN_tipo = new NodePositionList<>();
            ASIGFUN_tipo.addFirst("false");
            if (A_tipo.equals("logico") && E_tipo.equals("logico") ||
                    A_tipo.equals("tipo_ok")) {
                ASIGFUN_tipo.addLast(E_tipo);
            } else
                errSem("Asignacion lógica incorrecta",
                        "La asignación lógica (|=) solo acepta expresiones lógicas (se introdujo tipo " + E_tipo + ")");
            ;

        }
        // 31. ASIGFUN -> ( L ) ;
        else if (token.equals(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 0))) {
            parse = parse.concat("31 ");
            equipara(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 0), "Llamada a funcion incorrecta");
            L_tipo = L();
            equipara(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 1), "Llamada a funcion incorrecta");
            equipara(new Token(TipoToken.TYPE_PUNTO_Y_COMA, (Integer) null), "Sentencia no terminada");

            // Accion Semantica
            ASIGFUN_tipo = new NodePositionList<>(L_tipo);
            ASIGFUN_tipo.addFirst("true");


        } else{
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Asignacion o llamada a función '(...)' incorrecta", token.getTipo(),
                    "simbolo de asignacion '=' o '|=', parentesis abierto '('", codigoFuente, token.getLex(), token.getAtributo(), null);
        }
        return ASIGFUN_tipo;
    }

    /**
     * Función para el analisis de la produccion A.
     * 
     * @return A.tipo
     */
    private String A() {
        String A_tipo;

        // 32. A -> =
        if (token.equals(new Token(TipoToken.TYPE_ASIGNACION, (Integer) 0))) {
            parse = parse.concat("32 ");
            marcaErrToken(1);
            equipara(new Token(TipoToken.TYPE_ASIGNACION, (Integer) 0), "Comparando");

            // Accion Semantica
            A_tipo = "tipo_ok";

        } // 33. A -> |=
        else if (token.equals(new Token(TipoToken.TYPE_ASIGNACION, (Integer) 1))) {
            parse = parse.concat("33 ");
            marcaErrToken(2);
            equipara(new Token(TipoToken.TYPE_ASIGNACION, (Integer) 1), "Comparando");

            // Accion Semantica
            A_tipo = "logico";

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Asignacion incorrecta", token.getTipo(), "simbolo de asignacion '=' o '|='", 
                    codigoFuente, token.getLex(), token.getAtributo(), null);

        return A_tipo;
    }

    /**
     * Función para el analisis de la produccion L.
     * 
     * @return Lista de tipos (Es el comienzo, le sigue Q)
     */
    private NodePositionList<String> L() {
        NodePositionList<String> L_tipo;
        NodePositionList<String> Q_tipo;
        String E_tipo;

        // 34. L -> E Q
        if (type == TipoToken.TYPE_NEGACION || type == TipoToken.TYPE_IDENTIFICADOR ||
                type == TipoToken.TYPE_CADENA || type == TipoToken.TYPE_ENTERO || type == TipoToken.TYPE_REAL ||
                token.equals(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 0))) {
            parse = parse.concat("34 ");
            E_tipo = E();
            Q_tipo = Q();

            // Accion Semantica
            L_tipo = new NodePositionList<>();
            L_tipo.addLast(E_tipo);
            for (String t : Q_tipo)
                L_tipo.addLast(t);

        } // 35. L -> lambda FOLLOW(L) = {")"}
        else if (token.equals(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 1))) {
            parse = parse.concat("35 ");

            // Accion Semantica
            L_tipo = new NodePositionList<String>();

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Lista de argumentos mal formada", token.getTipo(),
                    "operador de negacion '!', 'identificador', parentesis abierto '(', cadena, constante entera, constante real", 
                    codigoFuente, token.getLex(), token.getAtributo(), null);

        return L_tipo;
    }

    /**
     * Función para el analisis de la produccion Q.
     * 
     * @return Lista de tipos (Es la continuacion de L)
     */
    private NodePositionList<String> Q() {
        NodePositionList<String> Q_tipo;
        NodePositionList<String> Q1_tipo;
        String E_tipo;

        // 36. Q -> , E Q
        if (type == TipoToken.TYPE_COMA) {
            parse = parse.concat("36 ");
            equipara(new Token(TipoToken.TYPE_COMA, (Integer) null), "Comparando");
            E_tipo = E();
            Q1_tipo = Q();

            // Accion Semantica
            Q_tipo = new NodePositionList<>();
            Q_tipo.addLast(E_tipo);
            for (String t : Q1_tipo)
                Q_tipo.addLast(t);

        } // 37. Q -> lambda FOLLOW(Q) = {")"}
        else if (token.equals(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 1))) {
            parse = parse.concat("37 ");

            // Accion Semantica
            Q_tipo = new NodePositionList<>();

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Lista de argumentos sin terminar", token.getTipo(), "',', parentesis cerrada ')'", codigoFuente,
                    token.getLex(), token.getAtributo(), null);

        return Q_tipo;
    }

    /**
     * Función para el analisis de la produccion X. Usado concretamente para el
     * return (return X).
     * 
     * @return X.tipo.
     */
    private String X() {
        String X_tipo;
        String E_tipo;

        // 38. X -> E
        if (type == TipoToken.TYPE_NEGACION || type == TipoToken.TYPE_IDENTIFICADOR ||
                type == TipoToken.TYPE_PARENTESIS || type == TipoToken.TYPE_CADENA ||
                type == TipoToken.TYPE_ENTERO || type == TipoToken.TYPE_REAL) {
            parse = parse.concat("38 ");
            E_tipo = E();

            // Accion Semantica
            X_tipo = E_tipo;

        } // 39. X -> lambda && FOLLOW(X) = {";"}
        else if (type == TipoToken.TYPE_PUNTO_Y_COMA) {
            parse = parse.concat("39 ");

            // Accion Semantica
            X_tipo = "vacio";

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Después de 'return' debe venir una expresión o un ';'", token.getTipo(),
                    "operador de negacion '!', 'identificador', parentesis abierto '(', cadena, constante entera, constante real, ';'", codigoFuente, token.getLex(), token.getAtributo(), null);

        return X_tipo;
    }

    /**
     * Función para el analisis de la produccion T. Son los tipos de datos.
     * 
     * @return T.tipo y T.ancho
     */
    private Pair<String, Integer> T() {
        String T_tipo;
        Integer T_ancho;

        // 43. T -> int
        if (type == TipoToken.TYPE_INT) {
            parse = parse.concat("43 ");
            equipara(new Token(TipoToken.TYPE_INT, (Integer) null), "Comparando");

            // Accion Semantica
            T_tipo = "entero";
            T_ancho = 1;

        } // 44. T -> float
        else if (type == TipoToken.TYPE_FLOAT) {
            parse = parse.concat("44 ");
            equipara(new Token(TipoToken.TYPE_FLOAT, (Integer) null), "Comparando");

            // Accion Semantica
            T_tipo = "real";
            T_ancho = 2;

        } // 45. T -> boolean
        else if (type == TipoToken.TYPE_BOOLEAN) {
            parse = parse.concat("45 ");
            equipara(new Token(TipoToken.TYPE_BOOLEAN, (Integer) null), "Comparando");

            // Accion Semantica
            T_tipo = "logico";
            T_ancho = 1;

        } // 46. T -> string
        else if (type == TipoToken.TYPE_STRING) {
            parse = parse.concat("46 ");
            equipara(new Token(TipoToken.TYPE_STRING, (Integer) null), "Comparando");

            // Accion Semantica
            T_tipo = "cadena";
            T_ancho = 64;

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Tipo de dato faltante o incorrecto", token.getTipo(),
                    "int, float, boolean, string", codigoFuente, token.getLex(), token.getAtributo(), null);

        return new Pair<String, Integer>(T_tipo, T_ancho);
    }

    /**
     * Función para el analisis de la produccion INI.
     * 
     * @return INI.tipo
     */
    private String INI() {
        String INI_tipo;
        String E_tipo;

        marcaInicioSentencia(); // Checkpoint para CONTEXTO del error

        // 47. INI -> id = E
        if (type == TipoToken.TYPE_IDENTIFICADOR) {
            parse = parse.concat("47 ");

            String idLex = token.getLex();
            int idPos = aseguraVarImpl(idLex);
            String idTipo = normalizarTipo(gestorTs.getTipo(idPos),false);

            token.setAtributo(idPos);
            token.setPos(idPos);
            equipara(new Token(TipoToken.TYPE_IDENTIFICADOR, (Integer) null), "Comparando");
            equipara(new Token(TipoToken.TYPE_ASIGNACION, (Integer) 0), "Inicializacion del bucle 'for' incorrecto");
            E_tipo = E();

            // Accion Semantica
            if (!idTipo.equals(E_tipo))
                errSem("Asignacion de id incorrecto",
                        "El id tiene tipo " + idTipo + " y no es compatible con el tipo " + E_tipo);

            INI_tipo = "tipo_ok";

        } // 48. INI -> lambda FOLLOW(INI) = {";"}
        else if (type == TipoToken.TYPE_PUNTO_Y_COMA) {
            parse = parse.concat("48 ");

            // Accion Semantica
            INI_tipo = "tipo_ok";
        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Inicialización de bucle 'for' incorrecto", token.getTipo(), "'identificador', ';'", codigoFuente,
                    token.getLex(), token.getAtributo(), null);

        return INI_tipo;
    }

    /**
     * Función para el analisis de la produccion ACTU.
     * 
     * @return ACTU.tipo
     */
    private String ACTU() {
        String ACTU_tipo;

        marcaInicioSentencia(); // Checkpoint para CONTEXTO del error

        // 49. ACTU -> id A E ;
        if (type == TipoToken.TYPE_IDENTIFICADOR) {
            parse = parse.concat("49 ");

            String idLex = token.getLex();
            int idPos = aseguraVarImpl(idLex);
            String idTipo = normalizarTipo(gestorTs.getTipo(idPos),false);

            token.setAtributo(idPos);
            token.setPos(idPos);
            equipara(new Token(TipoToken.TYPE_IDENTIFICADOR, (Integer) null), "Comparando");
            String A_tipo = A();
            String E_tipo = E();

            // Accion Semantica
            if (A_tipo.equals("tipo_ok")) {
                if (!idTipo.equals(E_tipo))
                    errSem("Asignación en sentencia de bucle 'for' incorrecto",
                            "El id tiene tipo " + idTipo + " y no es compatible con el tipo " + E_tipo);

            } else {
                if (A_tipo.equals("logico")) {
                    if (!E_tipo.equals("logico"))
                        errSem("Asignacion logico en sentencia de bucle 'for' incorrecto",
                                "La asignación lógica solo se puede aplicar a expresiones lógicas (se ha introducido tipo "
                                        + E_tipo + ")");
                    if (!idTipo.equals("logico"))
                        errSem("Asignación lógica a id en sentencia de bucle 'for' incorrecto",
                                "El identificador " + idLex + " debe ser de tipo logico, pero es " + idTipo);
                }
            }
            ACTU_tipo = "tipo_ok";
        } // 50. ACTU -> lambda FOLLOW(ACTU) = {")"}
        else if (token.equals(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 1))) {
            parse = parse.concat("50 ");
            ACTU_tipo = "tipo_ok";
        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Actualización de bucle 'for' incorrecto", token.getTipo(), "'identificador', parentesis cerrado ')'", codigoFuente,
                    token.getLex(), token.getAtributo(), null);

        return ACTU_tipo;
    }

    /**
     * Función para el analisis de la produccion C.
     * 
     * @return C.tipo y C.ret
     */
    private Pair<String, String> C() {
        String C_tipo;
        String C_ret;
        String S_tipo;
        String S_ret;
        String C1_tipo;
        String C1_ret;

        // 51. C -> S C
        if (type == TipoToken.TYPE_IDENTIFICADOR || type == TipoToken.TYPE_WRITE ||
                type == TipoToken.TYPE_READ || type == TipoToken.TYPE_RETURN ||
                type == TipoToken.TYPE_LET || type == TipoToken.TYPE_IF || type == TipoToken.TYPE_FOR) {
            parse = parse.concat("51 ");

            Pair<String, String> parS = S();
            S_tipo = parS.getLeft();
            S_ret = parS.getRight();
            Pair<String, String> parC = C();
            C1_tipo = parC.getLeft();
            C1_ret = parC.getRight();

            // Accion Semantica
            if (!S_tipo.equals("tipo_ok"))
                errSem("Sentencia incorrecta en cuerpo", "La sentencia es incorrecta en anidamientos de cuerpos");

            C_tipo = C1_tipo;

            // Logica interesante para C_ret:
            // Si S_ret es tipo_ok entonces no tiene "return" y por tanto depende C1_ret
            // Si S_ret tiene return, miro si C1_ret aporta return o no, en caso que no,
            // pues depende de S_ret (unico return) y en caso que C1_ret aporte return tb,
            // tengo que asegurarme que sea del mismo tipo que S_ret
            if (S_ret.equals("tipo_ok"))
                C_ret = C1_ret;
            else {
                if (!(C1_ret.equals("tipo_ok") || S_ret.equals(C1_ret)))
                    errSem("Returns incompatibles",
                            "En el mismo bloque hay returns de tipos distintos: " + S_ret + " y " + C1_ret);

                C_ret = S_ret;
            }

        } // 52. C -> lambda FOLLOW(C) = {"}"}
        else if (token.equals(new Token(TipoToken.TYPE_LLAVE, (Integer) 1))) {
            parse = parse.concat("52 ");

            // Accion Semantica
            C_tipo = "tipo_ok";
            C_ret = "tipo_ok";

        } else{
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Sentencia incorrecta en el bloque de codigo o bloque de código no terminado", token.getTipo(), "'identificador', sentencia escribir 'write', sentencia leer 'read', sentencia de retorno de funcion 'return', declaracion de variable 'let', sentencia condicional 'if', sentencia de bucle 'for', llave cerrada \'}\')",
                    codigoFuente, token.getLex(), token.getAtributo(), null);
        }
        return new Pair<String, String>(C_tipo, C_ret);
    }

    /**
     * Función para el analisis de la produccion H.
     * 
     * @return H.tipo
     */
    private String H() {
        String H_tipo;
        String T_tipo;

        // 54. H -> T
        if (type == TipoToken.TYPE_INT || type == TipoToken.TYPE_FLOAT ||
                type == TipoToken.TYPE_BOOLEAN || type == TipoToken.TYPE_STRING) {
            parse = parse.concat("54 ");
            Pair<String, Integer> par = T();
            T_tipo = par.getLeft();

            // Accion Semantica
            H_tipo = T_tipo;

        } // 55. H -> void
        else if (type == TipoToken.TYPE_VOID) {
            parse = parse.concat("55 ");
            equipara(new Token(TipoToken.TYPE_VOID, (Integer) null), "Comparando");

            // Accion Semantica
            H_tipo = "vacio"; // Simbolizo que no necesito return

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Se esperaba un tipo de retorno o 'void' en la declaracion de funcion", token.getTipo(), "int, float, boolean, string, void",
                    codigoFuente, token.getLex(), token.getAtributo(), null);

        return H_tipo;
    }

    /**
     * Función para el analisis de la produccion B. Comienzo de lista param para fun
     * en TSLocal.
     * 
     * @return Lista de tipos
     */
    private NodePositionList<String> B() {

        NodePositionList<String> B_tipo = new NodePositionList<>();
        NodePositionList<String> K_tipo;

        // 56. B -> T id K
        if (type == TipoToken.TYPE_INT || type == TipoToken.TYPE_FLOAT ||
                type == TipoToken.TYPE_BOOLEAN || type == TipoToken.TYPE_STRING) {
            parse = parse.concat("56 ");

            Pair<String, Integer> T_par = T();
            String T_tipo = T_par.getLeft();
            int T_ancho = T_par.getRight();
            String idLex = token.getLex();

            // Compruebo que no se este redeclarando
            int idPos = gestorTs.getEntradaTSLocal(idLex);
            if (idPos != 0) {
                errSem("Error de redeclaración de variable",
                        "La variable " + idLex + " ya esta declarado como " + normalizarTipo(gestorTs.getTipo(idPos),false)
                                + "y no admite redeclaracion a tipo "+T_tipo);
            }

            // Insertar parametro en TS local
            idPos = gestorTs.addEntradaTSLocal(idLex);
            gestorTs.setTipo(idPos, normalizarTipo(T_tipo,true));
            gestorTs.setValorAtributoEnt(idPos, ATR_DIR, despLoc);
            gestorTs.setValorAtributoEnt(idPos, ATR_PARAM, 1);
            despLoc += T_ancho;

            token.setAtributo(idPos);
            token.setPos(idPos);
            equipara(new Token(TipoToken.TYPE_IDENTIFICADOR, (Integer) null), "Parametros de funcion incorrectos");

            K_tipo = K();
            // B_tipo = T.tipo x K_tipo (si k = tipo_ok -> lambda = solo T.tipo)
            B_tipo.addLast(T_tipo);
            for (String tipoK : K_tipo)
                if (!tipoK.equals("tipo_ok")) {
                    B_tipo.addLast(tipoK);
                }
        } // 57. B -> void
        else if (type == TipoToken.TYPE_VOID) {
            parse = parse.concat("57 ");
            equipara(new Token(TipoToken.TYPE_VOID, (Integer) null), "Comparando");
            B_tipo.addLast("vacio");
        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Tipo de parametro incorrecto al declarar una funcion", token.getTipo(),
                    "int, float, boolean, string, void", codigoFuente, token.getLex(), token.getAtributo(), null);

        return B_tipo;
    }

    /**
     * Función para el analisis de la produccion K. Continuacion de B en TSLocal.
     * 
     * @return Lista de tipos
     */
    private NodePositionList<String> K() {
        NodePositionList<String> K_tipo = new NodePositionList<>();
        NodePositionList<String> K1_tipo;
        String T_tipo;
        Integer T_ancho;

        // 58. K -> , T id K
        if (type == TipoToken.TYPE_COMA) {
            parse = parse.concat("58 ");
            equipara(new Token(TipoToken.TYPE_COMA, (Integer) null),"Comparando");

            Pair<String, Integer> T_par = T();
            T_tipo = T_par.getLeft();
            T_ancho = T_par.getRight();
            int idPos;
            String idLex = token.getLex();

            // Compruebo que no se este redeclarando
            idPos = gestorTs.getEntradaTSLocal(idLex);
            if (idPos != 0) {
                errSem("Error de redeclaración de variable",
                        "La variable " + idLex + " ya esta declarado como " + normalizarTipo(gestorTs.getTipo(idPos),false)
                                + " y no admite redeclaracion a tipo "+T_tipo);
            }

            idPos = gestorTs.addEntradaTSLocal(idLex);
            gestorTs.setTipo(idPos, normalizarTipo(T_tipo,true));
            gestorTs.setValorAtributoEnt(idPos, ATR_DIR, despLoc);
            gestorTs.setValorAtributoEnt(idPos, ATR_PARAM, 1);
            despLoc += T_ancho;

            token.setAtributo(idPos);
            token.setPos(idPos);
            equipara(new Token(TipoToken.TYPE_IDENTIFICADOR, (Integer) null), "Parametros de funcion incorrectos");

            K1_tipo = K();
            K_tipo.addLast(T_tipo);
            for (String tipoK1 : K1_tipo)
                if (!tipoK1.equals("tipo_ok")) {
                    K_tipo.addLast(tipoK1);
                }

        } // 59. K -> lambda FOLLOW(K) = {")"}
        else if (token.equals(new Token(TipoToken.TYPE_PARENTESIS, (Integer) 1))) {
            parse = parse.concat("59 ");
            K_tipo.addLast("tipo_ok");

        } else
            throw new Token_Inesperado(lineaAnterior, columnaAnterior, linea,  lineaSF,
                    "Lista de parametros en declaracion de una funcion incorrectos o sin acabar", token.getTipo(), "',', parentesis cerrado ')'", codigoFuente,
                    token.getLex(), token.getAtributo(), null);
        return K_tipo;
    }
}