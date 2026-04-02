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
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class calendarApp {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Calendar API Java Quickstart";

    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static Calendar service;
    private static final String CREDENTIALS_FILE_PATH = "/cookie.json";
    static List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
// CALENDAR (not CALENDAR_READONLY) — you need write access for creating and deleting

    public static void prepareCalendarService() throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
         service =
                new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
    }

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

    public static void listUpcomingEvents() throws IOException {
        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = service.events().list("primary")
                .setMaxResults(10)
                .setTimeMin(now)          // only events starting from now
                .setOrderBy("startTime")
                .setSingleEvents(true)    // ← IMPORTANT: see note below
                .execute();

        for (Event event : events.getItems()) {
            EventDateTime start = event.getStart();
            String when = (start.getDateTime() != null)
                    ? start.getDateTime().toString()
                    : start.getDate().toString();
            System.out.println(event.getSummary() + " @ " + when);
            System.out.println("  ID: " + event.getId());  // print ID for delete/patch operations
        }
    }

    public static void searchByDateRange(String startDateStr, String endDateStr) throws IOException {
        DateTime now = new DateTime(System.currentTimeMillis());
        LocalDate userDate = LocalDate.parse(startDateStr);
        DateTime startOfDay = new DateTime(
                userDate.atStartOfDay(ZoneId.of("America/New_York"))
                        .toInstant().toEpochMilli());
        LocalDate userDate2 = LocalDate.parse(endDateStr);
        DateTime endOfDay = new DateTime(
                userDate2.atStartOfDay(ZoneId.of("America/New_York"))
                        .toInstant().toEpochMilli());
        Events events = service.events().list("primary")
                .setMaxResults(10)
                .setTimeMin(now)          // only events starting from now
                .setOrderBy("startTime")
                .setSingleEvents(true)    // ← IMPORTANT: see note below
                .setTimeMin(startOfDay)
                .setTimeMax(endOfDay)
                .execute();

        for (Event event : events.getItems()) {
            EventDateTime start = event.getStart();
            String when = (start.getDateTime() != null)
                    ? start.getDateTime().toString()
                    : start.getDate().toString();
            System.out.println(event.getSummary() + " @ " + when);
            System.out.println("  ID: " + event.getId());  // print ID for delete/patch operations
        }
    }

    public static void createEvent() throws IOException {

        Scanner scan = new Scanner(System.in);
        System.out.println("enter the year");
        Integer year = scan.nextInt();

        Scanner scan2 = new Scanner(System.in);
        System.out.println("enter the month");
        Integer month = scan2.nextInt();

        Scanner scan3 = new Scanner(System.in);
        System.out.println("enter the day");
        Integer day = scan3.nextInt();

        Scanner scan4= new Scanner(System.in);
        System.out.println("enter the hour");
        Integer hour = scan4.nextInt();

        Scanner scan5 = new Scanner(System.in);
        System.out.println("enter the minute");
        Integer minute = scan.nextInt();

        Scanner scan6 = new Scanner(System.in);
        System.out.println("enter the title");
        String summery = scan6.nextLine();

        Scanner scan7 = new Scanner(System.in);
        System.out.println("enter the description");
        String description = scan6.nextLine();

        Scanner scan8 = new Scanner(System.in);
        System.out.println("enter the duration in hours");
        Integer duration = scan6.nextInt();

        // 1. Build the Event object
        Event event = new Event()
                .setSummary(summery)
                .setDescription(description);

// 2. Set start time
        ZonedDateTime startZdt = LocalDateTime.of(year, month, day, hour, minute)
                .atZone(ZoneId.of("America/New_York"));
        event.setStart(new EventDateTime()
                .setDateTime(new DateTime(startZdt.toInstant().toEpochMilli()))
                .setTimeZone("America/New_York"));

// 3. Set end time (1 hour later)
        event.setEnd(new EventDateTime()
                .setDateTime(new DateTime(startZdt.plusHours(duration).toInstant().toEpochMilli()))
                .setTimeZone("America/New_York"));

        /*
            First scanner - do you wanna add attendees?
            Second scanner - attendee email
         */
// 4. Add attendees (optional)
        ArrayList<EventAttendee> peopleInvited = new ArrayList<>();
        boolean keepGoing = true;
        while(keepGoing) {
            //scann1 - add? if no, set keep going to false
            Scanner scann1 = new Scanner(System.in);
            System.out.println("do you want to add an attendee? answer yes or no");
            String yesorno = scann1.nextLine();

                //if yes
                  /*
                        prompt for email
                        EventAttendee personInvited = new EventAttendee();
                        personInvited.setEmail(scan2);
                        peopleInvited.add(personInvited)
                   */
            if(yesorno.equalsIgnoreCase("yes")){
                Scanner scann2 = new Scanner(System.in);
                System.out.println("enter email of attendee");
                String email = scann2.nextLine();

                EventAttendee personInvited = new EventAttendee();
                personInvited.setEmail(email);
                peopleInvited.add(personInvited);
            }
            else{
                keepGoing = false;
            }
        }


        event.setAttendees(peopleInvited);


// 6. Insert the event
        Event created = service.events()
                .insert("primary", event)
                .setSendUpdates("all")   // sends invite emails to attendees
                .execute();
        System.out.println("Created: " + created.getHtmlLink());
        System.out.println("Event ID: " + created.getId());  // save this to update/delete later
    }

    public static void checkAvailability() throws IOException {
        // Check the next hour
        Scanner scan1 = new Scanner(System.in);
        System.out.println("whats the start day? - 'yyyy-mm-dd' ");
        String startDay = scan1.next();
        Scanner scan2 = new Scanner(System.in);
        System.out.println("whats the start time? - 'hh-mm' ");
        String startTime = scan2.next();
        Scanner scan3 = new Scanner(System.in);
        System.out.println("whats the end day - 'yyyy-mm-dd' ");
        String endDay = scan3.next();
        Scanner scan4 = new Scanner(System.in);
        System.out.println("whats the end time - 'hh:mm' "); //needs to be hh:mm not hh-mm
        String endTime = scan4.next();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        //LocalDate loCal = LocalDate.parse();
        //startDay needs to be converted to a DateTime
        System.out.println("I got all the info!");

        //We first turn the user response into 4 different objects
        LocalDate startLD = LocalDate.parse(startDay);
        LocalTime startLT = LocalTime.parse(startTime);
        LocalDate endLD = LocalDate.parse(endDay);
        LocalTime endLT = LocalTime.parse(endTime);

        //We then need to merge the start LocalDate and LocalTime into a singular object
        //Same for end
        ZonedDateTime zdtStart = LocalDateTime.of(
                LocalDate.parse(startLD.toString()),   // "2026-03-05" ? a date object
                LocalTime.parse(startLT.toString())    // "10:30" ? a time object
        ).atZone(ZoneId.of("America/New_York"));
        ZonedDateTime zdtEnd = LocalDateTime.of(
                LocalDate.parse(endLD.toString()),   // "2026-03-05" ? a date object
                LocalTime.parse(endLT.toString())    // "10:30" ? a time object
        ).atZone(ZoneId.of("America/New_York"));

        //We then use the method offered by ZonedDateTime to get it in the format/class that Google likes
        DateTime startDT = new DateTime(zdtStart.toInstant().toEpochMilli());
        DateTime endDT = new DateTime(zdtEnd.toInstant().toEpochMilli());



        FreeBusyRequest request = new FreeBusyRequest()
                .setTimeMin(startDT)
                .setTimeMax(endDT)
                .setItems(List.of(new FreeBusyRequestItem().setId("primary")));

        FreeBusyResponse response = service.freebusy().query(request).execute();

// Parse the response: it's a map from calendar ID ? list of busy windows
        var busySlots = response.getCalendars().get("primary").getBusy();
        if (busySlots == null || busySlots.isEmpty()) {
            System.out.println("? Calendar is FREE during this window.");
        } else {
            System.out.println("? Busy during:");
            for (var slot : busySlots) {
                // Each slot has a startDay and end — these are RFC3339 timestamp strings
                // Example: "2026-03-05T10:00:00-05:00" to "2026-03-05T11:00:00-05:00"
                System.out.println("  " + slot.getStart() + "  ?  " + slot.getEnd());
            }
        }
    }

    public static void deleteEvent() throws IOException {
        // PATCH: update only specific fields — everything else is unchanged
        Scanner scan = new Scanner(System.in);
        System.out.println("give event ID");
        String eventId = scan.next();
        System.out.println("are you sure? answer yes or no");
        String response = scan.next();
        if(response.equalsIgnoreCase("no")){
            System.out.println("lol too late she gone 😂");
        }

        Event patch = new Event().setSummary("Renamed Meeting");
        service.events().patch("primary", eventId, patch)
                .setSendUpdates("all").execute();

// UPDATE: replaces the entire event object — unset fields get cleared!
// Use this when you want to change many fields at once.
        Event fullUpdate = service.events().get("primary", eventId).execute();
        fullUpdate.setSummary("New Title");
        fullUpdate.setDescription("Updated description");
        service.events().update("primary", eventId, fullUpdate).execute();

// DELETE
        service.events().delete("primary", eventId).execute();
        System.out.println("Event deleted");
    }

    public static void listCalendars() throws IOException {
        // List every calendar visible to the authenticated user
        var calendarList = service.calendarList().list().execute();
        for (CalendarListEntry entry : calendarList.getItems()) {
            System.out.println(entry.getSummary() + "  (ID: " + entry.getId() + ")");
            // Use entry.getId() wherever the API asks for a calendarId
            // "primary" is just a shortcut alias for the user's main calendar
        }
    }

    public static void runMenu() throws IOException {
        boolean cont = true;
        while (cont) {
            Scanner scan = new Scanner(System.in);
            System.out.println(
                    "+------------------------------------+\n" +
                            "|   Smart Scheduling Assistant  ??   |\n" +
                            "|------------------------------------|\n" +
                            "|  1. List upcoming events           |\n" +
                            "|  2. Search by date range           |\n" +
                            "|  3. Create an event                |\n" +
                            "|  4. Check availability (FreeBusy)  |\n" +
                            "|  5. Delete an event                |\n" +
                            "|  6. List all calendars             |\n" +
                            "|  0. Exit                           |\n" +
                            "+------------------------------------+\n" +
                            "Choice: ");
            int ans = scan.nextInt();
            if(ans == 1){
                listUpcomingEvents();
            }
            if(ans == 2){
                Scanner scan1 = new Scanner(System.in);
                System.out.println("enter start date - 'yyyy-mm-dd' ");
                String start = scan1.nextLine();
                System.out.println("enter end date - 'yyyy-mm-dd' ");
                String end = scan1.nextLine();
                searchByDateRange(start,end);
            }
            if(ans == 3){
                createEvent();
            }
            if(ans == 4){
                checkAvailability();
            }
            if(ans == 5){
                deleteEvent();
            }
            if(ans == 6){
                listCalendars();
            }
            if(ans == 0){
                cont = false;
            }
        }
    }
}

