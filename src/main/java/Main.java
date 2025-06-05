

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class Main {

    private static final String TOKEN = "MTI0NzEwMjI5NDU5MjMyNzcyMA.GocUWA            中文               .Fhtk7w5lo0FyoZC9wlId4ZfqM0LB8gMdcss2lk";
    private static final String API_KEY = "c28ce52e2f1a4d5d98444219252405";

    public static String getApiKey(){
        return API_KEY;
    }

    // Build a Bot
    public static void main(String[] args) throws LoginException {

        JDA jda = JDABuilder.createDefault(TOKEN)
                  .enableIntents(GatewayIntent.GUILD_MEMBERS)
                  .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                  .build();
        jda.addEventListener(new Listeners());
        jda.addEventListener(new SlashCommand());
        // jda.addEventListener(new Travel());
    }
}
