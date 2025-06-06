
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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Queue;

public class Listeners extends ListenerAdapter {

    String apiKey = Main.getApiKey();
    ConfigManager config = ConfigManager.getInstance();

    Listeners(){

    }

    /*
     * TO DO LIST 
     * 2. Add favorite function
     */

    public void deleteAllCommand(@NotNull ReadyEvent event , Guild guild){

        // 🔹 刪除全域 Slash Commands
        event.getJDA().retrieveCommands().queue(globalCommands -> {
            System.out.println("刪除全域指令:");
            for (Command command : globalCommands) {
                System.out.println(" - " + command.getName());
                event.getJDA().deleteCommandById(command.getId()).queue();
            }
        });

        if (guild != null) {
            // 刪除 Guild Slash Commands
            guild.retrieveCommands().queue(guildCommands -> {
                System.out.println("刪除伺服器指令:");
                for (Command command : guildCommands) {
                    System.out.println(" - " + command.getName());
                    guild.deleteCommandById(command.getId()).queue();
                }
            });
        } else {
            System.out.println("⚠ 無法取得指定的 Guild，請確認 guildId 是否正確。");
        }


    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        for (Guild guild : event.getJDA().getGuilds()) {

            /*
             * 在伺服器註冊指令
             */
            
            guild.upsertCommand("units", "Unit, Metric is Celsius/KPH and Imperial is Fahrenheit/MPH.")
                .addOptions(new OptionData(OptionType.STRING, "unit", "Unit, Metric is Celsius/KPH and Imperial is Fahrenheit/MPH.", true)
                    .addChoice("Metric", "metric")
                    .addChoice("Imperial", "imperial"))
                .queue();

            guild.upsertCommand("addfavorite", "Adds a place to your favorite places.")
                .addOptions(new OptionData(OptionType.STRING, "place", "Enter the place in the format: {city},{country}", true))
                .queue();

            guild.upsertCommand("favorite", "Select one of your saved places.")
                .addOptions(new OptionData(OptionType.STRING, "place", "Choose from your saved places.", true)
                .setAutoComplete(true))
                .queue();

            guild.upsertCommand("removefavorite", "Remove from your saved places.")
                .addOptions(new OptionData(OptionType.STRING, "place", "Choose from your saved places.", true)
                .setAutoComplete(true))
                .queue();
        }
    }


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String commandLine = event.getMessage().getContentRaw().toLowerCase();
        String[] commands = commandLine.split(" ");
        EmbedBuilder embed = new EmbedBuilder();
        if(commands[0].equalsIgnoreCase("weather")) {
            String option = commands[commands.length - 1];

            if(commands.length == 2) {
                System.out.println("==================================================================");
                System.out.println("GetCurrentWeather");
                embed = GetCurrentWeather(commands[1]);
                event.getChannel().sendMessageEmbeds(embed.build()).queue();
                System.out.println("==================================================================");
            } else if(commands.length == 3) {

                String cityName = commands[1];
                if (cityName.contains("_")) cityName = cityName.replace("_", "%20");

                switch (option) {
                    case "detail":
                        embed = getDetailWeather(event,cityName);
                        event.getChannel().sendMessageEmbeds(embed.build()).queue();
                        break;
                    case "hourly":
                        embed = getHourlyForecast(event ,cityName);
                        event.getChannel().sendMessageEmbeds(embed.build()).queue();
                        break;
                    case "weekly":
                        embed = getWeeklyForecast(event,cityName);
                        event.getChannel().sendMessageEmbeds(embed.build()).queue();
                        break;
                    default:
                        event.getChannel().sendMessage("Usage: weather [city] [function], Current we have functions:\n" +
                                                       "detail: getDetailWeather\n" +
                                                       "daily: getDailyWeather\n" +
                                                       "hourly: getHourlyWeather").queue();
                        break;
                }
            }
        }
        if (commands[0].equalsIgnoreCase("calendar")){
            if (commands.length >= 5){
                commands[3] = commands[3].toUpperCase();
                commands[4] = commands[4].toUpperCase();
                if (commands[1].equals("date")){
                    try {
                        GoogleCalendarHelper helper = new GoogleCalendarHelper();
                        Calendar service = helper.getService();
                        String eventName = commands[2];
                        LocalDate endDateExclusive = LocalDate.parse(commands[4]).plusDays(1);

                        DateTime startDate = new DateTime(commands[3]);
                        DateTime endDate = new DateTime(endDateExclusive.toString());
                        String[] startDateString = commands[3].split("-");
                        String[] endDateString = commands[4].split("-");

                        helper.addEventDate(service, eventName, startDate, endDate);
                        event.getChannel().sendMessage("Successfully set 📅" + startDateString[1] + "/" + startDateString[2] + "~" + endDateString[1] + "/" + endDateString[2] + " | added : " + eventName).queue();
                    } catch (IOException | GeneralSecurityException e) {
                        e.printStackTrace();
                        event.getChannel().sendMessage("❌ Failed to add calendar event: " + e.getMessage()).queue();
                    }
                }
                else if (commands[1].equals("time")){
                    commands[3] += ":00";
                    commands[4] += ":00";
                    try {
                        GoogleCalendarHelper helper = new GoogleCalendarHelper();
                        Calendar service = helper.getService();
                        String eventName = commands[2];
                        

                        String startDateInput[] = commands[3].split("T");
                        String endDateInput[] = commands[4].toString().split("T");

                        LocalDateTime startDateTime = LocalDateTime.parse(commands[3]);
                        LocalDateTime endDateTime = LocalDateTime.parse(commands[4].toString());


                        String[] startDateString = startDateInput[0].split("-");
                        String[] endDateString = endDateInput[0].split("-");


                        ZoneId zoneId = ZoneId.of("Asia/Taipei");
                        ZonedDateTime zonedStart = startDateTime.atZone(zoneId);
                        ZonedDateTime zonedEnd = endDateTime.atZone(zoneId);

                        DateTime startDateTimeGoogle = new DateTime(zonedStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                        DateTime endDateTimeGoogle = new DateTime(zonedEnd.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

                        helper.addEventTime(service, eventName, startDateTimeGoogle, endDateTimeGoogle);
                        event.getChannel().sendMessage("Successfully set 📅" + startDateInput[0] + " " + startDateInput[1] + " ~ " + endDateInput[0] + " " + endDateInput[1] + " | added : " + eventName).queue();
                    } catch (IOException | GeneralSecurityException e) {
                        e.printStackTrace();
                        event.getChannel().sendMessage("❌ Failed to add calendar event: " + e.getMessage()).queue();
                    }
                }
            }
            else if (commands[1].equals("help")){
                event.getChannel().sendMessage("formats:\n\"calendar  (date)  (title)  (yyyy/mm/dd)  (yyyy/mm/dd)\" \n" + 
                                                        "\"calendar  (time)  (title)  (yyyy/mm/dd(Txx:xx))  (yyyy/mm/dd(Txx:xx))\"\n" +
                                                        "example input :\ncalendar date test 2025-10-01 2025-10-02\n" +
                                                        "calendar time test 2025-10-01T00:00 2025-10-02T12:00\n").queue();
            }
        }


}




    
    // get weather data
    public JSONObject getJSONData(String urlString) {
        try{
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if(conn.getResponseCode() == 400) {
                return null;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            conn.disconnect();

            JSONObject obj = new JSONObject(content.toString());
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    

    public void listUpcomingEvents(){

    }


    // get current weather information including current temperature ,day temperature ,night temperature and weather condition
    public EmbedBuilder GetCurrentWeather(String city){
        city = city.replaceAll(" ", "%20");
        String urlString = "http://api.weatherapi.com/v1/current.json?key=" + apiKey + "&q=" + city + "&aqi=no";
        JSONObject weatherData = getJSONData(urlString);
        
        if(weatherData == null) return null;

        String condition = weatherData.getJSONObject("current").getJSONObject("condition").getString("text");
        String iconUrl = weatherData.getJSONObject("current").getJSONObject("condition").getString("icon");
        String location = weatherData.getJSONObject("location").getString("name");
        String country = weatherData.getJSONObject("location").getString("country");

        double temperature = 0;
        double feelsLike = 0;
 
        if(config.isMetric()){
            temperature = weatherData.getJSONObject("current").getDouble("temp_c");
            feelsLike = weatherData.getJSONObject("current").getDouble("feelslike_c");
        } else {
            temperature = weatherData.getJSONObject("current").getDouble("temp_f");
            feelsLike = weatherData.getJSONObject("current").getDouble("feelslike_f");
        }
        
        if (iconUrl.startsWith("//")) {
            iconUrl = "https:" + iconUrl;
        }


        System.out.println("地點：" + location + ", " + country);
        System.out.println("溫度：" + temperature + "°C");
        System.out.println("天氣狀況：" + condition);
        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setImage(iconUrl);
        embed.setTitle("Current Weather Information in " + location + "," + country);

        if(config.isMetric()) {
            embed.setDescription("Temperature : " + temperature + "°C" +"\n"
                    +"Weather : " + condition+"\n"
                    +"Feels Like " + feelsLike + "°C" + "\n\n"
                    );
        } else {
            embed.setDescription("Temperature : " + temperature + "°F" +"\n"
                                +"Weather : " + condition+"\n"
                                +"Feels Like " + feelsLike + "°F" + "\n\n"
                                );
        }


        embed.setColor(0x3498DB); //Sky Blue

        return embed;
    }

    // get detail weather information including  high/low temperature ,wind speed ,humidity ,and UV ,classify humidity and UV
    public EmbedBuilder getDetailWeather(MessageReceivedEvent event ,String city){
        city = city.replaceAll(" ", "%20");

        String urlStringForecast = "http://api.weatherapi.com/v1/forecast.json?key="+ apiKey + "&q=" + city + "&days=1&aqi=no&alerts=no"; 

        JSONObject forecastWeatherData = getJSONData(urlStringForecast);
        
        String location = forecastWeatherData.getJSONObject("location").getString("name");
        String country = forecastWeatherData.getJSONObject("location").getString("country");
        double humidity = forecastWeatherData.getJSONObject("current").getDouble("humidity");
        double UV = forecastWeatherData.getJSONObject("current").getDouble("uv");

        double wind = 0;
        double maxtemp = 0;
        double mintemp = 0;
        if(config.isMetric()){
            wind = forecastWeatherData.getJSONObject("current").getDouble("wind_kph");
            maxtemp = forecastWeatherData.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONObject("day").getDouble("maxtemp_c");
            mintemp = forecastWeatherData.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONObject("day").getDouble("mintemp_c");
        } else {
            wind = forecastWeatherData.getJSONObject("current").getDouble("wind_mph");
            maxtemp = forecastWeatherData.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONObject("day").getDouble("maxtemp_f");
            mintemp = forecastWeatherData.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONObject("day").getDouble("mintemp_f");            
        }


        // output
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Current Weather Detail Information in "+ location + "," + country);
        if(config.isMetric()) {
            embed.setDescription("🌡️   |   Temperature High / Low : " + maxtemp + "°C / " + mintemp + "°C"  + "\n"
                                    +"💨   |   Wind : " + wind +" KPH \n"
                                    +"💧   |   Humidity : " + humidity + "%\n"
                                    +"☀️   |   UV : " + UV + " " + UVLevel(UV));
        } else {
            embed.setDescription("🌡️   |   Temperature High / Low : " + maxtemp + "°F / " + mintemp + "°F"  + "\n"
                        +"💨   |   Wind : " + wind +" MPH \n"
                        +"💧   |   Humidity : " + humidity + "%\n"
                        +"☀️   |   UV : " + UV + " " + UVLevel(UV));
        }


        embed.setColor(0x3498DB); //Sky Blue

        return embed;
    }
    
    public EmbedBuilder getWeeklyForecast(MessageReceivedEvent event ,String city) {

        city = city.replaceAll(" ", "%20");

        String urlStringForecast = "http://api.weatherapi.com/v1/forecast.json?key="+ apiKey + "&q=" + city + "&days=7&aqi=no&alerts=no"; 
        JSONObject forecastWeatherData = getJSONData(urlStringForecast);
        JSONArray weatherDataArray = forecastWeatherData.getJSONObject("forecast").getJSONArray("forecastday");
        String location = forecastWeatherData.getJSONObject("location").getString("name");
        String country = forecastWeatherData.getJSONObject("location").getString("country");

        StringBuilder sb = new StringBuilder();
        for(int i = 0 ; i < weatherDataArray.length() ; i++){
            String line = "📅  ";

            JSONObject obj = weatherDataArray.getJSONObject(i);

            String condition = obj.getJSONObject("day").getJSONObject("condition").getString("text").trim();
            String monthAndDay = obj.getString("date").toString().replace("2025-" , "").replace("-","/");
            int daily_chance_of_rain = obj.getJSONObject("day").getInt("daily_chance_of_rain");

            double maxtemp = 0;
            double mintemp = 0;

            if(config.isMetric()) {
                maxtemp = obj.getJSONObject("day").getDouble("maxtemp_c");
                mintemp = obj.getJSONObject("day").getDouble("mintemp_c");
            } else {
                maxtemp = obj.getJSONObject("day").getDouble("maxtemp_f");
                mintemp = obj.getJSONObject("day").getDouble("mintemp_f");
            }

            if(config.isMetric()) {
                line += (monthAndDay + "  |  " + condition +"  | 🌡️  " + maxtemp + "°C / " + mintemp + "°C" + " |  🌧️  "+ daily_chance_of_rain +"%\n");
            } else {
                line += (monthAndDay + "  |  " + condition +"  | 🌡️  " + maxtemp + "°F / " + mintemp + "°F" + " |  🌧️  "+ daily_chance_of_rain +"%\n");
            }
            
            line += "========================================================\n";

            sb.append(line);
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Weekly Weather Forecast of "+ location + "," + country);
        embed.setDescription(sb.toString());
        embed.setColor(0x3498DB); //Sky Blue

        return embed;
    }

    public EmbedBuilder getHourlyForecast(MessageReceivedEvent event ,String city){
        String urlStringForecast = "http://api.weatherapi.com/v1/forecast.json?key="+ apiKey + "&q=" + city + "&days=2&aqi=no&alerts=no"; 
        JSONObject forecastTodayWeatherData = getJSONData(urlStringForecast);

        String localtime = forecastTodayWeatherData.getJSONObject("location").getString("localtime");
        String location = forecastTodayWeatherData.getJSONObject("location").getString("name");
        String country = forecastTodayWeatherData.getJSONObject("location").getString("country");
        LocalDateTime local = LocalDateTime.parse(localtime.replace(" " , "T"));
        int hourOfCurrentTime = local.getHour();
        JSONArray forecastByHourToday = forecastTodayWeatherData.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONArray("hour");
        JSONArray forecastByHourTomorrow = forecastTodayWeatherData.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(1).getJSONArray("hour");

        int counter = 0;
        StringBuilder sb = new StringBuilder();

        for (int i = hourOfCurrentTime; i < forecastByHourToday.length() && counter < 5; i++) {
            JSONObject data = forecastByHourToday.getJSONObject(i);
            String line = HourlyInfoBuilder(data, i, hourOfCurrentTime);
            sb.append(line);
            counter++;
        }

        if (counter < 5 && forecastByHourTomorrow != null) {
            for (int i = 0; i < forecastByHourTomorrow.length() && counter < 5; i++) {
                JSONObject data = forecastByHourTomorrow.getJSONObject(i);
                String line = HourlyInfoBuilder(data, i, hourOfCurrentTime);
                sb.append(line);
                counter++;
            }
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Hourly Weather Forecast of "+ location + "," + country);
        embed.setDescription(sb.toString());
        embed.setColor(0x3498DB); //Sky Blue

        System.out.println(sb.toString());

        return embed;
    }

    public String dayOfWeekFormat(String input){
        LocalDate date = LocalDate.parse(input);
        String dayOfWeek = date.getDayOfWeek().toString().toLowerCase();
        String reslut = "";

        switch (dayOfWeek) {
            case "monday":
                reslut = "Mon.";
                break;
            case "tuesday":
                reslut = "Tue.";
                break;
            case "wednesday":
                reslut = "Wed.";
                break;
            case "thursday":
                reslut = "Thu.";
                break;
            case "friday":
                reslut = "Fri.";
                break;
            case "saturday":
                reslut = "Sat.";
                break;
            case "sunday":
                reslut = "Sun.";
                break;
            default:
                break;
        }

        return reslut;
    }

    public String hourFormat(int hour) {
        if (hour == 0) return "12am";
        else if (hour == 12) return "12pm";
        else if (hour > 12) return (hour - 12) + "pm";
        else return hour + "am";
    }

    public String HourlyInfoBuilder(JSONObject data , int i , int hourOfCurrentTime) {
        String time = data.getString("time").replace(" ", "T");

        LocalDateTime dateTime = LocalDateTime.parse(time);
        String hour = hourFormat(dateTime.getHour());
        String condition = data.getJSONObject("condition").getString("text");
        double chance_of_rain = data.getDouble("chance_of_rain");
        double temp = 0;

        if(config.isMetric()) {
            temp = data.getDouble("temp_c");
        } else {
            temp = data.getDouble("temp_f");
        }

        String line = "🕐  | ";
        if(i == hourOfCurrentTime) line += "Now\n";
        else line += hour + "\n";

        line += "☁️   | " + condition + "\n";

        if(config.isMetric()) {
            line += "🌡️   | " + temp + "°C\n";
        } else {
            line += "🌡️   | " + temp + "°F\n";
        }

        line += "🌧️   | " + chance_of_rain + "%\n";
        line += "=========================\n";

        return line;
    }


    // classify the humidity level
    public String HumidityLevel(String Humidity){
        int humidityInt = Integer.parseInt(Humidity.replace("%",""));
        String level = "";
        if(humidityInt < 30) {
            level = "TOO DRY";
        } else if (30<=humidityInt && humidityInt < 60) {
            level = "COMFORTABLE";
        } else if (60<=humidityInt && humidityInt < 100) {
            level = "TOO HIGH";
        } else if (humidityInt == 100) {
            level = "MAYBE RAINY NOW";
        }
        return level;
    }

    // classify the UV level
    public String UVLevel(double UV){

        String level = "";
        UV = (int)UV;
        if(0<=UV && UV <=2) {
            level = "LOW";
        } else if (3<=UV && UV <=5) {
            level = "MODERATE";
        } else if (6<=UV && UV <=7) {
            level = "HIGH";
        } else if (8<=UV && UV <=10) {
            level = "VERY HIGH";
        } else if (11<=UV) {
            level = "EXTREME";
        }

        return level;
    }

    public void sendWeatherMessage(Object event, EmbedBuilder embed) {
        if (event instanceof SlashCommandInteractionEvent) {
            ((SlashCommandInteractionEvent) event).replyEmbeds(embed.build()).queue();
        } else if (event instanceof MessageReceivedEvent) {
            ((MessageReceivedEvent) event).getChannel().sendMessageEmbeds(embed.build()).queue();
        }
    }

}
