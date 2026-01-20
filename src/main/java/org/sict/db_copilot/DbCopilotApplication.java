package org.sict.db_copilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class DbCopilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbCopilotApplication.class, args);
    }

}
