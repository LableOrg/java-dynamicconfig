/*
 * Copyright (C) 2015 Lable (info@lable.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lable.oss.dynamicconfig.core.commonsconfiguration;

import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.tree.OverrideCombiner;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

public class ConcurrentConfigurationTest {

    @Test
    public void testMethodWrappers() {
        CombinedConfiguration mockConfiguration = mock(CombinedConfiguration.class);
        Configuration concurrentConfiguration = new ConcurrentConfiguration(mockConfiguration, null);

        concurrentConfiguration.subset("subset");
        concurrentConfiguration.isEmpty();
        concurrentConfiguration.containsKey("key");
        concurrentConfiguration.getProperty("getprop");
        concurrentConfiguration.getKeys("getkeys");
        concurrentConfiguration.getKeys();
        concurrentConfiguration.getProperties("getprops");
        concurrentConfiguration.getBoolean("getboolean1");
        concurrentConfiguration.getBoolean("getboolean2", true);
        concurrentConfiguration.getBoolean("getboolean3", Boolean.FALSE);
        concurrentConfiguration.getByte("getbyte1");
        concurrentConfiguration.getByte("getbyte2", (byte) 0);
        concurrentConfiguration.getByte("getbyte3", Byte.valueOf((byte) 0));
        concurrentConfiguration.getDouble("getdouble1");
        concurrentConfiguration.getDouble("getdouble2", 0.2);
        concurrentConfiguration.getDouble("getdouble3", Double.valueOf(0.2));
        concurrentConfiguration.getFloat("getfloat1");
        concurrentConfiguration.getFloat("getfloat2", 0f);
        concurrentConfiguration.getFloat("getfloat3", Float.valueOf(0f));
        concurrentConfiguration.getInt("getint1");
        concurrentConfiguration.getInt("getint2", 0);
        concurrentConfiguration.getInteger("getint3", 0);
        concurrentConfiguration.getLong("getlong1");
        concurrentConfiguration.getLong("getlong2", 0L);
        concurrentConfiguration.getLong("getlong3", Long.valueOf(0L));
        concurrentConfiguration.getShort("getshort1");
        concurrentConfiguration.getShort("getshort2", (short) 0);
        concurrentConfiguration.getShort("getshort3", Short.valueOf((short) 0));
        concurrentConfiguration.getBigDecimal("getbigd1");
        concurrentConfiguration.getBigDecimal("getbigd2", BigDecimal.valueOf(0.4));
        concurrentConfiguration.getBigInteger("getbigi1");
        concurrentConfiguration.getBigInteger("getbigi2", BigInteger.valueOf(2L));
        concurrentConfiguration.getString("getstring1");
        concurrentConfiguration.getString("getstring2", "def");
        concurrentConfiguration.getStringArray("stringarray");
        concurrentConfiguration.getList("getlist1");
        concurrentConfiguration.getList("getlist2", Arrays.asList("a", "b"));

        verify(mockConfiguration, times(1)).subset("subset");
        verify(mockConfiguration, times(1)).isEmpty();
        verify(mockConfiguration, times(1)).containsKey("key");
        verify(mockConfiguration, times(1)).getProperty("getprop");
        verify(mockConfiguration, times(1)).getKeys("getkeys");
        verify(mockConfiguration, times(1)).getKeys();
        verify(mockConfiguration, times(1)).getProperties("getprops");
        verify(mockConfiguration, times(1)).getBoolean("getboolean1");
        verify(mockConfiguration, times(1)).getBoolean("getboolean2", true);
        verify(mockConfiguration, times(1)).getBoolean("getboolean3", Boolean.FALSE);
        verify(mockConfiguration, times(1)).getByte("getbyte1");
        verify(mockConfiguration, times(1)).getByte("getbyte2", (byte) 0);
        verify(mockConfiguration, times(1)).getByte("getbyte3", Byte.valueOf((byte) 0));
        verify(mockConfiguration, times(1)).getDouble("getdouble1");
        verify(mockConfiguration, times(1)).getDouble("getdouble2", 0.2);
        verify(mockConfiguration, times(1)).getDouble("getdouble3", Double.valueOf(0.2));
        verify(mockConfiguration, times(1)).getFloat("getfloat1");
        verify(mockConfiguration, times(1)).getFloat("getfloat2", 0f);
        verify(mockConfiguration, times(1)).getFloat("getfloat3", Float.valueOf(0f));
        verify(mockConfiguration, times(1)).getInt("getint1");
        verify(mockConfiguration, times(1)).getInt("getint2", 0);
        verify(mockConfiguration, times(1)).getInteger("getint3", Integer.valueOf(0));
        verify(mockConfiguration, times(1)).getLong("getlong1");
        verify(mockConfiguration, times(1)).getLong("getlong2", 0L);
        verify(mockConfiguration, times(1)).getLong("getlong3", Long.valueOf(0L));
        verify(mockConfiguration, times(1)).getShort("getshort1");
        verify(mockConfiguration, times(1)).getShort("getshort2", (short) 0);
        verify(mockConfiguration, times(1)).getShort("getshort3", Short.valueOf((short) 0));
        verify(mockConfiguration, times(1)).getBigDecimal("getbigd1");
        verify(mockConfiguration, times(1)).getBigDecimal("getbigd2", BigDecimal.valueOf(0.4));
        verify(mockConfiguration, times(1)).getBigInteger("getbigi1");
        verify(mockConfiguration, times(1)).getBigInteger("getbigi2", BigInteger.valueOf(2L));
        verify(mockConfiguration, times(1)).getString("getstring1");
        verify(mockConfiguration, times(1)).getString("getstring2", "def");
        verify(mockConfiguration, times(1)).getStringArray("stringarray");
        verify(mockConfiguration, times(1)).getList("getlist1");
        verify(mockConfiguration, times(1)).getList("getlist2", Arrays.asList("a", "b"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void addPropertyTest() {
        CombinedConfiguration mockConfiguration = mock(CombinedConfiguration.class);
        Configuration concurrentConfiguration = new ConcurrentConfiguration(mockConfiguration, null);
        concurrentConfiguration.addProperty("test", "test");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setPropertyTest() {
        CombinedConfiguration mockConfiguration = mock(CombinedConfiguration.class);
        Configuration concurrentConfiguration = new ConcurrentConfiguration(mockConfiguration, null);
        concurrentConfiguration.setProperty("test", "test");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void clearPropertyTest() {
        CombinedConfiguration mockConfiguration = mock(CombinedConfiguration.class);
        Configuration concurrentConfiguration = new ConcurrentConfiguration(mockConfiguration, null);
        concurrentConfiguration.clearProperty("test");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void clearTest() {
        CombinedConfiguration mockConfiguration = mock(CombinedConfiguration.class);
        Configuration concurrentConfiguration = new ConcurrentConfiguration(mockConfiguration, null);
        concurrentConfiguration.clear();
    }

    @Test
    public void test() {
        HierarchicalConfiguration include = new HierarchicalConfiguration();
        include.setProperty("fruits", Arrays.asList("banana", "apple", "cherry"));
        include.setProperty("veg", Arrays.asList("cucumber", "hamster"));


        HierarchicalConfiguration main = new HierarchicalConfiguration();
        main.setProperty("tree.a.fruits", "xxx");

        final CombinedConfiguration allConfig = new CombinedConfiguration(new OverrideCombiner());

        allConfig.addConfiguration(main, "/");
        allConfig.addConfiguration(include, "1", "tree.a");
        allConfig.addConfiguration(include, "2", "tree.b");

        String[] fruit = allConfig.getStringArray("tree.b.fruits");
        for (String s : fruit) {
            System.out.println(s);
        }

        include.setProperty("fruits", Collections.singletonList("orange"));

        fruit = allConfig.getStringArray("tree.b.fruits");
        for (String s : fruit) {
            System.out.println(s);
        }
    }

    @Test
    public void modifyConfigurationTest() {
        CombinedConfiguration allConfig = new CombinedConfiguration(new OverrideCombiner());
        HierarchicalConfiguration confA = new HierarchicalConfiguration();
        HierarchicalConfiguration confB = new HierarchicalConfiguration();

        confA.setProperty("a.a", 1);
        confB.setProperty("a.a", 2);

        allConfig.addConfiguration(confA, "a");
        allConfig.addConfiguration(confB, "b");
        allConfig.addConfiguration(confB, "d", "b.b");
        allConfig.addConfiguration(confB, "c", "c.c");

        System.out.println(allConfig.getInt("a.a"));
        System.out.println(allConfig.getInt("b.b.a.a"));
        System.out.println(allConfig.getInt("c.c.a.a"));

        ConcurrentConfiguration concurrentConfiguration = new ConcurrentConfiguration(allConfig);

        System.out.println(concurrentConfiguration.getInt("a.a"));

        concurrentConfiguration.withConfiguration(cc -> {
            cc.clear();
            cc.addConfiguration(confB, "b");
            cc.addConfiguration(confA, "a");
        });

        System.out.println(concurrentConfiguration.getInt("a.a"));

    }
}