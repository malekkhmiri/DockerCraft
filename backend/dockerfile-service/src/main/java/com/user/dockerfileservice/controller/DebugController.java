package com.user.dockerfileservice.controller;

import com.user.dockerfileservice.service.impl.DockerfileServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/logs")
    public List<String> getLogs() {
        return DockerfileServiceImpl.getDebugLogs();
    }
}
