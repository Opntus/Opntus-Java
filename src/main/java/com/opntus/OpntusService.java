package com.opntus;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class OpntusService {
 public record Person(String id,String email,String name){}
 private final JdbcTemplate db;
 private final Set<String> admins;
 public OpntusService(JdbcTemplate db,@Value("${opntus.admin-emails}") String allowlist){this.db=db;this.admins=new HashSet<>();for(String e:allowlist.split(","))if(!e.isBlank())admins.add(e.trim().toLowerCase(Locale.ROOT));}
 private String uuid(){return UUID.randomUUID().toString();}
 private Map<String,Object> one(String sql,Object...args){var rows=db.queryForList(sql,args);return rows.isEmpty()?null:rows.get(0);}
 private List<Map<String,Object>> all(String sql,Object...args){return db.queryForList(sql,args);}
 private int run(String sql,Object...args){return db.update(sql,args);}
 private static String text(Object o,int max){if(!(o instanceof String s))return "";s=s.trim();return s.substring(0,Math.min(max,s.length()));}
 private static int integer(Object o){return o instanceof Number n?n.intValue():0;}
 private static boolean truth(Object o){return Boolean.TRUE.equals(o);}
 private static void require(boolean condition,int code,String message){if(!condition)throw new ApiException(code,message);}
 private Map<String,Object> member(Person p){String role=admins.contains(p.email().toLowerCase(Locale.ROOT))?"admin":"speaker";run("INSERT OR IGNORE INTO users(id,name,role) VALUES(?,?,?)",p.id(),p.name(),role);run("UPDATE users SET role=?,seen=? WHERE id=?",role,System.currentTimeMillis(),p.id());return one("SELECT * FROM users WHERE id=?",p.id());}
 @Transactional public synchronized Map<String,Object> state(Person p){return snapshot(p,member(p));}
 @Transactional public synchronized Map<String,Object> action(Person p,Map<String,Object>b){
  var me=member(p);String id=p.id(),action=text(b.get("action"),40),sid=text(b.get("id"),100);long now=System.currentTimeMillis();
  switch(action){
   case "profile" -> {require(truth(b.get("consent")),400,"Please accept the terms and community guidelines.");String name=text(b.get("name"),60),lang=text(b.get("languages"),100);run("UPDATE users SET name=?,languages=?,consent=1 WHERE id=?",name.isBlank()?me.get("name"):name,lang.isBlank()?"English":lang,id);}
   case "apply" -> {require(integer(me.get("consent"))==1,400,"Complete your profile and consent first.");require(!Set.of("approved","suspended").contains(me.get("status")),409,"This application cannot be resubmitted.");String bio=text(b.get("bio"),1000);require(bio.length()>=30,400,"Please write at least 30 characters about why you want to listen.");run("UPDATE users SET status='pending',bio=?,available=0 WHERE id=?",bio,id);}
   case "availability" -> {require("approved".equals(me.get("status")),403,"Admin approval is required.");run("UPDATE users SET available=? WHERE id=?",truth(b.get("available"))?1:0,id);}
   case "request" -> {require(integer(me.get("consent"))==1,403,"Please accept the terms first.");String mode=text(b.get("mode"),10);require(Set.of("chat","audio","video").contains(mode),400,"Select a conversation mode.");int changes=run("INSERT INTO sessions(id,speaker,mode,created) SELECT ?,?,?,? WHERE NOT EXISTS(SELECT 1 FROM sessions WHERE (speaker=? OR listener=?) AND status IN ('waiting','active'))",uuid(),id,mode,now,id,id);require(changes==1,409,"You already have an open conversation.");}
   case "accept" -> {require("approved".equals(me.get("status"))&&integer(me.get("available"))==1,403,"You must be an approved, available Listener.");var s=one("SELECT * FROM sessions WHERE id=?",sid);require(s!=null,404,"Request not found.");require(Arrays.asList(((String)me.get("modes")).split(",")).contains(s.get("mode"))&&!id.equals(s.get("speaker")),403,"Request not eligible.");int changes=run("UPDATE sessions SET listener=?,status='active' WHERE id=? AND status='waiting' AND NOT EXISTS(SELECT 1 FROM sessions WHERE (speaker=? OR listener=?) AND status IN ('waiting','active')) AND NOT EXISTS(SELECT 1 FROM blocks WHERE (owner=? AND target=?) OR (owner=? AND target=?))",id,sid,id,id,id,s.get("speaker"),s.get("speaker"),id);require(changes==1,409,"Another Listener accepted, or you are already busy.");}
   case "message","signal","end","cancel","rate","report","block" -> {
    var s=one("SELECT * FROM sessions WHERE id=?",sid);require(s!=null&&(id.equals(s.get("speaker"))||id.equals(s.get("listener"))),403,"Conversation access denied.");
    if(action.equals("message")||action.equals("signal")){require("active".equals(s.get("status")),409,"Conversation has ended.");String content=text(b.get("body"),action.equals("signal")?30000:2000);require(!content.isEmpty(),400,"Message is empty.");var recent=one("SELECT COUNT(*) n FROM messages WHERE sender=? AND created>?",id,now-10000);require(integer(recent.get("n"))<=30,429,"Please slow down.");run("INSERT INTO messages(id,session,sender,body,kind,created) VALUES(?,?,?,?,?,?)",uuid(),sid,id,content,action.equals("signal")?"signal":"chat",now);}
    if(action.equals("end")||action.equals("cancel"))run("UPDATE sessions SET status=?,ended=? WHERE id=? AND status IN ('waiting','active')","waiting".equals(s.get("status"))?"cancelled":"completed",now,sid);
    if(action.equals("rate")){require(id.equals(s.get("speaker"))&&"completed".equals(s.get("status"))&&s.get("listener")!=null,403,"Only the Speaker can rate a completed conversation.");int score=integer(b.get("score")),appScore=integer(b.get("appScore"));require(score>=1&&score<=5&&appScore>=1&&appScore<=5,400,"Choose both ratings.");run("INSERT OR IGNORE INTO ratings(id,session,speaker,listener,score,app_score,review,created) VALUES(?,?,?,?,?,?,?,?)",uuid(),sid,id,s.get("listener"),score,appScore,text(b.get("review"),1000),now);}
    if(action.equals("report")){String reason=text(b.get("reason"),1000);require(reason.length()>=5,400,"Please describe the issue.");run("INSERT INTO reports(id,session,reporter,reason,created) VALUES(?,?,?,?,?)",uuid(),sid,id,reason,now);}
    if(action.equals("block")){Object target=id.equals(s.get("speaker"))?s.get("listener"):s.get("speaker");require(target!=null,400,"Nobody is assigned yet.");run("INSERT OR IGNORE INTO blocks(id,owner,target) VALUES(?,?,?)",uuid(),id,target);run("UPDATE sessions SET status='completed',ended=? WHERE id=? AND status='active'",now,sid);}
   }
   case "moderate" -> {require("admin".equals(me.get("role")),403,"Admin access required.");String status=text(b.get("status"),20),target=text(b.get("target"),100);require(Set.of("approved","rejected","suspended").contains(status),400,"Invalid status.");require(one("SELECT id FROM users WHERE id=?",target)!=null,404,"User not found.");List<String> modes=new ArrayList<>();if(b.get("modes") instanceof List<?> list)for(Object m:list)if(m instanceof String mode&&Set.of("chat","audio","video").contains(mode)&&!modes.contains(mode))modes.add(mode);run("UPDATE users SET status=?,modes=?,available=0 WHERE id=?",status,String.join(",",modes),target);run("INSERT INTO audits(id,actor,action,created) VALUES(?,?,?,?)",uuid(),id,status+": "+target+"; modes: "+String.join(",",modes),now);if(!status.equals("approved"))run("UPDATE sessions SET status=CASE WHEN status='waiting' THEN 'cancelled' ELSE 'completed' END,ended=? WHERE (speaker=? OR listener=?) AND status IN ('waiting','active')",now,target,target);}
   case "resolve" -> {require("admin".equals(me.get("role")),403,"Admin access required.");run("UPDATE reports SET status='resolved' WHERE id=?",sid);}
   default -> throw new ApiException(400,"Unknown action.");
  }
  return snapshot(p,one("SELECT * FROM users WHERE id=?",id));
 }
 private Map<String,Object> snapshot(Person p,Map<String,Object> me){
  String id=p.id();long now=System.currentTimeMillis();
  var sessions=all("SELECT s.*,a.name speaker_name,b.name listener_name,r.score rated FROM sessions s LEFT JOIN users a ON a.id=s.speaker LEFT JOIN users b ON b.id=s.listener LEFT JOIN ratings r ON r.session=s.id WHERE s.speaker=? OR s.listener=? ORDER BY created DESC LIMIT 100",id,id);
  var active=sessions.stream().filter(s->Set.of("waiting","active").contains(s.get("status"))).findFirst().orElse(null);
  var messages=active!=null&&"active".equals(active.get("status"))?all("SELECT * FROM messages WHERE session=? ORDER BY created LIMIT 1000",active.get("id")):List.of();
  var listeners=all("SELECT u.id,u.name,u.languages,u.bio,u.modes,u.available,COUNT(r.id) reviews,AVG(r.score) rating FROM users u LEFT JOIN ratings r ON r.listener=u.id WHERE u.status='approved' AND u.available=1 AND u.seen>? AND u.id!=? AND NOT EXISTS(SELECT 1 FROM sessions s WHERE s.listener=u.id AND s.status='active') AND NOT EXISTS(SELECT 1 FROM blocks WHERE (owner=? AND target=u.id) OR (owner=u.id AND target=?)) GROUP BY u.id ORDER BY (COALESCE(AVG(r.score),3.5)*COUNT(r.id)+3.5*5)/(COUNT(r.id)+5.0) DESC",now-30000,id,id,id);
  var incoming="approved".equals(me.get("status"))&&integer(me.get("available"))==1?all("SELECT s.id,s.mode,s.created,u.name speaker_name FROM sessions s JOIN users u ON u.id=s.speaker WHERE s.status='waiting' AND s.speaker!=? AND instr(','||?||',',','||s.mode||',')>0 AND NOT EXISTS(SELECT 1 FROM blocks WHERE (owner=? AND target=s.speaker) OR (owner=s.speaker AND target=?)) ORDER BY s.created",id,me.get("modes"),id,id):List.of();
  Map<String,Object> admin=null;
  if("admin".equals(me.get("role"))){admin=new LinkedHashMap<>();admin.put("users",all("SELECT id,name,role,status,bio,languages,modes,available FROM users"));admin.put("reports",all("SELECT * FROM reports ORDER BY created DESC"));admin.put("audits",all("SELECT * FROM audits ORDER BY created DESC LIMIT 50"));admin.put("counts",one("SELECT COUNT(*) total,SUM(status='active') active,SUM(status='completed') completed FROM sessions"));admin.put("ratings",all("SELECT score,app_score,review,created FROM ratings ORDER BY created DESC LIMIT 50"));}
  Map<String,Object> result=new LinkedHashMap<>();result.put("me",me);result.put("sessions",sessions);result.put("messages",messages);result.put("listeners",listeners);result.put("incoming",incoming);result.put("admin",admin);result.put("adminConfigured",!admins.isEmpty());return result;
 }
}
