package com.example.dormitoryrepair;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class EnvTest {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(EnvTest.class, args);
        Environment env = ctx.getEnvironment();
        System.out.println("TEST_KEY=" + env.getProperty("DEEPSEEK_API_KEY"));
        System.out.println("APP_KEY=" + env.getProperty("app.ai.api-key"));
        System.exit(0);
    }
}
