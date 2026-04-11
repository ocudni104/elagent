package ng.elagent.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {


    @GetMapping("/hello")
    public String hello() {
        return "Hello from App Service!";
    }
}
