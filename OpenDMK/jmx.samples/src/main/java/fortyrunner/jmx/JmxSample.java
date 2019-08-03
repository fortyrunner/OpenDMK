package fortyrunner.jmx;

import com.sun.jdmk.comm.HtmlAdaptorServer;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public class JmxSample {

  public static void main(final String... args) {

    try {
      com.sun.jdmk.comm.HtmlAdaptorServer adapter = new HtmlAdaptorServer(9000);
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      server.registerMBean(adapter, new ObjectName("Adaptor:name=html,port=8000"));
      adapter.start();
      Thread.sleep(100000);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

  }

}
