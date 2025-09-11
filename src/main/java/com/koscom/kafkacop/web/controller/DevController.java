package com.koscom.kafkacop.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koscom.kafkacop.util.DefaultResponse;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

// @Profile("!prod")
@RestController
@RequiredArgsConstructor
public class DevController {

    @Hidden
    @GetMapping("/hello")
    DefaultResponse hello() {
        return DefaultResponse.ok();
    }
}
