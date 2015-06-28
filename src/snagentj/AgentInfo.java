package snagentj;

public class AgentInfo
{
  /*struct plugin_info
  {
      char bindaddr[64],connectaddr[64],ipaddr[64],name[64],NXTADDR[64];
      uint64_t daemonid,myid,nxt64bits;
      union endpoints all;
      uint32_t permanentflag,ppid,transportid,extrasize,timeout,numrecv,numsent,bundledflag,registered,sleepmillis,allowremote;
      uint16_t port;
      portable_mutex_t mutex;
      uint8_t pluginspace[];
  };*/
  
  public String bindaddr;
  public String connectaddr;
  public String ipaddr;
  public String name;
  public String nxtaddr;
  public Long daemonid;
  public Long myid;
  public Long nxt64bits;
  public boolean permanentflag;
  public boolean allowremote;
  public boolean bundledflag; //unused
  public boolean registered;
  public Long timeout;
  public Long numsent;
  public Long numrecv;
  public Long sleepmillis;
  public Long ppid;
  public short port;
}
