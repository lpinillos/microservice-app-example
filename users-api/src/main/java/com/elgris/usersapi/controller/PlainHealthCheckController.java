package com.elgris.usersapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class PlainHealthCheckController {

    @GetMapping("/healthz")
    public void health(HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.getWriter().write("users_api_health 1\n");
    }
}
