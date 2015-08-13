package snagentj.echodemo;

import java.util.Arrays;

import nrs.util.Convert;

import org.json.simple.JSONObject;

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
      
      String errorStr = Convert.emptyToNull((String)data.get("error"));
      String resultStr = Convert.emptyToNull((String)data.get("result"));
      
      if (resultStr != null && resultStr.equals("registered"))
      {
        info.registered = true;
        answer.put("result", "activated");
        return answer;
      }
      if (errorStr != null || resultStr != null)
      {
        answer.put("result", "completed");
        return answer;
      }
      
      String method = Convert.emptyToNull((String)data.get("method"));
      //plugin specific
      String echoStr = Convert.emptyToNull((String)data.get("echostr"));
      
      if (method == null)
      {
        System.out.println("request (" + data.toString() + ") has not method");
        return null;
      }
      
      if (method.equals("echo"))
      {
        answer.put("result", echoStr);
        return answer;
      }
      
      return null;
    }
    
    ///HACK: unfortunately this code must reside here. It cannot be abstracted out.
    public String getName() { return name; }
    public String[] getMethods() { return methods; }
    public String[] getPubmethods() { return pubmethods; }
    public String[] getAuthmethods() { return authmethods; }
  }
  
  public static void main(String[] args)
  {
    System.out.println("echodemo agent starting");
    System.out.println("Args are: " + Arrays.toString(args));
    
    Echodemo a = new Echodemo();
    snagentj.Framework.init(a, args, System.out);
  }
}
