/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package org.uga.distributed;

import java.util.Scanner;

/**
 *
 * @author ensimag
 */
public class SMSRouting {

    public static void main(String[] args) throws Exception {
      
if(args.length < 2){
    System.out.println("");
    System.out.println("for user user id user <id>");
}
        String role = args[0].toLowerCase();
        String id = args[1];
        if(role.equals("antenna")){
            double x = Double.parseDouble(args[2]);
            double y = Double.parseDouble(args[3]);
            Antenna antenna = new Antenna();
            antenna.antennaInit(id,x,y);
            antenna.startAntenna();
            while (true) {
                Thread.sleep(30000); 
                
            }}
          
        
        else if(role.equals("user")){
            User user = new User();
            user.startUser(id);
            
        
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("you are user with id" + id);
    System.out.println("To send sms type: sms <receiverId> <messageContent>");

        while (true) {
         String smsContent = scanner.nextLine();
            if(smsContent.startsWith("sms")){
                String [] parts = smsContent.split(" ",3);
                if (parts.length == 3){
                    String receiverId = parts[1];
                    String messageContent = parts[2];
                    user.sendSMS(receiverId,messageContent);
                }
                 else {
                    System.err.println("Invalid SMS format. Use: sms <receiverId> <messageContent>");
                }
            } 
            
        }
        
     }
     else {
            System.err.println("Invalid role specified. Use 'antenna' or 'user'.");
        }       
     }

 }

