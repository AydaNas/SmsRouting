package utils;

import Erreur.MissingQuotationMark;
import Erreur.Unrecognized_backslash;

import java.util.ArrayList;

public class utils_class {
    public utils_class() {}

    public static ArrayList<String> user_itf_terminal_commande(String commande) throws MissingQuotationMark, Unrecognized_backslash {
        ArrayList<String> list_of_string =  new ArrayList<>();
        String Element = "";
        boolean backslash = false;
        boolean proctected = false;
        for(char character :  commande.toCharArray()){
            switch (character){
                case '\\':
                    if(backslash){
                        Element += character;
                        backslash = false;
                    }
                    else
                        {backslash = true;}
                    break;
                case '"':
                    if(backslash){
                        Element += character;
                        backslash = false;
                    }else {
                        proctected= !proctected;
                    }
                    break;
                case ' ', '\t':
                    if(proctected||backslash){
                        Element += character;
                    }else {
                        if(!Element.isEmpty()){
                            list_of_string.add(Element);
                            Element = "";
                        }
                    }
                    break;
                case '\0', '\n', '\r':
                    if(backslash){
                        throw new Unrecognized_backslash("Unrecognized_backslash");
                    }
                    list_of_string.add(Element);
                    break;
                default:
                    if(backslash){
                        throw new Unrecognized_backslash("Unrecognized_backslash");
                    }
                    Element += character;
            }
        }
        if(proctected){
            throw new MissingQuotationMark("Missing quotation mark");
        }
        if(!Element.isEmpty()){
            list_of_string.add(Element);
        }

        return list_of_string;
    }
}
