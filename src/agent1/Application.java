package agent1;

import java.nio.charset.Charset;
import java.util.Arrays;

import nanomsg.pair.PairSocket;
import nrs.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Application
{

  @SuppressWarnings("unchecked")
  public static void main(String[] args)
  {
    System.out.println("Hello from agent1");
    System.out.println("The numbers are " + Arrays.toString(args));
    
    //boolean permanentFlag = Convert.parseUnsignedLong(args[0]) == 1L;
    Long daemonId = Convert.parseUnsignedLong(args[1]);
    
    JSONParser parser = new JSONParser();
    JSONObject params = null;
    ///HACK: params come as json array inside quotes - not so easy to parse
    try
    {
      System.out.println("json = " + args[2]);
      String tmp = (String) parser.parse(args[2]);
      JSONArray allParams = (JSONArray) parser.parse(tmp);
      params = (JSONObject) allParams.get(0);
      System.out.println("parsed json = " + params.toString());
    }
    catch (ParseException e)
    {
      e.printStackTrace();
    }
    
    ///HACK: do not forget to put daemonId + 1 in ()
    String bindAddress = "ipc://" + (daemonId + 1);
    String connectAddress = "ipc://" + (daemonId + 2);
    
    nanomsg.Socket x = new PairSocket();
    ///TODO: set timeouts from plugin777
    //x.setSendTimeout(30000);
    //x.setRecvTimeout(30000);
    x.bind(bindAddress + ".pair");
    x.connect(connectAddress + ".pair");
    
    System.out.println("connected to " + connectAddress);
    
    JSONObject registerJson = new JSONObject();
    
    ///TODO: take methods, pubmethods, authmethods from the agent
    JSONArray methodsJson = new JSONArray();
    methodsJson.add("help");
    registerJson.put("methods", methodsJson);
    JSONArray pubmethodsJson = new JSONArray();
    pubmethodsJson.add("help");
    registerJson.put("pubmethods", pubmethodsJson);
    JSONArray authmethodsJson = new JSONArray();
    authmethodsJson.add("help");
    registerJson.put("authmethods", authmethodsJson);
    registerJson.put("pluginrequest", "SuperNET");
    registerJson.put("requestType", "register");
    
    /*if(params != null)
    {
      registerJson.put("tag", params.get("tag"));
      registerJson.put("NXT", params.get("NXT"));
    }*/ 
    
    registerJson.put("NXT", "");
    //what is this?
    registerJson.put("sleepmillis", 0);
    registerJson.put("allowremote", 1);
    registerJson.put("permanentflag", 1);
    ///TODO: random number
    registerJson.put("myid", "12463223069612204128");
    registerJson.put("plugin", "run.sh");
    //registerJson.put("endpoint", bindAddress);
    registerJson.put("millis", 100.0);
    registerJson.put("sent", 0);
    registerJson.put("recv", 0);
    
    ///HACK: json library escapes forward slashes in ipc://
    ///      This is an override...
    String result = registerJson.toString();
    int where = result.length();
    result = result.substring(0, where - 1);
    result += ",\"endpoint\":\"" + bindAddress + "\"}";
    System.out.println("send: " + result);
    
    ///HACK: SuperNet expects trailing zero byte. Append it here.
    final Charset encoding = Charset.forName("ASCII");
    byte[] bytes = result.getBytes(encoding);
    byte[] addBytes = new byte[bytes.length + 1];
    System.arraycopy(bytes, 0, addBytes, 0, bytes.length);
    x.send(addBytes, true);
    System.out.println("send ok");
    
    ///TODO: numsent++
    
    while(true)
    {
      String message = x.recvString();
      System.out.println("recv: " + message);
      ///TODO: numrecv++
      try
      {
        Thread.sleep(10);
      }
      catch (InterruptedException e)
      {
        break;
      }
    }
  }
}
