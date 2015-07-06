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
        info.nxt64bits = Convert.parseUnsignedLong(nxtbits);
      }
      info.ipaddr = Convert.emptyToNull((String) params.get("ipaddr"));
      if(params.containsKey("port"))
      {
        info.port = ((Long) params.get("port")).shortValue();
      }
      ///TODO: create info.nxtaddr

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
      
      snetsock = new PushSocket();
      snetsock.setSendTimeout(500);
      snetsock.setRecvTimeout(500);
      /*if(info.timeout > 0)
      {
        logger.println("nanomsg timeout set to " + info.timeout);
        
        snetsock.setSendTimeout(info.timeout.intValue());
        snetsock.setRecvTimeout(info.timeout.intValue());
      }*/
      
      snetsock.connect(info.connectaddr);
      
      agentsock = new BusSocket();
      agentsock.setSendTimeout(500);
      agentsock.setRecvTimeout(500);
      agentsock.bind(info.bindaddr);
      
      logger.println("connected to " + info.connectaddr);
      logger.println("listen on " + info.bindaddr);

      agent.register(info, params);
      agent.processRegister(info, params);
      
      JSONObject registerJson = new JSONObject();
      
      JSONArray methodsJson = new JSONArray();
      for(String a : agent.getMethods())
      {
        methodsJson.add(a);
      }
      registerJson.put("methods", methodsJson);
      JSONArray pubmethodsJson = new JSONArray();
      for(String a : agent.getPubmethods())
      {
        pubmethodsJson.add(a);
      }
      registerJson.put("pubmethods", pubmethodsJson);
      JSONArray authmethodsJson = new JSONArray();
      for(String a : agent.getAuthmethods())
      {
        authmethodsJson.add(a);
      }
      registerJson.put("authmethods", authmethodsJson);
      registerJson.put("pluginrequest", "SuperNET");
      registerJson.put("requestType", "register");
      
      //stdfields
      registerJson.put("allowremote", info.allowremote? 1: 0);
      registerJson.put("daemonid", Convert.toUnsignedLong(info.daemonid));
      registerJson.put("myid", Convert.toUnsignedLong(info.myid));
      
      ///TODO: proper calculation
      registerJson.put("NXT", "1844690345");
      registerJson.put("serviceNXT", "18446744072072726485");
      //what is this?
      registerJson.put("sleepmillis", /*info.sleepmillis*/100);
      registerJson.put("permanentflag", info.permanentflag? 1:0);
      registerJson.put("plugin", info.name);
      
      registerJson.put("endpoint", info.bindaddr);
      registerJson.put("millis", 100.0);
      registerJson.put("sent", info.numsent);
      registerJson.put("recv", info.numrecv);
      
      reply(registerJson.toString());
      
      JSONObject answer = new JSONObject();
      while(true)
      {
        String message = null;
        ///TODO: will it timeout on died socket?
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
              ///TODO: some validate code...
              //timestamp = (uint32_t)get_API_int(cJSON_GetObjectItem(obj,"time"),0);
              //sender[0] = 0;
              //valid = validate_token(forwarder,pubkey,sender,jsonstr,(timestamp != 0)*MAXTIMEDIFF);
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
              //stdfields
              answer.put("allowremote", info.allowremote? 1: 0);
              answer.put("daemonid", Convert.toUnsignedLong(info.daemonid));
              answer.put("myid", Convert.toUnsignedLong(info.myid));
              reply(answer.toString());
              continue;
            }
            
            String tag = Convert.emptyToNull((String)request.get("tag"));
            
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
              //stdfields
              answer.put("allowremote", info.allowremote? 1: 0);
              answer.put("daemonid", Convert.toUnsignedLong(info.daemonid));
              answer.put("myid", Convert.toUnsignedLong(info.myid));
              reply(answer.toString());
              continue;
            }
            
            if(tag != null)
            {
              answer.put("tag", tag);
            }
            //stdfields
            answer.put("allowremote", info.allowremote? 1: 0);
            answer.put("daemonid", Convert.toUnsignedLong(info.daemonid));
            answer.put("myid", Convert.toUnsignedLong(info.myid));
            reply(answer.toString());
          }
          catch (ParseException | ClassCastException e)
          {
            logger.println("couldnt parse (" + message + ")");
            answer.put("result", "unparseable");
            answer.put("message", message);
            //stdfields
            answer.put("allowremote", info.allowremote? 1: 0);
            answer.put("daemonid", Convert.toUnsignedLong(info.daemonid));
            answer.put("myid", Convert.toUnsignedLong(info.myid));
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
          agent.idle(info);
          ///TODO: use sleepmillis
          Thread.sleep(100);
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
    mainLoop(args);
  }
}
