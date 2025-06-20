import com.google.api.client.auth.oauth2.Credential;
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
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class GoogleCalendarHelper {
    private static final String APPLICATION_NAME = "Google Calendar API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    // private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static String token_folder;
    private static String user = "";
    private Calendar service;

    public GoogleCalendarHelper(String user) throws GeneralSecurityException, IOException {
        // Determine the folder based on user input (A or B)
        if ("A".equalsIgnoreCase(user)) {
            this.token_folder = "tokens-userA";
        } else if ("B".equalsIgnoreCase(user)) {
            this.token_folder = "tokens-userB";
        } else {
            throw new IllegalArgumentException("Invalid user: " + user);
        }

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        this.service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public Calendar getService() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // Create and return an authorized Calendar client
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = GoogleCalendarHelper.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(token_folder)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp(flow, receiver)
                .authorize("user");
    }

    public List<String> getUpcomingEvents(int maxResults) throws IOException {
        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = service.events().list("primary")
                .setMaxResults(maxResults)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        List<Event> items = events.getItems();
        List<String> result = new ArrayList<>();
        if (items.isEmpty()) {
            result.add("No upcoming events found.");
        } else {
            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    start = event.getStart().getDate();
                }
                result.add(String.format("%s (%s)", event.getSummary(), start));
            }
        }
        return result;
    }

    public void addEventDate(Calendar service, String Name, DateTime startDateTime, DateTime endDateTime) throws IOException {
        Event event = new Event()
            .setSummary(Name)
            .setDescription("Created via Discord Bot");
        EventDateTime start = new EventDateTime()
                .setDate(startDateTime)
                .setTimeZone("Asia/Taipei"); // use your timezone
        event.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDate(endDateTime)
                .setTimeZone("Asia/Taipei");
        event.setEnd(end);

        event = service.events().insert("primary", event).execute();
        System.out.printf("Event created: %s\n", event.getHtmlLink());
    }


    public void addEventTime(Calendar service, String Name, DateTime startDateTime, DateTime endDateTime) throws IOException {
        Event event = new Event()
            .setSummary(Name)
            .setDescription("Created via Discord Bot");
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("Asia/Taipei"); // use your timezone
        event.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("Asia/Taipei");
        event.setEnd(end);

        event = service.events().insert("primary", event).execute();
        System.out.printf("Event created: %s\n", event.getHtmlLink());
    }

    public boolean deleteEventByTitleAndDate(Calendar service, String title, DateTime date) throws IOException {
        Events events = service.events().list("primary")
            .setTimeMin(date)
            .setTimeMax(new DateTime(date.getValue() + 24 * 60 * 60 * 1000))  // 1 day later
            .setSingleEvents(true)
            .execute();
        boolean deleted = false;
        for (Event event : events.getItems()) {
            if (event.getSummary().equalsIgnoreCase(title)) {
                deleted = true;
                service.events().delete("primary", event.getId()).execute();
                System.out.println("Deleted event: " + title);
                return deleted;
            }
        }
        System.out.println("No matching event \"" + title + "\" on " + date + " is found.");
        return deleted;
    }
    public String listEvents(Calendar service, int maxResults, DateTime dateInput) throws IOException {
        Events events = service.events().list("primary")
            .setMaxResults(maxResults)
            .setTimeMin(dateInput)
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute();

        List<Event> items = events.getItems();
        if (items.isEmpty()) {
            return "📭 No upcoming events found.";
        } else {
            StringBuilder sb = new StringBuilder("📅 **Upcoming Events:**\n");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (Event event : items) {
                String eventName = event.getSummary();
                DateTime start = event.getStart().getDateTime();
                DateTime end = event.getEnd().getDateTime();

                if (start != null && end != null) {
                    // Timed event
                    ZonedDateTime startZdt = Instant.ofEpochMilli(start.getValue()).atZone(ZoneId.of("Asia/Taipei"));
                    ZonedDateTime endZdt = Instant.ofEpochMilli(end.getValue()).atZone(ZoneId.of("Asia/Taipei"));

                    String formattedStart = startZdt.format(timeFormatter);
                    String formattedEnd = endZdt.format(timeFormatter);

                    sb.append("• ").append(eventName)
                    .append(" — ").append(formattedStart)
                    .append(" to ").append(formattedEnd)
                    .append(" (ID: ").append(event.getId()).append(")\n");
                } else {
                    // All-day event
                    LocalDate startDate = LocalDate.parse(event.getStart().getDate().toStringRfc3339());
                    LocalDate endDateExclusive = LocalDate.parse(event.getEnd().getDate().toStringRfc3339());
                    LocalDate endDateInclusive = endDateExclusive.minusDays(1);

                    sb.append("• ").append(eventName)
                    .append(" — ").append(startDate.format(dateFormatter))
                    .append(" to ").append(endDateInclusive.format(dateFormatter))
                    .append(" (ID: ").append(event.getId()).append(") (All-day)\n");
                }
            }
            return sb.toString();
        }
    }
    public void updateEventDate(Calendar service, String eventId, String newSummary, DateTime newStart, DateTime newEnd) throws IOException {
        Event event = service.events().get("primary", eventId).execute();

        if (newSummary != null) {
            event.setSummary(newSummary);
        }

        if (newStart != null && newEnd != null) {
            // Use setDate() for all-day events (not setDateTime())
            EventDateTime start = new EventDateTime().setDate(newStart);
            EventDateTime end = new EventDateTime().setDate(newEnd);
            event.setStart(start);
            event.setEnd(end);
        }

        service.events().update("primary", event.getId(), event).execute();
    }


    public void updateEventTime(Calendar service, String eventId, String newSummary, DateTime newStart, DateTime newEnd) throws IOException {
        Event event = service.events().get("primary", eventId).execute();

        if (newSummary != null) {
            event.setSummary(newSummary);
        }

        if (newStart != null && newEnd != null) {
            EventDateTime start = new EventDateTime().setDateTime(newStart).setTimeZone("Asia/Taipei");
            EventDateTime end = new EventDateTime().setDateTime(newEnd).setTimeZone("Asia/Taipei");
            event.setStart(start);
            event.setEnd(end);
        }

        service.events().update("primary", event.getId(), event).execute();
    }

}