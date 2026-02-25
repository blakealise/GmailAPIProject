package org.example;

import jakarta.mail.MessagingException;

import java.io.IOException;
import java.security.GeneralSecurityException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws GeneralSecurityException, IOException, MessagingException {
        GoogleAuthHelper.prepareGmailService();
        //GoogleAuthHelper.listUnreadTickets();
       // GoogleAuthHelper.replyToTicket("19c91096456eb202","food for thought: 🍿🍦🥖🍍🧁🍪🍣");
        //GoogleAuthHelper.applyLabel("19c91096456eb202" , "emails from blake");
    }

}
