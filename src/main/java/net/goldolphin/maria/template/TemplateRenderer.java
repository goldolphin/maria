package net.goldolphin.maria.template;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

import static freemarker.template.Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS;

/**
 * @author caofuxiang
 *         2014-12-08 14:22:22.
 */
public class TemplateRenderer {
    private final Configuration freeMarkerConf;

    public TemplateRenderer(Configuration freeMarkerConf) {
        this.freeMarkerConf = freeMarkerConf;
    }

    public TemplateRenderer(File templateDir) throws IOException {
        freeMarkerConf = new Configuration(DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        freeMarkerConf.setDirectoryForTemplateLoading(templateDir);
        freeMarkerConf.setObjectWrapper(new DefaultObjectWrapperBuilder(DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build());
    }

    public TemplateRenderer(String basePackagePath) throws IOException {
        freeMarkerConf = new Configuration(DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        freeMarkerConf.setClassForTemplateLoading(TemplateRenderer.class, basePackagePath);
        freeMarkerConf.setObjectWrapper(new DefaultObjectWrapperBuilder(DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build());
    }

    public TemplateRenderer(Class resourceLoaderClass, String basePackagePath) throws IOException {
        freeMarkerConf = new Configuration(DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        freeMarkerConf.setClassForTemplateLoading(resourceLoaderClass, basePackagePath);
        freeMarkerConf.setObjectWrapper(new DefaultObjectWrapperBuilder(DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build());
    }

    public ByteBuf render(Object model, String view) throws IOException, TemplateException {
        Template template = freeMarkerConf.getTemplate(view);
        ByteBuf byteBuf = Unpooled.buffer();
        OutputStreamWriter writer = new OutputStreamWriter(new ByteBufOutputStream(byteBuf));
        template.process(model, writer);
        return byteBuf;
    }
}
