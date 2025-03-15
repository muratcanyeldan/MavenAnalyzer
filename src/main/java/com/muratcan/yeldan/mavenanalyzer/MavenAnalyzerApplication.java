package com.muratcan.yeldan.mavenanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MavenAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MavenAnalyzerApplication.class, args);
    }

}
