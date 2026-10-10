package com.opntus;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
public class ApiException extends RuntimeException {
 final int status;
 public ApiException(int status,String message){super(message);this.status=status;}
}
@RestControllerAdvice
class ApiErrors {
 @ExceptionHandler(ApiException.class) ResponseEntity<?> handle(ApiException e){return ResponseEntity.status(e.status).body(Map.of("error",e.getMessage()));}
 @ExceptionHandler(org.springframework.dao.DataAccessException.class) ResponseEntity<?> database(RuntimeException e){return ResponseEntity.status(500).body(Map.of("error","Unable to complete this action. Please try again."));}
}
