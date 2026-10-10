package com.opntus;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
@RestController
public class OpntusController {
 private final OpntusService service;
 public OpntusController(OpntusService service){this.service=service;}
 @GetMapping("/api/opntus") public ResponseEntity<?> get(@AuthenticationPrincipal OpntusService.Person p){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.state(p));}
 @PostMapping("/api/opntus") public ResponseEntity<?> post(@AuthenticationPrincipal OpntusService.Person p,@RequestBody Map<String,Object> b){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.action(p,b));}
}
