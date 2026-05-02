package org.mttk.msgcenter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.mttk.msgcenter.mapper")
public class MsgcenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsgcenterApplication.class, args);
    }

}
