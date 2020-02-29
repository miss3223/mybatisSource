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
package org.apache.ibatis.builder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 *
 * 基础构造器抽象类，为子类提供通用的工具类
 */
public abstract class BaseBuilder {
  /** Configuration对象 */
  protected final Configuration configuration;
  /** typeAlias注册器 */
  protected final TypeAliasRegistry typeAliasRegistry;
  /** typeHandler注册器 */
  protected final TypeHandlerRegistry typeHandlerRegistry;

  public BaseBuilder(Configuration configuration) {
    this.configuration = configuration;
    this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
    // typeHandlerRegistry是configuration中的一个属性
    this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * 创建正则表达式
   * @param regex 指定表达式
   * @param defaultValue 默认表达式
   * @return 正则表达式
   */
  protected Pattern parseExpression(String regex, String defaultValue) {
    // 如果指定的表达式为null的时候，就使用默认的表达式
    return Pattern.compile(regex == null ? defaultValue : regex);
  }

  /**
   * 将不为null的值转换为Boolean类型
   * @param value  获取的值
   * @param defaultValue 默认类型
   * @return Boolean类型
   */
  protected Boolean booleanValueOf(String value, Boolean defaultValue) {
    // 如果指定的值为null的时候，就使用默认的值
    return value == null ? defaultValue : Boolean.valueOf(value);
  }

  /**
   * 将不为null的值转换为Integer类型
   * @param value 获取的值
   * @param defaultValue 默认类型
   * @return Integer类型
   */
  protected Integer integerValueOf(String value, Integer defaultValue) {
    // 如果指定的值为null的时候，就使用默认的值
    return value == null ? defaultValue : Integer.valueOf(value);
  }

  /**
   * 将不为null的值转换为Set<String>类型
   * @param value  获取的值
   * @param defaultValue 默认类型
   */
  protected Set<String> stringSetValueOf(String value, String defaultValue) {
    value = (value == null ? defaultValue : value);
    return new HashSet<>(Arrays.asList(value.split(",")));
  }

  /**
   * 解析对应的JdbcType类型
   */
  protected JdbcType resolveJdbcType(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return JdbcType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving JdbcType. Cause: " + e, e);
    }
  }

  /**
   * 解析对应的 ResultSetType 类型
   */
  protected ResultSetType resolveResultSetType(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return ResultSetType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ResultSetType. Cause: " + e, e);
    }
  }

  /**
   * 解析对应的ParameterMode类型
   */
  protected ParameterMode resolveParameterMode(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return ParameterMode.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
    }
  }

  /**
   * 创建指定对象
   * */
  protected Object createInstance(String alias) {
    // 获得对应的类型
    Class<?> clazz = resolveClass(alias);
    if (clazz == null) {
      return null;
    }
    try {
      // 创建对象
      return resolveClass(alias).newInstance();
    } catch (Exception e) {
      throw new BuilderException("Error creating instance. Cause: " + e, e);
    }
  }

  /** 获得对应的类型 */
  protected <T> Class<? extends T> resolveClass(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return resolveAlias(alias);
    } catch (Exception e) {
      throw new BuilderException("Error resolving class. Cause: " + e, e);
    }
  }

  /** 从typeHandlerRegistry中获得或创建对应的TypeHandler对象 */
  protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, String typeHandlerAlias) {
    if (typeHandlerAlias == null) {
      return null;
    }
    Class<?> type = resolveClass(typeHandlerAlias);
    if (type != null && !TypeHandler.class.isAssignableFrom(type)) {
      throw new BuilderException("Type " + type.getName() + " is not a valid TypeHandler because it does not implement TypeHandler interface");
    }
    @SuppressWarnings( "unchecked" ) // already verified it is a TypeHandler
    Class<? extends TypeHandler<?>> typeHandlerType = (Class<? extends TypeHandler<?>>) type;
    return resolveTypeHandler(javaType, typeHandlerType);
  }

  protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, Class<? extends TypeHandler<?>> typeHandlerType) {
    if (typeHandlerType == null) {
      return null;
    }
    // javaType ignored for injected handlers see issue #746 for full detail
    TypeHandler<?> handler = typeHandlerRegistry.getMappingTypeHandler(typeHandlerType);
    if (handler == null) {
      // not in registry, create a new one
      handler = typeHandlerRegistry.getInstance(javaType, typeHandlerType);
    }
    return handler;
  }

  protected <T> Class<? extends T> resolveAlias(String alias) {
    // 从typeAliasRegistry中，通过别名或类全名，获得对应的类
    return typeAliasRegistry.resolveAlias(alias);
  }
}
