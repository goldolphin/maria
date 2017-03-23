package net.goldolphin.maria.template;

import io.netty.util.CharsetUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author caofuxiang
 *         2016-04-06 19:46:46.
 */
public class TemplateRendererTest {
    @Test
    public void testRender() throws Exception {
        TemplateRenderer renderer = new TemplateRenderer("/templates");
        HashMap<String, String> model1 = new HashMap<String, String>();
        model1.put("name", "Xiao Ming");
        String result1 = renderer.render(model1, "index.html").toString(CharsetUtil.UTF_8);
        System.out.println(result1);
        Assert.assertEquals("Hello, Xiao Ming!", result1);

        Model2 model2 = new Model2();
        String result2 = renderer.render(model2, "index.html").toString(CharsetUtil.UTF_8);
        System.out.println(result2);
        Assert.assertEquals("Hello, Xiao Hong!", result2);
    }

    public static class Model2 {
        private final String name = "Xiao Hong";

        public String getName() {
            return name;
        }
    }
}