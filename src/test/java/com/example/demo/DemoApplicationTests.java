package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.dto.MessagePageResponse;
import com.example.demo.service.MessageService;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootTest
class DemoApplicationTests {

	static {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		dotenv.entries().forEach(entry -> {
			if (System.getProperty(entry.getKey()) == null) {
				System.setProperty(entry.getKey(), entry.getValue());
			}
		});
	}

	@Autowired
	private MessageService messageService;

	@Test
	void contextLoads() {
	}

	@Test
	void testGetMessages() {
		MessagePageResponse response = messageService.getMessages(8L, 2L, 15, null);
		System.out.println("=== testGetMessages Response: " + response);
	}

}
