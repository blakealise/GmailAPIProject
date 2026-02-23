package org.example;

import java.io.IOException;
import java.security.GeneralSecurityException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws GeneralSecurityException, IOException {
        GoogleAuthHelper.prepareGmailService();
        GoogleAuthHelper.listUnreadTickets();
    }

}
