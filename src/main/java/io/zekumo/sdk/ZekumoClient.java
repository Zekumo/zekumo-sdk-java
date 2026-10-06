package io.zekumo.sdk;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.net.*; import java.net.http.*; import java.nio.charset.StandardCharsets;
import java.time.Duration; import java.util.*; import java.util.concurrent.*;

public final class ZekumoClient implements AutoCloseable {
  public final String appId, baseUrl; private volatile String token; final HttpClient http; final ObjectMapper json;
  public ZekumoClient(String appId,String baseUrl){this(appId,baseUrl,"");}
  public ZekumoClient(String appId,String baseUrl,String token){this.appId=appId;this.baseUrl=baseUrl.replaceAll("/+$","");this.token=token;this.http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();this.json=new ObjectMapper();}
  public String token(){return token;} public void token(String value){token=value==null?"":value;} public void logout(){token="";}
  static String seg(Object s){return URLEncoder.encode(String.valueOf(s),StandardCharsets.UTF_8).replace("+","%20");}
  static String query(Map<String,?> q){var j=new StringJoiner("&");q.forEach((k,v)->{if(v!=null&&!v.toString().isEmpty()&&!v.toString().equals("0"))j.add(seg(k)+"="+seg(v));});return j.length()==0?"":"?"+j;}
  public CompletableFuture<JsonNode> request(String method,String path,Object body,boolean auth){
    try { var b=HttpRequest.newBuilder(URI.create(baseUrl+path)).timeout(Duration.ofSeconds(30)).header("Accept","application/json");
      if(auth&&!token.isEmpty())b.header("Authorization","Bearer "+token); String raw=body==null?"":json.writeValueAsString(body);
      if(body!=null)b.header("Content-Type","application/json"); b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(raw));
      return http.sendAsync(b.build(),HttpResponse.BodyHandlers.ofString()).thenApply(r->{JsonNode out=null;try{if(!r.body().isEmpty())out=json.readTree(r.body());}catch(Exception ignored){}
        if(r.statusCode()>=300){var e=out==null?null:out.path("error");throw new ZekumoException(r.statusCode(),e==null||e.path("code").isMissingNode()?"http_"+r.statusCode():e.path("code").asText(),e==null||e.path("message").isMissingNode()?r.body():e.path("message").asText());}return out;});
    } catch(Exception e){return CompletableFuture.failedFuture(e);}
  }
  public CompletableFuture<JsonNode> get(String p){return request("GET",p,null,true);} public CompletableFuture<JsonNode> post(String p,Object b){return request("POST",p,b,true);} public CompletableFuture<JsonNode> put(String p,Object b){return request("PUT",p,b,true);} public CompletableFuture<JsonNode> delete(String p){return request("DELETE",p,null,true);}
  private CompletableFuture<JsonNode> login(String p,Map<String,Object> b){b.put("app_id",appId);return request("POST",p,b,false).thenApply(x->{token=x.path("token").asText();return x;});}
  public CompletableFuture<JsonNode> loginGuest(String id,String nickname){return login("/v1/auth/login",map("provider","guest","device_id",id,"nickname",nickname));}
  public CompletableFuture<JsonNode> loginPassword(String u,String p){return login("/v1/auth/login",map("provider","password","username",u,"password",p));}
  public CompletableFuture<JsonNode> loginTicket(String t){return login("/v1/auth/login",map("provider","sso","ticket",t));}
  public CompletableFuture<JsonNode> register(String u,String p,String n){return login("/v1/auth/register",map("provider","password","username",u,"password",p,"nickname",n));}
  public CompletableFuture<JsonNode> player(){return get("/v1/player/profile");} public CompletableFuture<JsonNode> updatePlayer(Object n,Object profile){return put("/v1/player/profile",map("nickname",n,"profile",profile));}
  public CompletableFuture<JsonNode> bind(String t,String u,String p){return post("/v1/player/bind",map("ticket",t,"username",u,"password",p));}
  public CompletableFuture<JsonNode> dataList(){return get("/v1/player/data");} public CompletableFuture<JsonNode> dataGet(String k){return get("/v1/player/data/"+seg(k));} public CompletableFuture<JsonNode> dataSet(String k,Object v){return put("/v1/player/data/"+seg(k),map("value",v));} public CompletableFuture<JsonNode> dataDelete(String k){return delete("/v1/player/data/"+seg(k));}
  public CompletableFuture<JsonNode> submit(String b,long s,String m){return post("/v1/leaderboards/"+seg(b)+"/score",map("score",s,"mode",m));} public CompletableFuture<JsonNode> top(String b,int l,int o,String s){return get("/v1/leaderboards/"+seg(b)+query(map("limit",l,"offset",o,"scope",s)));} public CompletableFuture<JsonNode> rank(String b){return get("/v1/leaderboards/"+seg(b)+"/me");}
  public CompletableFuture<JsonNode> achievements(boolean unlocked){return get("/v1/achievements"+(unlocked?"/unlocked":""));}
  public CompletableFuture<JsonNode> friends(){return get("/v1/friends");} public CompletableFuture<JsonNode> friendRequests(){return get("/v1/friends/requests");} public CompletableFuture<JsonNode> friendAction(String action,String id){return post("/v1/friends/"+action,map(action.equals("request")||action.equals("block")?"target_player_id":"request_id",id));} public CompletableFuture<JsonNode> removeFriend(String id){return delete("/v1/friends/"+seg(id));}
  public CompletableFuture<JsonNode> balances(){return get("/v1/currency");} public CompletableFuture<JsonNode> ledger(String id,int l,int o){return get("/v1/currency/"+seg(id)+"/ledger"+query(map("limit",l,"offset",o)));} public CompletableFuture<JsonNode> spend(String id,long amount,String key,String note){return post("/v1/currency/"+seg(id)+"/spend",map("amount",amount,"idempotency_key",key,"note",note));}
  public CompletableFuture<JsonNode> mailbox(boolean unread){return get("/v1/mailbox"+(unread?"?unread_only=true":""));} public CompletableFuture<JsonNode> mailAction(String id,String action){return post("/v1/mailbox/"+seg(id)+"/"+action,null);}
  public CompletableFuture<JsonNode> announcements(Map<String,?> q){return request("GET","/v1/apps/"+seg(appId)+"/announcements"+query(q),null,false);} public CompletableFuture<JsonNode> call(String name,Object body,boolean pub){return request("POST",pub?"/v1/apps/"+seg(appId)+"/functions/"+seg(name):"/v1/functions/"+seg(name),body,!pub);}
  public CompletableFuture<JsonNode> updates(String version,Map<String,?> q){var x=new HashMap<String,Object>(q);x.put("version",version);return request("GET","/v1/apps/"+seg(appId)+"/updates/check"+query(x),null,false);} public CompletableFuture<JsonNode> releases(){return request("GET","/v1/apps/"+seg(appId)+"/releases",null,false);}
  public CompletableFuture<JsonNode> reportLog(Object log){return post("/v1/logs",log);} public CompletableFuture<JsonNode> dialogues(){return get("/v1/dialogues");} public CompletableFuture<JsonNode> dialogue(String k){return get("/v1/dialogues/"+seg(k));} public CompletableFuture<JsonNode> chatHistory(String c,long before,int limit){return get("/v1/chat/history"+query(map("channel",c,"before_id",before,"limit",limit)));} public CompletableFuture<JsonNode> kvGet(String n,String k){return get("/v1/kv/"+seg(n)+"/"+seg(k));} public CompletableFuture<JsonNode> kvList(String n,int l,int o){return get("/v1/kv/"+seg(n)+query(map("limit",l,"offset",o)));}
  public ZekumoRealtime realtime(){return new ZekumoRealtime(this);} public void close(){}
  static Map<String,Object> map(Object... xs){var m=new LinkedHashMap<String,Object>();for(int i=0;i<xs.length;i+=2)if(xs[i+1]!=null)m.put((String)xs[i],xs[i+1]);return m;}
}
