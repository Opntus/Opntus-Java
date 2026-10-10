package com.opntus;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
class SupabaseAuthTest {
 @Test void verifiedLoginCreatesSessionButUnverifiedAndBadCredentialsFail()throws Exception{
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  var verified=new java.util.concurrent.atomic.AtomicBoolean(true);
  var invalid=new java.util.concurrent.atomic.AtomicBoolean(false);
  server.createContext("/auth/v1/",e->{String body=e.getRequestURI().getPath().endsWith("/user")?"{\"id\":\"user-id\",\"email\":\"opntus.1@gmail.com\",\"email_confirmed_at\":"+(verified.get()?"\"2026-10-10\"":"null")+"}":"{\"access_token\":\"test-token\",\"expires_in\":3600}";byte[] bytes=body.getBytes();e.sendResponseHeaders(invalid.get()?400:200,bytes.length);e.getResponseBody().write(bytes);e.close();});
  server.start();
  try{
   var auth=new SupabaseAuthController("http://127.0.0.1:"+server.getAddress().getPort(),"public-test-key",new ObjectMapper());
   var request=new MockHttpServletRequest();request.getSession();String old=request.getSession().getId();
   auth.login(Map.of("email","opntus.1@gmail.com","password","password123"),request,new MockHttpServletResponse());
   assertNotEquals(old,request.getSession().getId());assertNotNull(request.getSession().getAttribute("AUTH_EXPIRES"));
   var person=(OpntusService.Person)SecurityContextHolder.getContext().getAuthentication().getPrincipal();assertEquals("supabase:user-id",person.id());assertEquals("opntus.1@gmail.com",person.email());
   verified.set(false);assertThrows(ApiException.class,()->auth.login(Map.of("email","opntus.1@gmail.com","password","password123"),new MockHttpServletRequest(),new MockHttpServletResponse()));
   invalid.set(true);assertThrows(ApiException.class,()->auth.login(Map.of("email","opntus.1@gmail.com","password","password123"),new MockHttpServletRequest(),new MockHttpServletResponse()));
  }finally{server.stop(0);SecurityContextHolder.clearContext();}
 }
 @Test void expiryInvalidatesSession()throws Exception{
  var request=new MockHttpServletRequest();request.getSession().setAttribute("AUTH_EXPIRES",0L);
  new SessionExpiryFilter().doFilter(request,new MockHttpServletResponse(),new MockFilterChain());assertNull(request.getSession(false));
 }
}
