package org.apache.dolphinscheduler.plugin.task.resource;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreemarkHelper {

    static final Configuration configuration = getFreemarkerConfiguration();

    public static Configuration getFreemarkerConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setDefaultEncoding("utf-8");
        return cfg;
    }

    /**
     * 解析模板
     *
     * @param configuration
     * @param templateName
     * @throws IOException
     * @throws TemplateException
     */
    public static String processTemplate(String templateValue, Map<String, String> root)
            throws IOException, TemplateException {
        StringWriter stringWriter = new StringWriter();
        Template template = new Template("templateName", templateValue, configuration);
        template.process(root, stringWriter);
        return stringWriter.toString();
    }
}
