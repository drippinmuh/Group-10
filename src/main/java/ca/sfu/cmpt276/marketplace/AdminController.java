package ca.sfu.cmpt276.marketplace;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin")
    public String dashboard() {
        return "admin";
    }
}
