package org.example;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.Scanner;

public class GoogleAuthHelper {

    /* class to demonstrate use of Gmail list labels API */
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    /**
     * Global instance of the JSON factory.
     */
    private static Gmail service;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    static List<String> SCOPES = Arrays.asList(
            GmailScopes.GMAIL_MODIFY,  // Read, label, trash
            GmailScopes.GMAIL_SEND     // Send emails
    );
    private static final String CREDENTIALS_FILE_PATH = "/cookie.json";

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = GoogleAuthHelper.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        //returns an authorized Credential object.
        return credential;
    }


    public static void prepareGmailService(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static Gmail getService() {
        return service;
    }

    public static String readTicket(String messageId) throws IOException {
        // Fetch full message (not just metadata)
        Message message = service.users().messages()
                .get("me", messageId)
                .setFormat("full")       // "full" gives us the entire MIME tree including body
                .execute();// Case 1: Simple single-part message — body data is directly in payload

        var parts = message.getPayload().getParts();
        if (parts != null) {
            for (var part : parts) {
                if ("text/plain".equals(part.getMimeType())) {   // find the plain-text part
                    String partData = part.getBody().getData();
                    byte[] bytes = Base64.getUrlDecoder().decode(partData);
                    return new String(bytes, StandardCharsets.UTF_8);
                }
            }
        }


        return "no messages found 🍪💌📭";
    }

    public static void listUnreadTickets() throws IOException {
        ListMessagesResponse listResponse = service.users()
                .messages()
                .list("me")           // "me" = the authenticated user
                .setQ("is:unread label:inbox")
                .setMaxResults(10L)
                .execute();

        if (listResponse.getMessages() == null) {
            System.out.println("No messages found.");
            return;
            }
        for (Message msgRef : listResponse.getMessages()) {
            Message fullMsg = service.users().messages()
                    .get("me", msgRef.getId())
                    .setMetadataHeaders(Arrays.asList("Subject", "From", "Date"))
                    .execute();

            System.out.println("Checking ID of: " + msgRef.getId());
            System.out.println(readTicket(msgRef.getId()));



            String subject = extractHeader(fullMsg,"Subject");

            String from = extractHeader(fullMsg, "From");

            String date = extractHeader(fullMsg, "Date");

            System.out.println("• " + subject + from + date);

        }

    }

    public static void searchTickets(Gmail service, String query) throws IOException {
        ListMessagesResponse listResponse = service.users()
                .messages()
                .list("me")           // "me" = the authenticated user
                .setQ(query)
                .setMaxResults(20L)
                .execute();
        if (listResponse.getMessages() == null) {
            System.out.println("No messages found.");
            return;
        }
        for (Message msgRef : listResponse.getMessages()) {
            Message fullMsg = service.users().messages()
                    .get("me", msgRef.getId())
                    .setFormat("metadata")
                    .setMetadataHeaders(Arrays.asList("Subject", "From"))
                    .execute();

            String subject = fullMsg.getPayload().getHeaders()
                    .stream()
                    .filter(h -> "Subject".equals(h.getName()))   // lambda: keep only the "Subject" header
                    .map(h -> h.getValue())                        // lambda: extract its value string
                    .findFirst()
                    .orElse("(no subject)");

            String from = fullMsg.getPayload().getHeaders()
                    .stream()
                    .filter(h -> "From".equals(h.getName()))   // lambda: keep only the "Subject" header
                    .map(h -> h.getValue())                        // lambda: extract its value string
                    .findFirst()
                    .orElse("(no sender)");

            System.out.println("• " + subject + from);

        }

    }

    public static String extractHeader(Message object, String title){
        String from = object.getPayload().getHeaders()
                .stream()
                .filter(h -> title.equals(h.getName()))   // lambda: keep only the "Subject" header
                .map(h -> h.getValue())                        // lambda: extract its value string
                .findFirst()
                .orElse("🍪");
        return from;
    }


    public static void replyToTicket(String messageId, String replyBody) throws MessagingException, IOException {
        // Step 1: Fetch original message to get threading info
        // We need: who to reply to, the subject, the Message-ID header, and the threadId
        Message original = service.users().messages()
                .get("me", messageId)
                .setFormat("metadata")
                .setMetadataHeaders(Arrays.asList("Subject", "From", "Message-ID"))
                .execute();

        String originalSubject   = extractHeader(original, "Subject");
        String originalFrom      = extractHeader(original, "From");
        String originalMessageId = extractHeader(original, "Message-ID");  // e.g. <abc123@gmail.com>
        String threadId          = original.getThreadId();                  // Gmail's thread ID

        // Step 2: Build reply MimeMessage
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress("me"));
        email.addRecipient(MimeMessage.RecipientType.TO,
                new InternetAddress(originalFrom));            // reply goes back to original sender
        email.setSubject("🐶replying to: " + originalSubject);        // conventional "Re:" prefix
        email.setText(replyBody);

        // Step 3: Set threading headers — these tell email clients this is a reply
        email.setHeader("In-Reply-To", originalMessageId);
        email.setHeader("References", originalMessageId);

        // Step 4: Encode and send (same pattern as sending a new email)
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        String encodedEmail = Base64.getUrlEncoder()
                .encodeToString(buffer.toByteArray());

        Message gmailMessage = new Message();
        gmailMessage.setRaw(encodedEmail);
        gmailMessage.setThreadId(threadId);  // tell Gmail which thread this belongs to

        service.users().messages().send("me", gmailMessage).execute();
        System.out.println("Reply sent in thread: " + threadId);
    }

    public static void applyLabel(String messageId, String labelName) throws IOException {
        // List all labels — both system and custom

        /*
                We wanna label an email DataStructures
                Does the label already exist?
                    Yes
                    No

         */

        var labels = service.users().labels().list("me").execute();

        boolean doesExist = false;
        String labelId = "";
        for (Label label : labels.getLabels()) {
            if(labelName.equals(label.getName())) {
                // labelId comes from listing or creating labels above
                labelId = label.getId();
                doesExist = true;
            }
        }

        if(doesExist == true){
            ModifyMessageRequest request = new ModifyMessageRequest()
                    .setAddLabelIds(Collections.singletonList(labelId))       // labels to add
                    .setRemoveLabelIds(Collections.singletonList("UNREAD"));  // labels to remove (marks as read)

            service.users().messages()
                    .modify("me", messageId, request)
                    .execute();
            return;
        }
        else{
            Label newLabel = new Label()
                    .setName(labelName)
                    .setLabelListVisibility("labelShow")      // show in the Gmail sidebar
                    .setMessageListVisibility("show");        // show on messages in the list

            Label created = service.users().labels()
                    .create("me", newLabel)
                    .execute();

            System.out.println("Created label ID: " + created.getId());  // save this ID for later use
        }



    }


    public static void trashTicket(String messageId) throws IOException {
        // Move message to trash (recoverable for 30 days)
        service.users().messages()
                .trash("me", messageId)
                .execute();

        System.out.println("Message moved to Trash (30-day retention)");
    }

    public static void runMenu() throws IOException, MessagingException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("+----------------------------------+\n" +
                "¦      Help-Desk Bot  \uD83D\uDCEC           ¦\n" +
                "¦----------------------------------¦\n" +
                "¦  1. List unread tickets          ¦\n" +
                "¦  2. Search tickets               ¦\n" +
                "¦  3. Read full ticket             ¦\n" +
                "¦  4. Reply to ticket              ¦\n" +
                "¦  5. Label ticket IN_PROGRESS     ¦\n" +
                "¦  6. Trash a ticket               ¦\n" +
                "¦  0. Exit                         ¦\n" +
                "+----------------------------------+\n" +
                "Choice:  ");
        while(scanner.nextInt() != 0) {
            if (scanner.nextInt() == 1) {
                GoogleAuthHelper.listUnreadTickets();
            }
            if(scanner.nextInt() == 2){
                Scanner quarry = new Scanner(System.in);
                System.out.println("What's your query");
                String userAnswer = quarry.nextLine();
                GoogleAuthHelper.searchTickets(service, userAnswer);
            }
            if(scanner.nextInt() == 3){
                Scanner printme = new Scanner(System.in);
                System.out.println("What's your message Id");
                String userAnswer = printme.nextLine();
                GoogleAuthHelper.readTicket(userAnswer);
            }
            if(scanner.nextInt() == 4){
                Scanner printme = new Scanner(System.in);
                System.out.println("What's your message Id");
                String userAnswer = printme.nextLine();
                Scanner print2 = new Scanner(System.in);
                System.out.println("What's your response");
                String userAnswer2 = print2.nextLine();
                GoogleAuthHelper.replyToTicket(userAnswer, userAnswer2 );
            }
            if(scanner.nextInt() == 5){
                Scanner printme = new Scanner(System.in);
                System.out.println("What's your message Id");
                String userAnswer = printme.nextLine();
                Scanner print2 = new Scanner(System.in);
                System.out.println("What's the label");
                String userAnswer2 = print2.nextLine();
                GoogleAuthHelper.applyLabel(userAnswer, userAnswer2 );
            }
            if(scanner.nextInt() ==6){
                Scanner printme = new Scanner(System.in);
                System.out.println("What's your message Id");
                String userAnswer = printme.nextLine();
                GoogleAuthHelper.trashTicket(userAnswer);
            }
        }
    }

}