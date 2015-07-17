package snagentj;

import java.io.PrintStream;
import java.nio.charset.Charset;

import nanomsg.bus.BusSocket;
import nanomsg.exceptions.IOException;
import nanomsg.pipeline.PushSocket;
import nrs.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Framework
{
  public static PrintStream logger;
  public static nanomsg.Socket snetsock;
  public static nanomsg.Socket agentsock;
  //all agent parameters
  public static AgentInfo info = new AgentInfo();
  
  public static AgentInterface agent = null;
  
  public static void reply(String data)
  {
    logger.println("send: " + data);
    
    ///HACK: SuperNet expects trailing zero byte. Append it here.
    final Charset encoding = Charset.forName("UTF-8");
    byte[] bytes = data.getBytes(encoding);
    byte[] addBytes = new byte[bytes.length + 1];
    System.arraycopy(bytes, 0, addBytes, 0, bytes.length);
    snetsock.send(addBytes, true);
    info.numsent++;
  }
  
  @SuppressWarnings("unchecked")
  public static JSONObject addstdfields(JSONObject data)
  {
    data.put("allowremote", info.allowremote? 1: 0);
    data.put("daemonid", Convert.toUnsignedLong(info.daemonid));
    data.put("myid", Convert.toUnsignedLong(info.myid));
    if(info.nxtaddr != 0)
    {
      data.put("NXT", Convert.toUnsignedLong(info.nxtaddr));
    }
    if(info.servicenxtaddr != 0)
    {
      data.put("serviceNXT", Convert.toUnsignedLong(info.servicenxtaddr));
    }
    return data;
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
      info.ppid = (long) Convert.parseUnsignedLong(args[3]);
      info.name = agent.getName();
      info.numsent = 0L;
      info.numrecv = 0L;
      info.nxtaddr = 0L;
      info.servicenxtaddr = 0L;
      
      byte[] randomBytes = ProcessUtils.RandomBytes(8);
      info.myid = Convert.bytesToLong(randomBytes);
      
      JSONParser parser = new JSONParser();
      
      ///HACK: params come as json array inside quotes - not so easy to parse
      String tmp = (String) parser.parse(args[2]);
      //JSONArray allParams = (JSONArray) parser.parse(args[2]);
      JSONObject params = (JSONObject) parser.parse(tmp);
      
      String nxtbits = Convert.emptyToNull((String) params.get("NXT"));
      if(nxtbits != null)
      {
        info.nxtaddr = Convert.parseUnsignedLong(nxtbits);
      }
      nxtbits = Convert.emptyToNull((String) params.get("serviceNXT"));
      if(nxtbits != null)
      {
        info.servicenxtaddr = Convert.parseUnsignedLong(nxtbits);
      }
      info.ipaddr = Convert.emptyToNull((String) params.get("ipaddr"));
      if(params.containsKey("port"))
      {
        info.port = ((Long) params.get("port")).shortValue();
      }

      System.out.println("Parent PID is " + info.ppid);

      info.connectaddr = "ipc://SuperNET";
      info.bindaddr = "ipc://" + info.daemonid;
      /*if(info.permanentflag)
      {
        ///TODO: make port detection
        if(info.ipaddr != null && info.port != 0)
        {
          info.bindaddr = "tcp://" + info.ipaddr + ":" + (info.port + 1);
        }
      }*/
      
      info.timeout = 0L;
      if(params.containsKey("timeout"))
      {
        info.timeout = (long) params.get("timeout");
      }
      
      //default supernet sleep
      info.sleepmillis = 100L;
      if(params.containsKey("sleepmillis"))
      {
        info.sleepmillis = (long) params.get("sleepmillis");
      }
      
      snetsock = new PushSocket();
      /*if(info.timeout > 0)
      {
        logger.println("nanomsg timeout set to " + info.timeout);
        
        snetsock.setSendTimeout(info.timeout.intValue());
        snetsock.setRecvTimeout(info.timeout.intValue());
      }*/
      
      snetsock.connect(info.connectaddr);
      
      agentsock = new BusSocket();
      agentsock.bind(info.bindaddr);
      
      logger.println("connected to " + info.connectaddr);
      logger.println("listen on " + info.bindaddr);

      Long disabledMethods = agent.register(info, params);
      agent.processRegister(info, params);
      
      JSONObject registerJson = new JSONObject();
      
      JSONArray methodsJson = new JSONArray();
      int i = 0;
      for(String a : agent.getMethods())
      {
        if(((1L << i) & disabledMethods) == 0)
        {
          methodsJson.add(a);
        }
        i++;
      }
      registerJson.put("methods", methodsJson);
      
      JSONArray pubmethodsJson = new JSONArray();
      i = 0;
      for(String a : agent.getPubmethods())
      {
        if(((1L << i) & disabledMethods) == 0)
        {
          pubmethodsJson.add(a);
        }
        i++;
      }
      registerJson.put("pubmethods", pubmethodsJson);
      
      JSONArray authmethodsJson = new JSONArray();
      i = 0;
      for(String a : agent.getAuthmethods())
      {
        if(((1L << i) & disabledMethods) == 0)
        {
          authmethodsJson.add(a);
        }
        i++;
      }
      registerJson.put("authmethods", authmethodsJson);
      registerJson.put("pluginrequest", "SuperNET");
      registerJson.put("requestType", "register");
      
      addstdfields(registerJson);

      //what is this?
      registerJson.put("sleepmillis", info.sleepmillis);
      registerJson.put("permanentflag", info.permanentflag? 1:0);
      registerJson.put("plugin", info.name);
      
      registerJson.put("endpoint", info.bindaddr);
      registerJson.put("millis", System.currentTimeMillis());
      registerJson.put("sent", info.numsent);
      registerJson.put("recv", info.numrecv);
      
      reply(registerJson.toString());
      
      JSONObject answer = new JSONObject();
      while(true)
      {
        String message = null;
        try
        {
          message = agentsock.recvString(false);
        }
        catch(IOException e)
        {
        }
        
        if(message != null && message.length() != 0)
        {
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
            
            JSONArray itemsArray = null;
            try
            {
              itemsArray = (JSONArray) parser.parse(message);
            }
            catch(ParseException e)
            {
            }
            catch(ClassCastException e)
            {
            }
            
            if(itemsArray != null)
            {
              request = (JSONObject) itemsArray.get(0);
              
              if(itemsArray.size() > 1)
              {
                //token unused now - debug only
                JSONObject tokenObject = (JSONObject) itemsArray.get(1);
                String forwarder = Convert.emptyToNull((String) tokenObject.get("forwarder"));
                String sender = Convert.emptyToNull((String) tokenObject.get("sender"));
                Long valid = (Long) tokenObject.get("valid");
                
                logger.println("token: forwarder = " + forwarder + ", sender = " + sender + ", valid = " + valid);
              }
            }
            else
            {
              request = (JSONObject) parser.parse(message);
            }
            
            if(request == null)
            {
              logger.println("couldnt parse (" + message + ")");
              answer.put("result", "unparseable");
              answer.put("message", message);
              addstdfields(answer);
              reply(answer.toString());
              continue;
            }
            
            String tag = Convert.emptyToNull((String)request.get("tag"));
            String name = Convert.emptyToNull((String)request.get("plugin"));
            if(name == null)
            {
              name = Convert.emptyToNull((String)request.get("agent"));
            }
            
            String destname = Convert.emptyToNull((String)request.get("destplugin"));
            if(destname == null)
            {
              destname = Convert.emptyToNull((String)request.get("destagent"));
            }
            
            //check request is ours
            if((name == null || !info.name.equals(name)) && (destname == null || !info.name.equals(destname)))
            {
              continue;
            }
            
            answer = agent.process(info, request);
            if(answer == null)
            {
              logger.println("request (" + message + ") has no response");
              answer = new JSONObject();
              answer.put("result", "no response");
              if(tag != null)
              {
                answer.put("tag", tag);
              }
              addstdfields(answer);
              reply(answer.toString());
              continue;
            }
            
            if(tag != null)
            {
              answer.put("tag", tag);
            }
            addstdfields(answer);
            reply(answer.toString());
          }
          catch (ParseException | ClassCastException e)
          {
            logger.println("couldnt parse (" + message + ")");
            answer.put("result", "unparseable");
            answer.put("message", message);
            addstdfields(answer);
            reply(answer.toString());
            continue;
          }
        }
        
        int parentalive = ProcessUtils.OsPingPid(info.ppid.intValue());
        if(parentalive != 0)
        {
          logger.println("Parent " + info.ppid + " died. Terminating.");
          break;
        }
        
        try
        {
          if(agent.idle(info) == 0)
          {
            Thread.sleep(info.sleepmillis);
          }
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
    agentsock.close();
    snetsock.close();
  }
  
  public static void main(String[] args)
  {
    System.out.println("Using empty agent implementation. Make sure to call valid entry function.");
    logger = System.out;
    agent = new AgentInterface();
    mainLoop(args);
  }
}
