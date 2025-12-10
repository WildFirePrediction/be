package com.capstone25.WildFirePrediction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyMessagesResult {
    private List<EmergencyMessageResponse> messages;
    private int page;
    private int size;
    private long totalCount;
    private boolean hasMore;
}
