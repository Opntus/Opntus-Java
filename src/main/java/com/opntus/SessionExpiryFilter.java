package com.opntus;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
public class SessionExpiryFilter extends OncePerRequestFilter {
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  var session=request.getSession(false);
  if(session!=null && session.getAttribute("AUTH_EXPIRES") instanceof Long expires && expires<=System.currentTimeMillis())session.invalidate();
  chain.doFilter(request,response);
 }
}
