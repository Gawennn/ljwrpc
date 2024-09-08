package com.ljw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 刘家雯
 * @version 1.0
 */
@SpringBootApplication
@RestController
public class ApplicationProvider {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationProvider.class, args);
    }

    @GetMapping("test")
    public String Hello(){
        return "hello provider";
    }
}
