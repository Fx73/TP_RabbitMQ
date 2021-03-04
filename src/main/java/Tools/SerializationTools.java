package Tools;

public class SerializationTools {

    public static String myStringParser(String[] origin){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= origin.length - 2; i++) {
            sb.append(origin[i]).append("<-delimiter->");
        }
        if(origin.length>0) sb.append(origin[origin.length - 1]);
        return sb.toString();
    }


    public static String[] myStringUnparser(String origin){
        String [] s = origin.split("<-delimiter->");
        return s;
    }


}
