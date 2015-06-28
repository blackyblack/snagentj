package snagentj.echodemo;

import java.util.Arrays;

import org.json.simple.JSONObject;

import nrs.util.Convert;
import snagentj.AgentInfo;
import snagentj.AgentInterface;

public class Application
{
  public static class Echodemo extends AgentInterface
  {
    public String name = "echodemo";
    public String[] methods = {"echo"};
    public String[] pubmethods = {"echo"};
    public String[] authmethods = {"echo"};
  }
  
  public static void main(String[] args)
  {
    System.out.println("Hello from echodemo");
    System.out.println("The numbers are " + Arrays.toString(args));
    
    Echodemo a = new Echodemo();
    snagentj.Framework.init(a, args, System.out);
  }
  
  //initialize from JSON object. process JSON, return JSON
  @SuppressWarnings("unchecked")
  public JSONObject processRegister(AgentInfo info, JSONObject data)
  {
    JSONObject answer = new JSONObject();
    info.allowremote = true;
    answer.put("result", "echodemo init");
    return answer;
  }
  
  //process JSON, return JSON
  @SuppressWarnings("unchecked")
  public JSONObject process(AgentInfo info, JSONObject data)
  {
    JSONObject answer = new JSONObject();
    
    String method = Convert.emptyToNull((String)data.get("method"));
    String methodResult = Convert.emptyToNull((String)data.get("result"));
    //plugin specific
    String echoStr = Convert.emptyToNull((String)data.get("echo"));
    
    if (method == null)
    {
      System.out.println("request (" + data.toString() + ") has not method");
      return null;
    }
    
    String errorStr = Convert.emptyToNull((String)data.get("error"));
    String resultStr = Convert.emptyToNull((String)data.get("result"));
    
    if (errorStr != null || resultStr != null)
    {
      answer.put("result", "completed");
      answer.put("allowremote", info.allowremote? 1:0);
      answer.put("tag", "0");
      answer.put("NXT", "");
      return answer;
    }
    
    if (methodResult != null && methodResult.equals("registered"))
    {
      info.registered = true;
      answer.put("result", "activated");
      answer.put("allowremote", info.allowremote? 1:0);
      return answer;
    }
    else if (method.equals("echo"))
    {
      answer.put("result", echoStr);
      answer.put("allowremote", info.allowremote? 1:0);
      return answer;
    }
    
    return null;
  }
}
