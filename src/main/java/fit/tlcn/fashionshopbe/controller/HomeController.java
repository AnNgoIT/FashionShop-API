package fit.tlcn.fashionshopbe.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping("/")
    public String helloWorld(){
        return "Hello World, This is AT Shop!!";
    }
}
