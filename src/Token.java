import Enumeraciones.TipoToken;

public class Token {

    private TipoToken tipo;
    private Integer atributo;
    private String atrLex;
    private double atrNum;
    private int pos;

    public Token(TipoToken tipo, Integer atributo) {
        this.tipo = tipo;
        this.atributo = atributo;
    }

    public Token(TipoToken tipo, String lex){
        this.tipo = tipo;
        this.atrLex = lex;
    }

    public Token(TipoToken tipo, double num) {
        this.tipo = tipo;
        this.atrNum = num;
    }

    public TipoToken getTipo() {
        return tipo;
    }

    public Integer getAtributo() {
        return atributo;
    }

    public String getLex() {
        return atrLex;
    }

    public double getNum() {
        return atrNum;
    }

    public int getPos() {
        return pos;
    }

    public void setAtributo(int pos){
        this.atributo = pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public void setLexema(String lex){
        this.atrLex = lex;
    }
    
    @Override
    public boolean equals(Object obj){
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof Token))
            return false;

        Token other = (Token) obj;

    // Compara tipo
    if (this.tipo != other.tipo)
        return false;

    // Solo compara atributo si al menos uno lo tiene
    if (this.atributo != null || other.atributo != null) {
        return this.atributo != null && this.atributo.equals(other.atributo);
    }

    return true;

    }
}