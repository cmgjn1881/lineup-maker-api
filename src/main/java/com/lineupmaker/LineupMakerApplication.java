package com.lineupmaker;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LineupMakerApplication {

	public static void main(String[] args) {

		// 프로젝트 루트에 있는 .env 파일을 찾아 시스템 환경 변수로 로드합니다.
		//Dotenv dotenv = Dotenv.load();
//		dotenv.entries().forEach(entry -> {
//			System.setProperty(entry.getKey(), entry.getValue());
//		});

		SpringApplication.run(LineupMakerApplication.class, args);
	}

}
