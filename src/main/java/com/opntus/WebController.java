package com.opntus;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@Controller
public class WebController {
 @GetMapping("/") public String home(){return "forward:/index.html";}
 @GetMapping("/api/csrf") @ResponseBody public Map<String,String> csrf(HttpServletRequest req){var t=(CsrfToken)req.getAttribute(CsrfToken.class.getName());return Map.of("token",t.getToken(),"headerName",t.getHeaderName());}
 @GetMapping("/healthz") @ResponseBody public Map<String,Boolean> health(){return Map.of("ok",true);}
}
