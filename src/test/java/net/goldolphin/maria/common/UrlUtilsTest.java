package net.goldolphin.maria.common;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author caofuxiang
 *         2014-03-19 15:53
 */
public class UrlUtilsTest {
    @Test
    public void testConcat() throws Exception {
        Assert.assertEquals("aaaa/bbbb/cccc/dddd/eeee/ffff/", UrlUtils.concat("aaaa", "bbbb///cccc///", "/////dddd", "///eeee", "/ffff/"));
    }
}
