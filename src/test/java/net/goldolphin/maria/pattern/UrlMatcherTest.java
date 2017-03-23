package net.goldolphin.maria.pattern;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author caofuxiang
 *         2014-03-19 13:32
 */
public class UrlMatcherTest {
    @Test
    public void testMatch() throws Exception {
        UrlMatcher<String> matcher = UrlMatcher.newInstance();
        matcher.addPattern("device/$deviceId/[$resource]/[user]", "bindUser");
        matcher.addPattern("device/$deviceId/$resource/group", "bindGroup");
        matcher.addPattern("device/$deviceId/$resource/group/$groupId", "bindGroupId");

        Assert.assertEquals("bindUser", print(matcher.match("device/123/res1/user")).getData());
        Assert.assertEquals("bindUser", print(matcher.match("device/123")).getData());
        Assert.assertEquals("bindUser", print(matcher.match("device/123/res1")).getData());
        Assert.assertEquals("bindGroup", print(matcher.match("device/1235/res2/group")).getData());
        Assert.assertEquals("bindGroupId", print(matcher.match("device/1236/res1/group/234")).getData());
        Assert.assertFalse(print(matcher.match("device/1236/res1/group/234/8")).isMatched());
        Assert.assertFalse(print(matcher.match("device/1236/res1/group1/234")).isMatched());
    }

    private static <T> T print(T t) {
        System.out.println(t);
        return t;
    }
}
