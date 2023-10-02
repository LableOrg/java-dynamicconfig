/*
 * Copyright © 2015 Lable (info@lable.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Provides thread safe access to a {@link CombinedConfiguration} instance.
 */
public class ConcurrentConfiguration implements Configuration {
    public static final String MODIFICATION_TIMESTAMP = "dc.last-modified-at";

    // This lock allows for multiple concurrent readers, but if a write-lock is acquired no other threads can read or
    // write until the lock is released.
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    final CombinedConfiguration wrapped;

    final static String NO_MODIFICATION_MESSAGE =
            "This configuration class does not permit modification, " +
            "except through #withConfiguration.";

    public ConcurrentConfiguration(CombinedConfiguration wrapped) {
        this.wrapped = wrapped;
    }
    public void withConfiguration(Consumer<CombinedConfiguration> consumer) {
        writeLock.lock();
        try {
            consumer.accept(wrapped);
            // Mark the time of modification.
            markAsModified();
        } finally {
            writeLock.unlock();
        }
    }

    void markAsModified() {
        wrapped.setProperty(MODIFICATION_TIMESTAMP, System.nanoTime());
    }

    /*
     * Mutations are forbidden, except through #withConfiguration(). By disabling modification here,
     * all Configuration methods implemented that remain are read-only.
     */

    @Override
    public void addProperty(String key, Object value) {
        throw new UnsupportedOperationException(NO_MODIFICATION_MESSAGE);
    }

    @Override
    public void setProperty(String key, Object value) {
        throw new UnsupportedOperationException(NO_MODIFICATION_MESSAGE);
    }

    @Override
    public void clearProperty(String key) {
        throw new UnsupportedOperationException(NO_MODIFICATION_MESSAGE);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException(NO_MODIFICATION_MESSAGE);
    }

    /*
     * All methods below are wrapped with the read lock, and passed on to the super class(es).
     * The same couple of lines are repeated for each method, simply passing the call on to the wrapped instance.
     */

    @Override
    public Configuration subset(String prefix) {
        readLock.lock();
        try {
            return wrapped.subset(prefix);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        readLock.lock();
        try {
            return wrapped.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean containsKey(String key) {
        readLock.lock();
        try {
            return wrapped.containsKey(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Object getProperty(String key) {
        readLock.lock();
        try {
            return wrapped.getProperty(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Iterator<String> getKeys(String prefix) {
        readLock.lock();
        try {
            return wrapped.getKeys(prefix);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Iterator<String> getKeys() {
        readLock.lock();
        try {
            return wrapped.getKeys();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Properties getProperties(String key) {
        readLock.lock();
        try {
            return wrapped.getProperties(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean getBoolean(String key) {
        readLock.lock();
        try {
            return wrapped.getBoolean(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        readLock.lock();
        try {
            return wrapped.getBoolean(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        readLock.lock();
        try {
            return wrapped.getBoolean(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public byte getByte(String key) {
        readLock.lock();
        try {
            return wrapped.getByte(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public byte getByte(String key, byte defaultValue) {
        readLock.lock();
        try {
            return wrapped.getByte(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        readLock.lock();
        try {
            return wrapped.getByte(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double getDouble(String key) {
        readLock.lock();
        try {
            return wrapped.getDouble(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        readLock.lock();
        try {
            return wrapped.getDouble(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        readLock.lock();
        try {
            return wrapped.getDouble(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public float getFloat(String key) {
        readLock.lock();
        try {
            return wrapped.getFloat(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        readLock.lock();
        try {
            return wrapped.getFloat(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        readLock.lock();
        try {
            return wrapped.getFloat(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getInt(String key) {
        readLock.lock();
        try {
            return wrapped.getInt(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getInt(String key, int defaultValue) {
        readLock.lock();
        try {
            return wrapped.getInt(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        readLock.lock();
        try {
            return wrapped.getInteger(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getLong(String key) {
        readLock.lock();
        try {
            return wrapped.getLong(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getLong(String key, long defaultValue) {
        readLock.lock();
        try {
            return wrapped.getLong(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        readLock.lock();
        try {
            return wrapped.getLong(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public short getShort(String key) {
        readLock.lock();
        try {
            return wrapped.getShort(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public short getShort(String key, short defaultValue) {
        readLock.lock();
        try {
            return wrapped.getShort(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        readLock.lock();
        try {
            return wrapped.getShort(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        readLock.lock();
        try {
            return wrapped.getBigDecimal(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        readLock.lock();
        try {
            return wrapped.getBigDecimal(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BigInteger getBigInteger(String key) {
        readLock.lock();
        try {
            return wrapped.getBigInteger(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        readLock.lock();
        try {
            return wrapped.getBigInteger(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getString(String key) {
        readLock.lock();
        try {
            return wrapped.getString(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getString(String key, String defaultValue) {
        readLock.lock();
        try {
            return wrapped.getString(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getStringArray(String key) {
        readLock.lock();
        try {
            return wrapped.getStringArray(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Object> getList(String key) {
        readLock.lock();
        try {
            return wrapped.getList(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Object> getList(String key, List<?> defaultValue) {
        readLock.lock();
        try {
            return wrapped.getList(key, defaultValue);
        } finally {
            readLock.unlock();
        }
    }
}
