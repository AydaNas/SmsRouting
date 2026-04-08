
import Erreur.MissingQuotationMark;
import Erreur.Unrecognized_backslash;

import java.util.ArrayList;
public class test_function_utils_class {
    public static void main(String[] args) {
        String test_message="send 20 \"hello world\"";
        System.out.println("test_message:"+test_message);
        try {
            ArrayList<String> string_list = utils.utils_class.user_itf_terminal_commande(test_message);
            for(String string:string_list){
                System.out.print(string+"_");
            }
            System.out.println();
        } catch (MissingQuotationMark e) {
            System.out.println("missing quotation mark");
        } catch (Unrecognized_backslash e){
            System.out.println("Unrecognized backslash");
        }

        String test_message_missing_quote="send 20 \"hello world";
        System.out.println("test_message_missing_quote:"+test_message_missing_quote);
        try {
            ArrayList<String> string_list = utils.utils_class.user_itf_terminal_commande(test_message_missing_quote);
            for(String string:string_list){
                System.out.print(string+"_");
            }
            System.out.println();
        } catch (MissingQuotationMark e) {
            System.out.println("missing quotation mark");
        }catch (Unrecognized_backslash e){
            System.out.println("Unrecognized backslash");
        }

        String test_message_quote = "send 20 \"\\\"hello world\\\"\"";
        System.out.println("test_message_quote:"+test_message_quote);
        try {
            ArrayList<String> string_list = utils.utils_class.user_itf_terminal_commande(test_message_quote);
            for(String string:string_list){
                System.out.print(string+"_");
            }
            System.out.println();
        } catch (MissingQuotationMark e) {
            System.out.println("missing quotation mark");
        }catch (Unrecognized_backslash e){
            System.out.println("Unrecognized backslash");
        }
    }
}
