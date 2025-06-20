import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;


public class Main {

    private static final String TOKEN = "MTI0NzEwMjI5NDU5MjMyNzcyMA.G490HJ.              4l0JZ3dNVqkZdi4QPIdS1ldW27Gt5d_KErY1Bs".replaceAll(" ","");
    private static final String API_KEY = "214fdac5000b4882bd052918250706";

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
    }
}
