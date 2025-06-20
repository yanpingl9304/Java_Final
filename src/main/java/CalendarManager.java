import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class CalendarManager {

    public static String currentUser = "";

    public static void handleCalendarCommand(String[] commands, MessageReceivedEvent event) {
        if (commands.length < 3) {
            sendHelpMessage(event);
            return;
        }

        String option = commands[1];
        switch (option.toLowerCase()) {
            case "adddate":
                handleAddDate(commands, event);
                break;
            case "addtime":
                handleAddTime(commands, event);
                break;
            case "delete":
                handleDelete(commands, event);
                break;
            case "listevent":
                handleListEvent(commands, event);
                break;
            case "updatedate":
                handleUpdateDate(commands, event);
                break;
            case "updatetime":
                handleUpdateTime(commands, event);
                break;
            default:
                sendHelpMessage(event);
                break;
        }
    }

    private static void handleAddDate(String[] commands, MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        if (commands.length >= 5) {
            commands[3] = commands[3].toUpperCase();
            commands[4] = commands[4].toUpperCase();
            try {
                GoogleCalendarHelper helper = new GoogleCalendarHelper(currentUser);
                Calendar service = helper.getService();
                String eventName = commands[2];
                LocalDate endDateExclusive = LocalDate.parse(commands[4]).plusDays(1);
                if (!commands[3].matches("\\d{4}-\\d{2}-\\d{2}") || !commands[4].matches("\\d{4}-\\d{2}-\\d{2}")) {
                    channel.sendMessage("\u274C Invalid date format. Please use YYYY-MM-DD.").queue();
                    return;
                }
                DateTime startDate = new DateTime(commands[3]);
                DateTime endDate = new DateTime(endDateExclusive.toString());

                helper.addEventDate(service, eventName, startDate, endDate);
                channel.sendMessage("Successfully set \ud83d\uddd3" + commands[3] + "~" + commands[4] + " | added : \"" + eventName + "\"").queue();
            } catch (Exception e) {
                e.printStackTrace();
                channel.sendMessage("\u274C Failed to add calendar event.").queue();
            }
        }
    }

    private static void handleAddTime(String[] commands, MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        if (commands.length >= 5) {
            try {
                commands[3] += ":00";
                commands[4] += ":00";
                GoogleCalendarHelper helper = new GoogleCalendarHelper(currentUser);
                Calendar service = helper.getService();
                String eventName = commands[2];

                LocalDateTime startDateTime = LocalDateTime.parse(commands[3]);
                LocalDateTime endDateTime = LocalDateTime.parse(commands[4]);

                ZoneId zoneId = ZoneId.of("Asia/Taipei");
                ZonedDateTime zonedStart = startDateTime.atZone(zoneId);
                ZonedDateTime zonedEnd = endDateTime.atZone(zoneId);

                DateTime startDateTimeGoogle = new DateTime(zonedStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                DateTime endDateTimeGoogle = new DateTime(zonedEnd.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

                helper.addEventTime(service, eventName, startDateTimeGoogle, endDateTimeGoogle);
                channel.sendMessage("Successfully set \ud83d\uddd3 " + commands[3] + " ~ " + commands[4] + " | added : \"" + eventName + "\"").queue();
            } catch (Exception e) {
                e.printStackTrace();
                channel.sendMessage("\u274C Failed to add calendar time event.").queue();
            }
        }
    }

    private static void handleDelete(String[] commands, MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        if (commands.length >= 4) {
            try {
                GoogleCalendarHelper helper = new GoogleCalendarHelper(currentUser);
                Calendar service = helper.getService();
                String title = commands[2];
                String dateString = commands[3];
                if (!dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    channel.sendMessage("\u274C Invalid date format. Please use YYYY-MM-DD.").queue();
                    return;
                }
                DateTime targetDate = new DateTime(dateString + "T00:00:00Z");
                if (helper.deleteEventByTitleAndDate(service, title, targetDate)) {
                    channel.sendMessage("Successfully deleted event \"" + title + "\" on " + dateString).queue();
                } else {
                    channel.sendMessage("No matching event \"" + title + "\" on " + dateString + " is found.").queue();
                }
            } catch (Exception e) {
                e.printStackTrace();
                channel.sendMessage("\u274C Failed to delete calendar event.").queue();
            }
        }
    }

    private static void handleListEvent(String[] commands, MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        try {
            GoogleCalendarHelper helper = new GoogleCalendarHelper(currentUser);
            Calendar service = helper.getService();
            if (commands.length == 3) {
                DateTime now = new DateTime(System.currentTimeMillis());
                channel.sendMessage(helper.listEvents(service, Integer.parseInt(commands[2]), now)).queue();
            } else if (commands.length == 4) {
                String dateString = commands[3];
                if (!dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    channel.sendMessage("\u274C Invalid date format. Please use YYYY-MM-DD.").queue();
                    return;
                }
                DateTime targetDate = new DateTime(dateString + "T00:00:00Z");
                channel.sendMessage(helper.listEvents(service, Integer.parseInt(commands[2]), targetDate)).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
            channel.sendMessage("\u274C Failed to list calendar event.").queue();
        }
    }

    private static void handleUpdateTime(String[] commands, MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        if (commands.length >= 6) {
            try {
                commands[4] += ":00";
                commands[5] += ":00";
                String eventId = commands[2];
                String newTitle = commands[3];
                DateTime start = new DateTime(commands[4]);
                DateTime end = new DateTime(commands[5]);

                GoogleCalendarHelper helper = new GoogleCalendarHelper(currentUser);
                Calendar service = helper.getService();
                helper.updateEventTime(service, eventId, newTitle, start, end);

                channel.sendMessage("\u2705 Event " + newTitle + " updated successfully!").queue();
            } catch (Exception e) {
                e.printStackTrace();
                channel.sendMessage("\u274C Failed to update time event: " + e.getMessage()).queue();
            }
        }
    }

    private static void handleUpdateDate(String[] commands, MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        if (commands.length >= 6) {
            try {
                String eventId = commands[2];
                String newTitle = commands[3];
                LocalDate localStart = LocalDate.parse(commands[4]);
                LocalDate localEndExclusive = LocalDate.parse(commands[5]).plusDays(1);

                DateTime start = new DateTime(localStart.toString());
                DateTime end = new DateTime(localEndExclusive.toString());

                GoogleCalendarHelper helper = new GoogleCalendarHelper(currentUser);
                Calendar service = helper.getService();
                helper.updateEventDate(service, eventId, newTitle, start, end);

                channel.sendMessage("\u2705 All-day event `" + newTitle + "` updated successfully!").queue();
            } catch (Exception e) {
                e.printStackTrace();
                channel.sendMessage("\u274C Failed to update all-day event: " + e.getMessage()).queue();
            }
        }
    }

    private static void sendHelpMessage(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        String message = "formats:\n\n"
                + "\"calendar  (adddate)  (title)  (yyyy/mm/dd)  (yyyy/mm/dd)\" \n"
                + "\"calendar  (addtime)  (title)  (yyyy/mm/ddTxx:xx)  (yyyy/mm/ddTxx:xx)\"\n"
                + "\"calendar  (delete)  (title)  (yyyy/mm/dd)\"\n"
                + "\"calendar  listevent  (number)\"\n"
                + "\"calendar updatedate <eventId> <newTitle> (yyyy/mm/dd)  (yyyy/mm/dd)\"\n"
                + "\"calendar updatetime <eventId> <newTitle> (yyyy/mm/ddTxx:xx)  (yyyy/mm/ddTxx:xx)\"\n"
                + "example input :\n\n"
                + "calendar adddate test 2025-10-01 2025-10-02\n"
                + "calendar addtime test 2025-10-01T00:00 2025-10-02T12:00\n"
                + "calendar delete test 2025-10-01\n"
                + "calendar listevent 5\n"
                + "calendar updatedate asdasdasdasd hellooooooo 2025-10-01 2025-10-03\n"
                + "calendar updatetime asdasdasdasd hellooooooo 2025-10-01T00:00 2025-10-03T12:00\n";
        channel.sendMessage(message).queue();
    }
} 
