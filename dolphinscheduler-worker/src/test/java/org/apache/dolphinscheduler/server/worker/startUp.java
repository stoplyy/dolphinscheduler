package org.apache.dolphinscheduler.server.worker;

import org.apache.dolphinscheduler.plugin.storage.api.StorageOperate;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.server.worker.runner.WorkerTaskExecutorFactoryBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
// 这是Spring Boot注解，为了进行集成测试，需要通过这个注解加载和配置Spring应用上下
@SpringBootTest(classes = WorkerServer.class, properties = { "graceful.shutdown.enable=false" })
@WebAppConfiguration
@ActiveProfiles("test")
public class startUp {

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty("java._appid_", "int-website-arch-hubble-portal-api");
        System.setProperty("java._environment_", "work");
        System.setProperty("spring.profiles.active", "test");
        System.setProperty("apollo.meta", "http://apollo.tuhu.work:8090");
        System.setProperty("env", "DEV");
        System.setProperty("jfrog.dir.prefix", "test");
    }

    @Autowired(required = false)
    private StorageOperate storageOperate;


    @Autowired
    private WorkerTaskExecutorFactoryBuilder workerTaskExecutorFactoryBuilder;

    @Test
    public void test() {
        workerTaskExecutorFactoryBuilder.createWorkerTaskExecutorFactory(new TaskExecutionContext());
        System.out.println("test");
    }
}
