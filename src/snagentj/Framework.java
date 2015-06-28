package snagentj;

import java.io.PrintStream;
import java.nio.charset.Charset;

import nanomsg.pair.PairSocket;
import nrs.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Framework
{
  public static PrintStream logger;
  public static nanomsg.Socket comm;
  //all agent parameters
  public static AgentInfo info = new AgentInfo();
  
  public static AgentInterface agent = null;
  
  public static void reply(String data)
  {
    logger.println("send: " + data);
    
    ///HACK: SuperNet expects trailing zero byte. Append it here.
    final Charset encoding = Charset.forName("ASCII");
    byte[] bytes = data.getBytes(encoding);
    byte[] addBytes = new byte[bytes.length + 1];
    System.arraycopy(bytes, 0, addBytes, 0, bytes.length);
    comm.send(addBytes, true);
    info.numsent++;
  }
  
  public static void init(AgentInterface agent, String[] args, PrintStream logger)
  {
    Framework.logger = logger;
    Framework.agent = agent;
    ///HACK: maybe switch logger off if passed null
    if(logger == null) logger = System.out;
    mainLoop(args);
  }
  
  @SuppressWarnings("unchecked")
  public static void mainLoop(String[] args)
  {    
    try
    {
      info.permanentflag = Convert.parseUnsignedLong(args[0]) == 1L;
      info.daemonid = Convert.parseUnsignedLong(args[1]);
      ///TODO: get process id
      info.ppid = 0L;
      info.name = agent.name;
      info.numsent = 0L;
      info.numrecv = 0L;
      
      JSONParser parser = new JSONParser();
      
      ///HACK: params come as json array inside quotes - not so easy to parse
      String tmp = (String) parser.parse(args[2]);
      JSONArray allParams = (JSONArray) parser.parse(tmp);
      JSONObject params = (JSONObject) allParams.get(0);
      
      String nxtbits = Convert.emptyToNull((String) params.get("NXT"));
      if(nxtbits != null)
      {
        info.nxt64bits = Convert.parseUnsignedLong(nxtbits);
      }
      info.ipaddr = Convert.emptyToNull((String) params.get("ipaddr"));
      if(params.containsKey("port"))
      {
        info.port = ((Long) params.get("port")).shortValue();
      }
      ///TODO: create info.nxtaddr

      agent.register(info, params);
      
      ///HACK: do not forget to put daemonId + 1 in ()
      info.bindaddr = "ipc://" + (info.daemonid + 1);
      info.connectaddr = "ipc://" + (info.daemonid + 2);
      if(info.permanentflag)
      {
        ///TODO: make port detection
        if(info.ipaddr != null && info.port != 0)
        {
          info.bindaddr = "tcp://" + info.ipaddr + ":" + (info.port + 1);
        }
      }
      
      info.timeout = 0L;
      if(params.containsKey("timeout"))
      {
        info.timeout = (long) params.get("timeout");
      }
  
      comm = new PairSocket();
      if(info.timeout > 0)
      {
        logger.println("nanomsg timeout set to " + info.timeout);
        
        comm.setSendTimeout(info.timeout.intValue());
        comm.setRecvTimeout(info.timeout.intValue());
      }
      
      comm.bind(info.bindaddr + ".pair");
      comm.connect(info.connectaddr + ".pair");
      
      logger.println("connected to " + info.connectaddr);
      
      JSONObject registerJson = new JSONObject();
      
      JSONArray methodsJson = new JSONArray();
      for(String a : agent.methods)
      {
        methodsJson.add(a);
      }
      registerJson.put("methods", methodsJson);
      JSONArray pubmethodsJson = new JSONArray();
      for(String a : agent.pubmethods)
      {
        pubmethodsJson.add(a);
      }
      registerJson.put("pubmethods", pubmethodsJson);
      JSONArray authmethodsJson = new JSONArray();
      for(String a : agent.authmethods)
      {
        authmethodsJson.add(a);
      }
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
      registerJson.put("sleepmillis", info.sleepmillis);
      registerJson.put("allowremote", info.allowremote? 1: 0);
      registerJson.put("permanentflag", info.permanentflag? 1:0);
      ///TODO: random number
      registerJson.put("myid", "12463223069612204128");
      registerJson.put("plugin", info.name);
      registerJson.put("endpoint", info.bindaddr);
      registerJson.put("millis", 100.0);
      registerJson.put("sent", info.numsent);
      registerJson.put("recv", info.numrecv);
      
      reply(registerJson.toString());
      
      while(true)
      {
        String message = comm.recvString();
        logger.println("recv: " + message);
        info.numrecv++;
        
        JSONObject request = null;
  
        try
        {
          ///HACK: trailing bytes mess with parser
          message.trim();
          if(message.charAt(message.length() - 1) == 0)
          {
            message = message.substring(0, message.length() - 1);
          }
          if(message.charAt(message.length() - 1) == '\r')
          {
            message = message.substring(0, message.length() - 1);
          }
          if(message.charAt(message.length() - 1) == '\n')
          {
            message = message.substring(0, message.length() - 1);
          }
          
          request = (JSONObject) parser.parse(message);
          if(request == null)
          {
            logger.println("request (" + message + ") not parsed");
            continue;
          }
          
          JSONObject answer = agent.process(info, request);
          if(answer == null)
          {
            logger.println("request (" + message + ") has no response");
            continue;
          }
          
          reply(answer.toString());
        }
        catch (ParseException e)
        {
          e.printStackTrace(logger);
        }
        
        try
        {
          agent.idle(info);
          ///TODO: use sleepmillis
          Thread.sleep(10);
        }
        catch (InterruptedException e)
        {
          break;
        }
      }
    }
    catch(Exception e)
    {
      e.printStackTrace(logger);
    }
    
    agent.shutdown(info, 0);
  }
  
  public static void main(String[] args)
  {
    mainLoop(args);
  }
}
