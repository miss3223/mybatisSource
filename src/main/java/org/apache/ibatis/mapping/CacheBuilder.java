/**
 *    Copyright 2009-2018 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.mapping;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.builder.InitializingObject;
import org.apache.ibatis.cache.decorators.BlockingCache;
import org.apache.ibatis.cache.decorators.LoggingCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.ScheduledCache;
import org.apache.ibatis.cache.decorators.SerializedCache;
import org.apache.ibatis.cache.decorators.SynchronizedCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * @author Clinton Begin
 *
 * Cache构造器，属于装饰者模式，进行Cache对象的构造
 */
public class CacheBuilder {
  /** 编号
   *  代表命名空间
   * */
  private final String id;

  /** 负责存储的Cache实现类 */
  private Class<? extends Cache> implementation;

  /** Cache 装饰类集合*/
  private final List<Class<? extends Cache>> decorators;

  /** 缓存容器的大小 */
  private Integer size;

  /** 清空缓存的频率 0 代表不清空 */
  private Long clearInterval;

  /** 是否序列化 */
  private boolean readWrite;

  /** Properties对象 */
  private Properties properties;

  /** 是否阻塞 */
  private boolean blocking;

  public CacheBuilder(String id) {
    this.id = id;
    this.decorators = new ArrayList<>();
  }

  public CacheBuilder implementation(Class<? extends Cache> implementation) {
    this.implementation = implementation;
    return this;
  }

  public CacheBuilder addDecorator(Class<? extends Cache> decorator) {
    if (decorator != null) {
      this.decorators.add(decorator);
    }
    return this;
  }

  public CacheBuilder size(Integer size) {
    this.size = size;
    return this;
  }

  public CacheBuilder clearInterval(Long clearInterval) {
    this.clearInterval = clearInterval;
    return this;
  }

  public CacheBuilder readWrite(boolean readWrite) {
    this.readWrite = readWrite;
    return this;
  }

  public CacheBuilder blocking(boolean blocking) {
    this.blocking = blocking;
    return this;
  }
  
  public CacheBuilder properties(Properties properties) {
    this.properties = properties;
    return this;
  }

  public Cache build() {
    // 设置默认实现类
    setDefaultImplementations();
    // 创建基础Cache对象
    Cache cache = newBaseCacheInstance(implementation, id);
    // 设置Cache属性
    setCacheProperties(cache);
    // issue #352, do not apply decorators to custom caches
    // 如果是PerpetualCache类，则进行包装  PerpetualCache --> 永不过去的Cache
    if (PerpetualCache.class.equals(cache.getClass())) {
      // 遍历decorators
      for (Class<? extends Cache> decorator : decorators) {
        // 包装Cache对象
        cache = newCacheDecoratorInstance(decorator, cache);
        // 设置cache对象
        setCacheProperties(cache);
      }
      // 执行标准化的Cache包装
      cache = setStandardDecorators(cache);
      // 如果是自定义的Cache类，则包装成LoggingCache对象，做统计用
    } else if (!LoggingCache.class.isAssignableFrom(cache.getClass())) {
      cache = new LoggingCache(cache);
    }
    return cache;
  }

  /** 设置默认实现类 */
  private void setDefaultImplementations() {
    if (implementation == null) {
      implementation = PerpetualCache.class;
      if (decorators.isEmpty()) {
        decorators.add(LruCache.class);
      }
    }
  }

  /**
   * 指定标准化的Cache包装
   * @param cache cache对象
   * @return
   */
  private Cache setStandardDecorators(Cache cache) {
    try {
      // 设置MetaObject
      MetaObject metaCache = SystemMetaObject.forObject(cache);
      // 如果缓存容器不为null
      if (size != null && metaCache.hasSetter("size")) {
        // 设置Cache的值
        metaCache.setValue("size", size);
      }
      // 包装成ScheduledCache对象  ScheduledCache -- >定时清空整个容器 的Cache实现类
      if (clearInterval != null) {
        cache = new ScheduledCache(cache);
        ((ScheduledCache) cache).setClearInterval(clearInterval);
      }
      // 包装成SerializedCache对象
      if (readWrite) {
        cache = new SerializedCache(cache);
      }
      // 包装成LoggingCache对象
      cache = new LoggingCache(cache);
      // 包装成 SynchronizedCache对象
      cache = new SynchronizedCache(cache);
      if (blocking) {
        // 包装成BlockingCache对象
        cache = new BlockingCache(cache);
      }
      return cache;
    } catch (Exception e) {
      throw new CacheException("Error building standard cache decorators.  Cause: " + e, e);
    }
  }

  private void setCacheProperties(Cache cache) {
    if (properties != null) {
      // 初始化Cache对象的属性
      MetaObject metaCache = SystemMetaObject.forObject(cache);
      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        String name = (String) entry.getKey();
        String value = (String) entry.getValue();
        if (metaCache.hasSetter(name)) {
          Class<?> type = metaCache.getSetterType(name);
          if (String.class == type) {
            metaCache.setValue(name, value);
          } else if (int.class == type
              || Integer.class == type) {
            metaCache.setValue(name, Integer.valueOf(value));
          } else if (long.class == type
              || Long.class == type) {
            metaCache.setValue(name, Long.valueOf(value));
          } else if (short.class == type
              || Short.class == type) {
            metaCache.setValue(name, Short.valueOf(value));
          } else if (byte.class == type
              || Byte.class == type) {
            metaCache.setValue(name, Byte.valueOf(value));
          } else if (float.class == type
              || Float.class == type) {
            metaCache.setValue(name, Float.valueOf(value));
          } else if (boolean.class == type
              || Boolean.class == type) {
            metaCache.setValue(name, Boolean.valueOf(value));
          } else if (double.class == type
              || Double.class == type) {
            metaCache.setValue(name, Double.valueOf(value));
          } else {
            throw new CacheException("Unsupported property type for cache: '" + name + "' of type " + type);
          }
        }
      }
    }
    if (InitializingObject.class.isAssignableFrom(cache.getClass())){
      try {
        ((InitializingObject) cache).initialize();
      } catch (Exception e) {
        throw new CacheException("Failed cache initialization for '" +
            cache.getId() + "' on '" + cache.getClass().getName() + "'", e);
      }
    }
  }

  /**
   * 创建基础Cache对象
   * @param cacheClass cache类
   * @param id 编号
   * @return Cache对象
   */
  private Cache newBaseCacheInstance(Class<? extends Cache> cacheClass, String id) {
    // 获得Cache类的构造方法
    Constructor<? extends Cache> cacheConstructor = getBaseCacheConstructor(cacheClass);
    try {
      // 创建Cache对象
      return cacheConstructor.newInstance(id);
    } catch (Exception e) {
      throw new CacheException("Could not instantiate cache implementation (" + cacheClass + "). Cause: " + e, e);
    }
  }

  /**
   * 获得Cache类的构造方法
   * @param cacheClass cache类
   * @return 构造方法
   */
  private Constructor<? extends Cache> getBaseCacheConstructor(Class<? extends Cache> cacheClass) {
    try {
      return cacheClass.getConstructor(String.class);
    } catch (Exception e) {
      throw new CacheException("Invalid base cache implementation (" + cacheClass + ").  " +
          "Base cache implementations must have a constructor that takes a String id as a parameter.  Cause: " + e, e);
    }
  }

  /**
   * 包装指定cache
   *
   * @param cacheClass 包装的Cache类
   * @param base 被包装的Cache类
   * @return 包装后的Cache类
   */
  private Cache newCacheDecoratorInstance(Class<? extends Cache> cacheClass, Cache base) {
    // 获得方法参数为Cache的构造方法
    Constructor<? extends Cache> cacheConstructor = getCacheDecoratorConstructor(cacheClass);
    try {
      // 创建Cache对象
      return cacheConstructor.newInstance(base);
    } catch (Exception e) {
      throw new CacheException("Could not instantiate cache decorator (" + cacheClass + "). Cause: " + e, e);
    }
  }

  /**
   * 获得方法参为Cache的构造方法
   *
   * @param cacheClass 指定类
   * @return 构造方法
   */
  private Constructor<? extends Cache> getCacheDecoratorConstructor(Class<? extends Cache> cacheClass) {
    try {
      return cacheClass.getConstructor(Cache.class);
    } catch (Exception e) {
      throw new CacheException("Invalid cache decorator (" + cacheClass + ").  " +
          "Cache decorators must have a constructor that takes a Cache instance as a parameter.  Cause: " + e, e);
    }
  }
}
