package org.mttk.msgcenter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("org.mttk.msgcenter.mapper")
@EnableScheduling
public class MsgcenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsgcenterApplication.class, args);
    }

}
