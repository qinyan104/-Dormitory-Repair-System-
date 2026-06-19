package com.example.dormitoryrepair.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {

    private String type;
    private String title;
    private String content;
    private Long orderId;
    private Long timestamp;
}
