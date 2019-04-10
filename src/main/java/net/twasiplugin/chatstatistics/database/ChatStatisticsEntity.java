package net.twasiplugin.chatstatistics.database;

import net.twasi.core.database.models.BaseEntity;
import net.twasiplugin.dependency.streamtracker.database.StreamEntity;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@Entity(value = "twasi.chatstats.entities", noClassnameStored = true)
public class ChatStatisticsEntity extends BaseEntity {

    @Reference
    private StreamEntity stream;

    private Date timestamp;

    private Map<String, Integer> MessagesByUser;
    private Map<String, Integer> UsedEmotes;

    public ChatStatisticsEntity(StreamEntity stream, Map<String, Integer> messagesByUser, Map<String, Integer> usedEmotes) {
        this.stream = stream;
        this.timestamp = Calendar.getInstance().getTime();
        MessagesByUser = messagesByUser;
        UsedEmotes = usedEmotes;
    }

    public StreamEntity getSession() {
        return stream;
    }

    public Map<String, Integer> getMessagesByUser() {
        return MessagesByUser;
    }

    public Map<String, Integer> getUsedEmotes() {
        return UsedEmotes;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
