package org.lable.dynamicconfig.core.util;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ConfigurationSystemPropertyTest {
    @Test
    public void testReadSystemProperty() {
        System.setProperty("unittest.xxx", "yyy");
        assertThat(ConfigurationSystemProperty.readSystemProperty("unittest.xxx"), is("yyy"));
    }
}