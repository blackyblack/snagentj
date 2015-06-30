package snagentj;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

public class ProcessUtils {
    public interface NativeProcUtils extends Library {
      public int OS_getpid();
      public void randombytes(Pointer x,int xlen);
    }

    static NativeProcUtils procutils;

    static {
      procutils = (NativeProcUtils) Native.loadLibrary("procutils", NativeProcUtils.class);
    }

    public static int OsGetPid () {
      return procutils.OS_getpid();
    }

    public static byte[] RandomBytes (int size) {
      Pointer x = new Memory(size);
      procutils.randombytes(x, size);
      return x.getByteArray(0, size);
    }
}