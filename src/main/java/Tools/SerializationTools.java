package Tools;

public class SerializationTools {
    public static final String SEPARATOR = "<-I_AM_A_SEPARATOR->";

    public static String myStringParser(String[] origin){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= origin.length - 2; i++) {
            sb.append(origin[i]).append(SEPARATOR);
        }
        if(origin.length>0) sb.append(origin[origin.length - 1]);
        return sb.toString();
    }


    public static String[] myStringUnparser(String origin){
        String [] s = origin.split(SEPARATOR);
        return s;
    }


}
