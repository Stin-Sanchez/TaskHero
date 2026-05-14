package com.stinjoss.chat.websocket.chat_websocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableAsync
public class ChatWebsocketApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatWebsocketApplication.class, args);
	}

}
