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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.CacheBuilder;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 *
 * Mapper构造器的小助手，提供了一些公用的方法，例如创建ParameterMap和MappedStatement对象等等
 */
public class MapperBuilderAssistant extends BaseBuilder {

  /** 当前mapper的命名空间 */
  private String currentNamespace;

  /** 资源引用的地址 */
  private final String resource;

  /** 当前Cache对象 */
  private Cache currentCache;

  /** 是否解析成功Cache引用 */
  private boolean unresolvedCacheRef; // issue #676

  public MapperBuilderAssistant(Configuration configuration, String resource) {
    super(configuration);
    ErrorContext.instance().resource(resource);
    this.resource = resource;
  }

  public String getCurrentNamespace() {
    return currentNamespace;
  }

  /**
   * 设置当前命名空间
   * @param currentNamespace 当前命名空间
   */
  public void setCurrentNamespace(String currentNamespace) {
    // 如果传入的 currentNamespace 参数为空，抛出 BuilderException 异常
    if (currentNamespace == null) {
      throw new BuilderException("The mapper element requires a namespace attribute to be specified.");
    }

    // 如果当前已经设置，并且还和传入的不相等，抛出 BuilderException 异常
    if (this.currentNamespace != null && !this.currentNamespace.equals(currentNamespace)) {
      throw new BuilderException("Wrong namespace. Expected '"
          + this.currentNamespace + "' but found '" + currentNamespace + "'.");
    }

    // 设置
    this.currentNamespace = currentNamespace;
  }

  /**
   * 拼接命名空间
   */
  public String applyCurrentNamespace(String base, boolean isReference) {
    if (base == null) {
      return null;
    }
    if (isReference) {
      // is it qualified with any namespace yet?
      if (base.contains(".")) {
        return base;
      }
    } else {
      // is it qualified with this namespace yet?
      if (base.startsWith(currentNamespace + ".")) {
        return base;
      }
      if (base.contains(".")) {
        throw new BuilderException("Dots are not allowed in element names, please remove it from " + base);
      }
    }
    // 拼接命名空间
    return currentNamespace + "." + base;
  }

  /**
   * 获得指向Cache 的对象
   * @param namespace 命名空间
   * @return Cache对象
   */
  public Cache useCacheRef(String namespace) {
    if (namespace == null) {
      throw new BuilderException("cache-ref element requires a namespace attribute.");
    }
    try {
      // 标记未解决
      unresolvedCacheRef = true;
      // 获取Cache对象
      Cache cache = configuration.getCache(namespace);
      if (cache == null) {
        throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
      }
      // 记录当前cache
      currentCache = cache;
      // 标记已解决
      unresolvedCacheRef = false;
      return cache;
    } catch (IllegalArgumentException e) {
      throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.", e);
    }
  }

  /** 创建Cache对象 */
  public Cache useNewCache(Class<? extends Cache> typeClass,
      Class<? extends Cache> evictionClass,
      Long flushInterval,
      Integer size,
      boolean readWrite,
      boolean blocking,
      Properties props) {
    // 创建Cache对象
    Cache cache = new CacheBuilder(currentNamespace)
        .implementation(valueOrDefault(typeClass, PerpetualCache.class))
        .addDecorator(valueOrDefault(evictionClass, LruCache.class))
        .clearInterval(flushInterval)
        .size(size)
        .readWrite(readWrite)
        .blocking(blocking)
        .properties(props)
        .build();
    // 在configuration添加中添加cache
    configuration.addCache(cache);
    // 赋值给当前cache
    currentCache = cache;
    return cache;
  }

  public ParameterMap addParameterMap(String id, Class<?> parameterClass, List<ParameterMapping> parameterMappings) {
    id = applyCurrentNamespace(id, false);
    ParameterMap parameterMap = new ParameterMap.Builder(configuration, id, parameterClass, parameterMappings).build();
    configuration.addParameterMap(parameterMap);
    return parameterMap;
  }

  public ParameterMapping buildParameterMapping(
      Class<?> parameterType,
      String property,
      Class<?> javaType,
      JdbcType jdbcType,
      String resultMap,
      ParameterMode parameterMode,
      Class<? extends TypeHandler<?>> typeHandler,
      Integer numericScale) {

    // 拼接命名空间
    resultMap = applyCurrentNamespace(resultMap, true);

    // Class parameterType = parameterMapBuilder.type();
    // 解析对应的JavaType 和TypeHandler对象
    Class<?> javaTypeClass = resolveParameterJavaType(parameterType, property, javaType, jdbcType);
    TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);

    //创建ParameterMapping对象
    return new ParameterMapping.Builder(configuration, property, javaTypeClass)
        .jdbcType(jdbcType)
        .resultMapId(resultMap)
        .mode(parameterMode)
        .numericScale(numericScale)
        .typeHandler(typeHandlerInstance)
        .build();
  }

  /** 创建resultMap对象，并添加到configuration对象中 */
  public ResultMap addResultMap(
      String id,
      Class<?> type,
      String extend,
      Discriminator discriminator,
      List<ResultMapping> resultMappings,
      Boolean autoMapping) {
    //  获得ResultMap编号 即${namespace}.${id}
    id = applyCurrentNamespace(id, false);
    // 获取完整的extend属性，即${namespace}.${extend}
    extend = applyCurrentNamespace(extend, true);

    // 如果有父类，则将父类的 ResultMap结合，添加到resultMappings中
    if (extend != null) {
      // 获得 extend 对应的 ResultMap 对象。如果不存在，则抛出 IncompleteElementException 异常
      if (!configuration.hasResultMap(extend)) {
        throw new IncompleteElementException("Could not find a parent resultmap with id '" + extend + "'");
      }
      //  获得 extend 对应的 ResultMap 对象的ResultMapping集合，并移除ResultMappings。
      ResultMap resultMap = configuration.getResultMap(extend);
      List<ResultMapping> extendedResultMappings = new ArrayList<>(resultMap.getResultMappings());
      extendedResultMappings.removeAll(resultMappings);
      // Remove parent constructor if this resultMap declares a constructor.
      // 判断当前的ResultMappings是否有构造方法，如果有，则从extendedResultMappings移除所有的构造类型的ResultMapping
      boolean declaresConstructor = false;
      for (ResultMapping resultMapping : resultMappings) {
        if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
          declaresConstructor = true;
          break;
        }
      }
      if (declaresConstructor) {
        Iterator<ResultMapping> extendedResultMappingsIter = extendedResultMappings.iterator();
        while (extendedResultMappingsIter.hasNext()) {
          if (extendedResultMappingsIter.next().getFlags().contains(ResultFlag.CONSTRUCTOR)) {
            extendedResultMappingsIter.remove();
          }
        }
      }
      // 将extendedResultMappings添加到resultMappings中
      resultMappings.addAll(extendedResultMappings);
    }
    // 创建ResultMap对象
    ResultMap resultMap = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping)
        .discriminator(discriminator)
        .build();
    // 将resultMap添加到configuration中
    configuration.addResultMap(resultMap);
    return resultMap;
  }

  /** 构建Discriminator -- > 鉴别器 对象 */
  public Discriminator buildDiscriminator(
      Class<?> resultType,
      String column,
      Class<?> javaType,
      JdbcType jdbcType,
      Class<? extends TypeHandler<?>> typeHandler,
      Map<String, String> discriminatorMap) {
    // 构建ResultMapping对象
    ResultMapping resultMapping = buildResultMapping(
        resultType,
        null,
        column,
        javaType,
        jdbcType,
        null,
        null,
        null,
        null,
        typeHandler,
        new ArrayList<ResultFlag>(),
        null,
        null,
        false);
    //创建namespaceDiscriminatorMap映射
    Map<String, String> namespaceDiscriminatorMap = new HashMap<>();
    for (Map.Entry<String, String> e : discriminatorMap.entrySet()) {
      String resultMap = e.getValue();
      // 生成resultMap标识
      resultMap = applyCurrentNamespace(resultMap, true);
      namespaceDiscriminatorMap.put(e.getKey(), resultMap);
    }
    // 构建Discriminator对象
    return new Discriminator.Builder(configuration, resultMapping, namespaceDiscriminatorMap).build();
  }

  /** 构建MappedStatement对象 */
  public MappedStatement addMappedStatement(
      String id,
      SqlSource sqlSource,
      StatementType statementType,
      SqlCommandType sqlCommandType,
      Integer fetchSize,
      Integer timeout,
      String parameterMap,
      Class<?> parameterType,
      String resultMap,
      Class<?> resultType,
      ResultSetType resultSetType,
      boolean flushCache,
      boolean useCache,
      boolean resultOrdered,
      KeyGenerator keyGenerator,
      String keyProperty,
      String keyColumn,
      String databaseId,
      LanguageDriver lang,
      String resultSets) {

    // 如果没有成功解析Cache引用，则抛出异常
    if (unresolvedCacheRef) {
      throw new IncompleteElementException("Cache-ref not yet resolved");
    }

    // 获得id编号，格式为`${namespace}.${id}`
    id = applyCurrentNamespace(id, false);
    boolean isSelect = sqlCommandType == SqlCommandType.SELECT;

    // 创建MappedStatement.Builder 对象对象
    MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType)
        .resource(resource)
        .fetchSize(fetchSize)
        .timeout(timeout)
        .statementType(statementType)
        .keyGenerator(keyGenerator)
        .keyProperty(keyProperty)
        .keyColumn(keyColumn)
        .databaseId(databaseId)
        .lang(lang)
        .resultOrdered(resultOrdered)
        .resultSets(resultSets)
        .resultMaps(getStatementResultMaps(resultMap, resultType, id))
        .resultSetType(resultSetType)
        .flushCacheRequired(valueOrDefault(flushCache, !isSelect))
        .useCache(valueOrDefault(useCache, isSelect))
        .cache(currentCache);
    // 获得ParameterMap，并设置到 MappedStatement.Builder 中
    ParameterMap statementParameterMap = getStatementParameterMap(parameterMap, parameterType, id);
    if (statementParameterMap != null) {
      statementBuilder.parameterMap(statementParameterMap);
    }

    // 创建MappedStatement对象
    MappedStatement statement = statementBuilder.build();
    // 将MappedStatement添加到configuration中
    configuration.addMappedStatement(statement);
    return statement;
  }

  private <T> T valueOrDefault(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  private ParameterMap getStatementParameterMap(
      String parameterMapName,
      Class<?> parameterTypeClass,
      String statementId) {
    parameterMapName = applyCurrentNamespace(parameterMapName, true);
    ParameterMap parameterMap = null;
    if (parameterMapName != null) {
      try {
        parameterMap = configuration.getParameterMap(parameterMapName);
      } catch (IllegalArgumentException e) {
        throw new IncompleteElementException("Could not find parameter map " + parameterMapName, e);
      }
    } else if (parameterTypeClass != null) {
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      parameterMap = new ParameterMap.Builder(
          configuration,
          statementId + "-Inline",
          parameterTypeClass,
          parameterMappings).build();
    }
    return parameterMap;
  }

  private List<ResultMap> getStatementResultMaps(
      String resultMap,
      Class<?> resultType,
      String statementId) {
    resultMap = applyCurrentNamespace(resultMap, true);

    List<ResultMap> resultMaps = new ArrayList<>();
    if (resultMap != null) {
      String[] resultMapNames = resultMap.split(",");
      for (String resultMapName : resultMapNames) {
        try {
          resultMaps.add(configuration.getResultMap(resultMapName.trim()));
        } catch (IllegalArgumentException e) {
          throw new IncompleteElementException("Could not find result map " + resultMapName, e);
        }
      }
    } else if (resultType != null) {
      ResultMap inlineResultMap = new ResultMap.Builder(
          configuration,
          statementId + "-Inline",
          resultType,
          new ArrayList<ResultMapping>(),
          null).build();
      resultMaps.add(inlineResultMap);
    }
    return resultMaps;
  }

  public ResultMapping buildResultMapping(
      Class<?> resultType,
      String property,
      String column,
      Class<?> javaType,
      JdbcType jdbcType,
      String nestedSelect,
      String nestedResultMap,
      String notNullColumn,
      String columnPrefix,
      Class<? extends TypeHandler<?>> typeHandler,
      List<ResultFlag> flags,
      String resultSet,
      String foreignColumn,
      boolean lazy) {
    // 解析赌赢的Java Type 类和TypeHandler对象
    Class<?> javaTypeClass = resolveResultJavaType(resultType, property, javaType);
    TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);
    // 解析组合字段名称成ResultMapping集合
    List<ResultMapping> composites = parseCompositeColumnName(column);
    // 创建ResultMapping对象
    return new ResultMapping.Builder(configuration, property, column, javaTypeClass)
        .jdbcType(jdbcType)
        .nestedQueryId(applyCurrentNamespace(nestedSelect, true))
        .nestedResultMapId(applyCurrentNamespace(nestedResultMap, true))
        .resultSet(resultSet)
        .typeHandler(typeHandlerInstance)
        .flags(flags == null ? new ArrayList<ResultFlag>() : flags)
        .composites(composites)
        .notNullColumns(parseMultipleColumnNames(notNullColumn))
        .columnPrefix(columnPrefix)
        .foreignColumn(foreignColumn)
        .lazy(lazy)
        .build();
  }

  /**
   * 将字符串解析成集合
   *
   * @param columnName 字段名称
   * @return
   */
  private Set<String> parseMultipleColumnNames(String columnName) {
    Set<String> columns = new HashSet<>();
    if (columnName != null) {
      if (columnName.indexOf(',') > -1) {
        // StringTokenizer使用不同分隔符来分隔字符串
        StringTokenizer parser = new StringTokenizer(columnName, "{}, ", false);
        while (parser.hasMoreTokens()) {
          String column = parser.nextToken();
          columns.add(column);
        }
      } else {
        columns.add(columnName);
      }
    }
    return columns;
  }

  /**
   * 解析组合字段名称成ResultMapping 集合
   * @param columnName 字段名称
   * @return 集合
   */
  private List<ResultMapping> parseCompositeColumnName(String columnName) {
    List<ResultMapping> composites = new ArrayList<>();
    // 分词，解析其中的property和column的组合对
    if (columnName != null && (columnName.indexOf('=') > -1 || columnName.indexOf(',') > -1)) {
      StringTokenizer parser = new StringTokenizer(columnName, "{}=, ", false);
      while (parser.hasMoreTokens()) {
        String property = parser.nextToken();
        String column = parser.nextToken();
        // 创建ResultMapping对象
        ResultMapping complexResultMapping = new ResultMapping.Builder(
            configuration, property, column, configuration.getTypeHandlerRegistry().getUnknownTypeHandler()).build();
        // 添加到composites中
        composites.add(complexResultMapping);
      }
    }
    return composites;
  }

  private Class<?> resolveResultJavaType(Class<?> resultType, String property, Class<?> javaType) {
    if (javaType == null && property != null) {
      try {
        MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
        javaType = metaResultType.getSetterType(property);
      } catch (Exception e) {
        //ignore, following null check statement will deal with the situation
      }
    }
    if (javaType == null) {
      javaType = Object.class;
    }
    return javaType;
  }

  private Class<?> resolveParameterJavaType(Class<?> resultType, String property, Class<?> javaType, JdbcType jdbcType) {
    if (javaType == null) {
      if (JdbcType.CURSOR.equals(jdbcType)) {
        javaType = java.sql.ResultSet.class;
      } else if (Map.class.isAssignableFrom(resultType)) {
        javaType = Object.class;
      } else {
        MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
        javaType = metaResultType.getGetterType(property);
      }
    }
    if (javaType == null) {
      javaType = Object.class;
    }
    return javaType;
  }

  /** Backward compatibility signature */
  public ResultMapping buildResultMapping(
      Class<?> resultType,
      String property,
      String column,
      Class<?> javaType,
      JdbcType jdbcType,
      String nestedSelect,
      String nestedResultMap,
      String notNullColumn,
      String columnPrefix,
      Class<? extends TypeHandler<?>> typeHandler,
      List<ResultFlag> flags) {
      return buildResultMapping(
        resultType, property, column, javaType, jdbcType, nestedSelect,
        nestedResultMap, notNullColumn, columnPrefix, typeHandler, flags, null, null, configuration.isLazyLoadingEnabled());
  }

  /** 获得LanguageDriver 对象*/
  public LanguageDriver getLanguageDriver(Class<? extends LanguageDriver> langClass) {
    // 获得langClass类
    if (langClass != null) {
      configuration.getLanguageRegistry().register(langClass);
    } else {
      langClass = configuration.getLanguageRegistry().getDefaultDriverClass();
    }
    return configuration.getLanguageRegistry().getDriver(langClass);
  }

  /** Backward compatibility signature */
  public MappedStatement addMappedStatement(
    String id,
    SqlSource sqlSource,
    StatementType statementType,
    SqlCommandType sqlCommandType,
    Integer fetchSize,
    Integer timeout,
    String parameterMap,
    Class<?> parameterType,
    String resultMap,
    Class<?> resultType,
    ResultSetType resultSetType,
    boolean flushCache,
    boolean useCache,
    boolean resultOrdered,
    KeyGenerator keyGenerator,
    String keyProperty,
    String keyColumn,
    String databaseId,
    LanguageDriver lang) {
    return addMappedStatement(
      id, sqlSource, statementType, sqlCommandType, fetchSize, timeout,
      parameterMap, parameterType, resultMap, resultType, resultSetType,
      flushCache, useCache, resultOrdered, keyGenerator, keyProperty,
      keyColumn, databaseId, lang, null);
  }

}
