package org.apache.dolphinscheduler.plugin.alert.hermes;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FreemarkerHelper {

    static final Configuration configuration = new Configuration(Configuration.getVersion());

    public FreemarkerHelper() {
        configuration.setNumberFormat("0");
    }

    public static String paseWithMultiParams(String source, List<Map> mapList) {
        Map<String, Object> root = new HashMap<>();
        for (Map<String, Object> item : mapList) {
            if (item != null && item.size() > 0) {
                item.forEach((k, v) -> {
                    if (!root.containsKey(k)) {
                        root.put(k, v);
                    }
                });
            }
        }
        try {
            return processTemplate(source, root);
        } catch (IOException | TemplateException e) {
            log.error("freemark convert error！source:" + source + " mapList:" + mapList, e);
            return source;
        }
    }

    /**
     * 按照参数列表的顺序进行占位符替换
     * 
     * @param source
     * @param arrays
     * @return
     */
    public static String paseWithMultiParams(String source, Map<String, Object>... arrays) {
        Map<String, Object> root = new HashMap<>();
        for (int i = 0; i < arrays.length; i++) {
            Map<String, Object> item = arrays[i];
            if (item != null && item.size() > 0) {
                item.forEach((k, v) -> {
                    if (!root.containsKey(k)) {
                        root.put(k, v);
                    }
                });
            }
        }
        try {
            return processTemplate(source, root);
        } catch (IOException | TemplateException e) {
            log.error("freemark convert error！source:" + source + " arrays:" + arrays, e);
            return "freemark convert error！source:" + source + " arrays:" + arrays;
        }
    }

    /**
     * 解析模板
     *
     * @param configuration
     * @param templateName
     * @throws IOException
     * @throws TemplateException
     */
    public static String processTemplate(String templateValue, Map<String, Object> root)
            throws IOException, TemplateException {
        configuration.setDefaultEncoding("utf-8");
        StringWriter stringWriter = new StringWriter();
        Template template = new Template("templateName", templateValue, configuration);
        template.process(root, stringWriter);
        return stringWriter.toString();
    }
}
