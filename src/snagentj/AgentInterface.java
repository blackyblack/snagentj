package snagentj;

import org.json.simple.JSONObject;

public class AgentInterface
{
  //name of the agent. Must be equal to executable name.
  public String name = "";
  //list of supported methods approved for local access
  public String[] methods = new String[0];
  //list of supported methods approved for public (Internet) access
  public String[] pubmethods = new String[0];
  //list of supported methods that require authentication
  public String[] authmethods = new String[0];
  
  //return disabled flags here
  public long register(AgentInfo info, JSONObject args)
  {
    // set bits corresponding to array position in methods[]
    return 0;
  }
  
  //initialize from JSON object. process JSON, return JSON
  public JSONObject processRegister(AgentInfo info, JSONObject data)
  {
    return null;
  }
  
  //called while idle
  public int idle(AgentInfo info)
  {
    return 0;
  }
  
  //process JSON, return JSON
  public JSONObject process(AgentInfo info, JSONObject data)
  {
    return null;
  }
  
  public int shutdown(AgentInfo info, int retcode)
  {
    return 0;
  }
}
