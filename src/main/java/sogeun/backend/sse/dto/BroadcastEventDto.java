package sogeun.backend.sse.dto;

import java.util.UUID;

public class BroadcastEventDto {

    private String type;
    private Long senderId;

    private MusicDto music;

    private Integer likeCount;    // 좋아요 수
    private Integer radiusMeter;  // 반경

    private long ts;
    private String eventId;

    private BroadcastEventDto(
            String type,
            Long senderId,
            MusicDto music,
            Integer likeCount,
            Integer radiusMeter
    ) {
        this.type = type;
        this.senderId = senderId;
        this.music = music;
        this.likeCount = likeCount;
        this.radiusMeter = radiusMeter;
        this.ts = System.currentTimeMillis();
        this.eventId = UUID.randomUUID().toString();
    }

    public static BroadcastEventDto on(Long senderId, MusicDto music) {
        return new BroadcastEventDto(
                "BROADCAST_ON",
                senderId,
                music,
                null,
                null
        );
    }

    public static BroadcastEventDto off(Long senderId) {
        return new BroadcastEventDto(
                "BROADCAST_OFF",
                senderId,
                null,
                null,
                null
        );
    }

    //좋아요 변경 이벵
    public static BroadcastEventDto likeUpdated(
            Long senderId,
            int likeCount,
            int radiusMeter
    ) {
        return new BroadcastEventDto(
                "BROADCAST_LIKE",
                senderId,
                null,
                likeCount,
                radiusMeter
        );
    }
}
