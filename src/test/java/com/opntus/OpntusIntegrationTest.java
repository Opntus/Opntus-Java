package com.opntus;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:sqlite:file:test?mode=memory&cache=shared","opntus.admin-emails=opntus.1@gmail.com"})
@AutoConfigureMockMvc
class OpntusIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 RequestPostProcessor person(String name,String email){return authentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(new OpntusService.Person("supabase:"+name,email,name),null,List.of()));}
 org.springframework.test.web.servlet.ResultActions action(String name,String email,Map<String,Object>body)throws Exception{return mvc.perform(post("/api/opntus").with(person(name,email)).with(csrf()).contentType("application/json").content(json.writeValueAsString(body)));}
 void profile(String name)throws Exception{action(name,name+"@example.test",Map.of("action","profile","name",name,"languages","Tamil","consent",true)).andExpect(status().isOk());}
 void approve(String name,List<String>modes)throws Exception{action("admin","opntus.1@gmail.com",Map.of("action","moderate","target","supabase:"+name,"status","approved","modes",modes)).andExpect(status().isOk());}
 String request(String name,String mode)throws Exception{var response=action(name,name+"@example.test",Map.of("action","request","mode",mode)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(response).get("sessions").get(0).get("id").asText();}
 @Test void authenticationAndCsrfAreRequired()throws Exception{
  mvc.perform(get("/api/opntus")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/opntus").with(person("speaker","s@example.test")).contentType("application/json").content("{\"action\":\"request\",\"mode\":\"chat\"}")).andExpect(status().isForbidden());
  mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"a@example.test\",\"password\":\"password123\"}")).andExpect(status().isServiceUnavailable());
  mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
 }
 @Test void fullConversationWithPermissionsAndReviews()throws Exception{
  mvc.perform(get("/api/opntus").with(person("old","vikashpalanisamy005@gmail.com"))).andExpect(status().isOk()).andExpect(jsonPath("$.me.role").value("speaker"));
  profile("speaker");profile("listener");profile("listener2");
  action("speaker","speaker@example.test",Map.of("action","moderate","target","supabase:listener","status","approved","modes",List.of("chat"))).andExpect(status().isForbidden());
  action("listener","listener@example.test",Map.of("action","apply","bio","I want to listen respectfully and help people feel heard.")).andExpect(status().isOk());
  action("listener","listener@example.test",Map.of("action","availability","available",true)).andExpect(status().isForbidden());
  approve("listener",List.of("chat"));approve("listener2",List.of("chat"));
  action("listener","listener@example.test",Map.of("action","availability","available",true)).andExpect(status().isOk());
  action("listener2","listener2@example.test",Map.of("action","availability","available",true)).andExpect(status().isOk());
  String audio=request("speaker","audio");
  action("listener","listener@example.test",Map.of("action","accept","id",audio)).andExpect(status().isForbidden());
  action("speaker","speaker@example.test",Map.of("action","cancel","id",audio)).andExpect(status().isOk());
  String chat=request("speaker","chat");
  action("listener","listener@example.test",Map.of("action","accept","id",chat)).andExpect(status().isOk());
  action("listener2","listener2@example.test",Map.of("action","accept","id",chat)).andExpect(status().isConflict());
  action("intruder","intruder@example.test",Map.of("action","message","id",chat,"body","Unauthorized")).andExpect(status().isForbidden());
  action("speaker","speaker@example.test",Map.of("action","message","id",chat,"body","Hello")).andExpect(status().isOk()).andExpect(jsonPath("$.messages[0].body").value("Hello"));
  action("speaker","speaker@example.test",Map.of("action","end","id",chat)).andExpect(status().isOk());
  action("speaker","speaker@example.test",Map.of("action","message","id",chat,"body","Ended")).andExpect(status().isConflict());
  action("speaker","speaker@example.test",Map.of("action","rate","id",chat,"score",5,"appScore",4,"review","Helpful")).andExpect(status().isOk());
  action("speaker","speaker@example.test",Map.of("action","rate","id",chat,"score",1,"appScore",1,"review","Duplicate")).andExpect(status().isOk());
  action("admin","opntus.1@gmail.com",Map.of("action","profile","name","Admin","consent",true)).andExpect(status().isOk()).andExpect(jsonPath("$.admin.ratings.length()").value(1)).andExpect(jsonPath("$.admin.ratings[0].score").value(5));
 }
}
