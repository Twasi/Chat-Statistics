package net.twasiplugin.chatstatistics.database;

import net.twasi.core.database.lib.Repository;
import net.twasi.core.database.models.User;
import net.twasi.core.services.ServiceRegistry;
import net.twasi.core.services.providers.DataService;
import net.twasiplugin.dependency.streamtracker.database.StreamEntity;
import net.twasiplugin.dependency.streamtracker.database.StreamRepository;

import java.util.Date;
import java.util.List;

public class ChatStatisticsRepository extends Repository<ChatStatisticsEntity> {

    public List<ChatStatisticsEntity> getByStream(StreamEntity stream) {
        return store.createQuery(ChatStatisticsEntity.class).field("stream").equal(stream).asList();
    }

    public List<ChatStatisticsEntity> getByLatestStreamOfUser(User user) {
        StreamEntity stream = ServiceRegistry.get(DataService.class).get(StreamRepository.class).getLatestStreamEntityOfUser(user);
        if (stream == null) return null;
        return getByStream(stream);
    }

    public List<ChatStatisticsEntity> getByRange(User user, Date from, Date to) {
        if (from.getTime() < to.getTime()) {
            long temp = from.getTime();
            from.setTime(to.getTime());
            to.setTime(temp);
        }
        return store.createQuery(ChatStatisticsEntity.class)
                .field("stream.user").equal(user)
                .field("timestamp").greaterThanOrEq(to)
                .field("timestamp").lessThanOrEq(from).asList();
    }

}
