package net.twasiplugin.chatstatistics;

import net.twasi.core.database.models.User;
import net.twasi.core.events.TwasiEventHandler;
import net.twasi.core.interfaces.api.TwasiInterface;
import net.twasi.core.models.Message.TwasiMessage;
import net.twasi.core.plugin.api.TwasiUserPlugin;
import net.twasi.core.plugin.api.TwasiVariable;
import net.twasi.core.plugin.api.events.TwasiDisableEvent;
import net.twasi.core.plugin.api.events.TwasiEnableEvent;
import net.twasi.core.plugin.api.events.TwasiMessageEvent;
import net.twasi.core.services.ServiceRegistry;
import net.twasi.core.services.providers.DataService;
import net.twasi.twitchapi.kraken.chat.response.EmoticonImagesDTO;
import net.twasiplugin.chatstatistics.database.ChatStatisticsEntity;
import net.twasiplugin.chatstatistics.database.ChatStatisticsRepository;
import net.twasiplugin.dependency.streamtracker.StreamTrackerService;
import net.twasiplugin.dependency.streamtracker.StreamTrackerService.TwasiStreamTrackEventHandler;
import net.twasiplugin.dependency.streamtracker.database.StreamEntity;
import net.twasiplugin.dependency.streamtracker.events.StreamStartEvent;
import net.twasiplugin.dependency.streamtracker.events.StreamStopEvent;
import net.twasiplugin.dependency.streamtracker.events.StreamTrackEvent;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.stream.Collectors;

import static net.twasi.twitchapi.TwitchAPI.kraken;

public class ChatStatisticsUserPlugin extends TwasiUserPlugin {

    private static HashMap<ObjectId, EmoticonImagesDTO> emoticonCache = new HashMap<>();
    private EmoticonImagesDTO streamerEmotes;

    private Map<String, Integer> EmoteUses;
    private Map<String, Integer> ChatMessagesByUser;

    private StreamEntity currentStream;

    private ChatStatisticsRepository repo;

    private boolean keepTracking = true;

    @Override
    public void onEnable(TwasiEnableEvent e) {
        clear();

        repo = ServiceRegistry.get(DataService.class).get(ChatStatisticsRepository.class);

        User user = getTwasiInterface().getStreamer().getUser();
        ObjectId enablingUserId = user.getId();
        if (emoticonCache.containsKey(enablingUserId)) {
            this.streamerEmotes = emoticonCache.get(enablingUserId);
        } else {
            streamerEmotes = kraken().emoticons().getEmoticonImages(user.getTwitchAccount().getTwitchId());
            emoticonCache.put(enablingUserId, streamerEmotes); // TODO query more than one at once
        }

        TwasiStreamTrackEventHandler trackEvents = new TwasiStreamTrackEventHandler() {
            @Override
            public void on(StreamTrackEvent e) {
                if (!keepTracking) return;
                currentStream = e.getCurrentTrackEntity().getStream();
                repo.add(new ChatStatisticsEntity(currentStream, ChatMessagesByUser, EmoteUses));
                clear();
            }
        };
        TwasiEventHandler<StreamStartEvent> startEvents = new TwasiEventHandler<StreamStartEvent>() {
            @Override
            public void on(StreamStartEvent e) {
                if (!keepTracking) return;
                clear();
            }
        };
        TwasiEventHandler<StreamStopEvent> stopEvents = new TwasiEventHandler<StreamStopEvent>() {
            @Override
            public void on(StreamStopEvent e) {
                if (!keepTracking) return;
                repo.add(new ChatStatisticsEntity(currentStream, ChatMessagesByUser, EmoteUses));
                clear();
            }
        };

        StreamTrackerService service = ServiceRegistry
                .get(StreamTrackerService.class);
        service.registerStreamTrackEvent(user, trackEvents);
        service.registerStreamStartEvent(user, startEvents);
        service.registerStreamStopEvent(user, stopEvents);
    }

    private void clear() {
        EmoteUses = new HashMap<>();
        ChatMessagesByUser = new HashMap<>();
    }

    @Override
    public void onDisable(TwasiDisableEvent e) {
        keepTracking = false;
    }

    private boolean match(String part) {
        return this.streamerEmotes.getEmoticonSets().keySet().stream().anyMatch(part::equals) ||
                ChatStatisticsPlugin.globalEmotes.keySet().stream().anyMatch(part::equals);
    }

    @Override
    public void onMessage(TwasiMessageEvent e) {
        Thread t1 = new Thread(() -> {
            TwasiMessage message = e.getMessage();
            String[] parts = message.getMessage().split(" ");
            List<String> usedEmotes = Arrays.stream(parts).filter(this::match).collect(Collectors.toList());
            usedEmotes.forEach(emote -> {
                int uses = 0;
                if (EmoteUses.containsKey(emote)) uses = EmoteUses.get(emote);
                EmoteUses.put(emote, ++uses);
            });
            String sender = e.getMessage().getSender().getTwitchId();
            int messages = 0;
            if (ChatMessagesByUser.containsKey(sender)) messages = ChatMessagesByUser.get(sender);
            ChatMessagesByUser.put(sender, ++messages);
        });
        t1.setDaemon(true);
        t1.start();
    }

    @Override
    public List<TwasiVariable> getVariables() {
        return Collections.singletonList(new TwasiVariable(this) {
            @Override
            public List<String> getNames() {
                return Collections.singletonList("messages");
            }

            @Override
            public String process(String name, TwasiInterface inf, String[] params, TwasiMessage message) {
                return String.valueOf(repo.getChatMessageAmount(getTwasiInterface().getStreamer().getUser(), message.getSender().getTwitchId()));
            }
        });
    }
}
