package snagentj;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

public class ProcessUtils {
    public interface NativeProcUtils extends Library {
      public int OS_getppid();
      public int OS_pingpid(int pid);
      public void randombytes(Pointer x,int xlen);
    }

    static NativeProcUtils procutils;

    static {
      procutils = (NativeProcUtils) Native.loadLibrary("procutils", NativeProcUtils.class);
    }

    public static int OsGetPpid () {
      return procutils.OS_getppid();
    }
    
    public static int OsPingPid (int pid) {
      return procutils.OS_pingpid(pid);
    }

    public static byte[] RandomBytes (int size) {
      Pointer x = new Memory(size);
      procutils.randombytes(x, size);
      return x.getByteArray(0, size);
    }
}