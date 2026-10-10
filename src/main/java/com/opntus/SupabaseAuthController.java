package com.opntus;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
@RestController
public class SupabaseAuthController {
 private final String url,key; private final ObjectMapper json;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
 public SupabaseAuthController(@Value("${opntus.supabase.url}") String url,@Value("${opntus.supabase.key}") String key,ObjectMapper json){this.url=url.replaceAll("/+$","");this.key=key;this.json=json;}
 private JsonNode call(String path,String token,Map<String,String> body){
  if(url.isBlank()||key.isBlank())throw new ApiException(503,"Email login is not configured. Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY on the server.");
  try{
   var b=HttpRequest.newBuilder(URI.create(url+"/auth/v1/"+path)).timeout(Duration.ofSeconds(15)).header("apikey",key).header("Content-Type","application/json");
   if(token!=null)b.header("Authorization","Bearer "+token);
   if(body==null)b.GET();else b.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
   var response=client.send(b.build(),HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()>=400)throw new ApiException(response.statusCode()==429?429:400,response.statusCode()==429?"Too many attempts. Please try again later.":"Authentication failed. Check your credentials and confirm your email, or try again later.");
   return json.readTree(response.body());
  }catch(ApiException e){throw e;}catch(Exception e){throw new ApiException(502,"Authentication service is unavailable. Please try again.");}
 }
 private Map<String,String> credentials(Map<String,String> body){
  String email=Objects.toString(body.get("email"),"").trim(),password=Objects.toString(body.get("password"),"");
  if(email.length()>254||!email.contains("@")||password.length()<8||password.length()>256)throw new ApiException(400,"Enter a valid email and a password of 8–256 characters.");
  return Map.of("email",email,"password",password);
 }
 @PostMapping("/auth/signup") public ResponseEntity<?> signup(@RequestBody Map<String,String> body){call("signup",null,credentials(body));return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("message","Check your email for a confirmation link, then return here to sign in. If you already have an account, sign in."));}
 @PostMapping("/auth/login") public ResponseEntity<?> login(@RequestBody Map<String,String> body,HttpServletRequest request,HttpServletResponse response){
  var result=call("token?grant_type=password",null,credentials(body));
  String token=result.path("access_token").asText();if(token.isBlank())throw new ApiException(401,"Sign-in failed.");
  var user=call("user",token,null);
  if(user.path("email_confirmed_at").asText("").isBlank()||user.path("id").asText().isBlank()||user.path("email").asText().isBlank())throw new ApiException(403,"Confirm your email before signing in.");
  var principal=new OpntusService.Person("supabase:"+user.path("id").asText(),user.path("email").asText(),user.path("user_metadata").path("name").asText("Friend"));
  request.getSession();request.changeSessionId();
  // Cap the local login session at the Supabase token lifetime. No tokens are returned to the browser.
  request.getSession().setAttribute("AUTH_EXPIRES",System.currentTimeMillis()+Math.min(3600,Math.max(1,result.path("expires_in").asLong(3600)))*1000);
  var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,List.of()));SecurityContextHolder.setContext(context);
  new HttpSessionSecurityContextRepository().saveContext(context,request,response);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("ok",true));
 }
}
