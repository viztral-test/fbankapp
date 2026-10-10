package com.fbankapp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FbankappApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void runsOnJava8() {
        String version = System.getProperty("java.version");
        assertTrue(version.startsWith("1.8"), "Expected Java 8 runtime but was " + version);
    }
}
