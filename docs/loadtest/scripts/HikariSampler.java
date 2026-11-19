import com.sun.tools.attach.VirtualMachine;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 실행 중인 앱(JVM)의 HikariCP 풀 상태를 JMX로 1초마다 읽어 CSV로 남긴다. 앱 코드·의존성 변경 없이 쓸 수 있다.
 * 앱은 spring.datasource.hikari.register-mbeans=true 로 떠 있어야 한다.
 *
 * usage: java --add-modules jdk.attach HikariSampler.java <pid> <out.csv> <seconds>
 * columns: epoch_ms, active(사용 중), idle(놀고 있음), waiting(커넥션을 기다리는 스레드), total(풀 크기)
 */
public class HikariSampler {
    public static void main(String[] args) throws Exception {
        String pid = args[0];
        Path out = Path.of(args[1]);
        int seconds = Integer.parseInt(args[2]);

        VirtualMachine vm = VirtualMachine.attach(pid);
        String address = vm.startLocalManagementAgent();
        vm.detach();

        try (JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(address))) {
            MBeanServerConnection mbs = connector.getMBeanServerConnection();
            ObjectName pool = null;
            for (ObjectName name : mbs.queryNames(new ObjectName("com.zaxxer.hikari:type=Pool (*)"), null)) {
                pool = name;
            }
            if (pool == null) {
                throw new IllegalStateException("Hikari MBean 이 없습니다. register-mbeans=true 로 기동했는지 확인하세요.");
            }

            try (BufferedWriter writer = Files.newBufferedWriter(out)) {
                writer.write("epoch_ms,active,idle,waiting,total\n");
                long end = System.currentTimeMillis() + seconds * 1000L;
                while (System.currentTimeMillis() < end) {
                    long now = System.currentTimeMillis();
                    writer.write(now + ","
                            + mbs.getAttribute(pool, "ActiveConnections") + ","
                            + mbs.getAttribute(pool, "IdleConnections") + ","
                            + mbs.getAttribute(pool, "ThreadsAwaitingConnection") + ","
                            + mbs.getAttribute(pool, "TotalConnections") + "\n");
                    writer.flush();
                    Thread.sleep(1000);
                }
            }
        }
    }
}
