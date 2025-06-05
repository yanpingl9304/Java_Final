

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class Travel extends ListenerAdapter {

    // @Override
    // public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    //     String message = event.getMessage().getContentRaw();
    //     String[] messageSplit = message.split(" ");

    //     if(messageSplit[0].equalsIgnoreCase("travel") && messageSplit.length == 1) {
    //         event.getChannel().sendMessage("Where would you like to travel?\n" +
    //                                        "Let me tell you the weather over there ,please enter airport code\n" +
    //                                        "travel [Airport Code]").queue();
    //     } else if (messageSplit[0].equalsIgnoreCase("travel") && messageSplit.length == 2) {
    //         JSONObject jsonCity = getJSONFile(messageSplit[0]);
    //         if(jsonCity.has(messageSplit[1])) {
    //             messageSplit[1] = messageSplit[1].toUpperCase();
    //             EmbedBuilder embed = getFlagsAndTime(messageSplit[1],jsonCity);
    //             event.getChannel().sendMessage("").setEmbeds(embed.build()).queue();
    //             // GetCurrentWeather(event, messageSplit[1],jsonCity);
    //         } else {
    //             event.getChannel().sendMessage("City " + messageSplit[1].toUpperCase() + " not found in the JSON data.").queue();
    //         }
    //     } else if (messageSplit[0].equalsIgnoreCase("travel") && messageSplit.length == 3) {
    //         JSONObject jsonCity = getJSONFile(messageSplit[0]);
    //         EmbedBuilder embed = getFlagsAndTime(messageSplit[1],jsonCity);
    //         event.getChannel().sendMessage("").setEmbeds(embed.build()).queue();
    //         switch (messageSplit[2]) {
    //             case "daily" :
    //                 if(jsonCity.has(messageSplit[1])) {
    //                     getDailyForecast(event, messageSplit[1],jsonCity);
    //                 } else {
    //                     event.getChannel().sendMessage("Airport " + messageSplit[1].toUpperCase() + " not found in the JSON data.").queue();
    //                 }
    //                 break;
    //             case "detail" :
    //                 if(jsonCity.has(messageSplit[1])) {
    //                     getDetailWeather(event, messageSplit[1],jsonCity);
    //                 } else {
    //                     event.getChannel().sendMessage("Airport " + messageSplit[1].toUpperCase() + " not found in the JSON data.").queue();
    //                 }
    //                 break;
    //             case "hourly" :
    //                 if(jsonCity.has(messageSplit[1])) {
    //                     getHourlyForecast(event, messageSplit[1],jsonCity);
    //                 } else {
    //                     event.getChannel().sendMessage("Airport " + messageSplit[1].toUpperCase() + " not found in the JSON data.").queue();
    //                 }
    //                 break;
    //             default:
    //                 break;
    //         }
    //     }
    // }
    // // get country flag of the airport's country
    // public EmbedBuilder getFlagsAndTime(String airportCode , JSONObject jsonCity) {
    //     EmbedBuilder embed = new EmbedBuilder();
    //     try {
    //         String url =  jsonCity.getString(airportCode);
    //         Document doc = Jsoup.connect(url).get();
    //         Elements elements = doc.select("span.CurrentConditions--timestamp--1ybTk");
    //         String time = elements.text().replace("As of ","");

    //         String flagUrl = "";
    //         String jsonFlagsString = Main.readConfigFile("Flags.json");
    //         JSONObject jsonFlags = new JSONObject(jsonFlagsString);
    //         if(jsonFlags.has(airportCode)) {
    //             flagUrl = jsonFlags.getString(airportCode);
    //         } else {
    //             flagUrl = null;
    //         }

    //         embed.setTitle("Local time "+time);
    //         embed.setImage(flagUrl);
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    //     return embed;
    // }
}
