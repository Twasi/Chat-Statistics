package net.twasiplugin.chatstatistics;

import net.twasi.core.plugin.TwasiPlugin;
import net.twasi.core.plugin.api.TwasiUserPlugin;
import net.twasi.core.services.ServiceRegistry;
import net.twasi.twitchapi.kraken.chat.response.EmoticonDTO;

import java.util.List;
import java.util.Map;

import static net.twasi.twitchapi.TwitchAPI.kraken;

public class ChatStatisticsPlugin extends TwasiPlugin {

    public static ChatStatisticsService service;

    public static Map<String, List<EmoticonDTO>> globalEmotes;

    public Class<? extends TwasiUserPlugin> getUserPluginClass() {
        return ChatStatisticsUserPlugin.class;
    }

    @Override
    public void onActivate() {
        ServiceRegistry.register(service = new ChatStatisticsService());
        globalEmotes = kraken().emoticons().getEmoticonImages("0").getEmoticonSets();
    }
}
