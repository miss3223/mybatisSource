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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 *
 * Mapper XML配置构造器，主要负责解析Mapper映射配置文件
 */
public class XMLMapperBuilder extends BaseBuilder {

  /** Java XPath 解析器 */
  private final XPathParser parser;

  /** Mapper 构造器助手 */
  private final MapperBuilderAssistant builderAssistant;

  /**
   * ：<sql id="userColumns"> ${alias}.id,${alias}.username,${alias}.password </sql>
   * 可被其他语句引用的可重用语句块的集合
   * eg：
   * */
  private final Map<String, XNode> sqlFragments;

  /** 资源引用地址 */
  private final String resource;

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(reader, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(inputStream, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    super(configuration);
    // 创建MapperBuilderAssistant对象
    this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
    this.parser = parser;
    this.sqlFragments = sqlFragments;
    this.resource = resource;
  }

  /**
   * 解析**Mapper.xml文件
   */
  public void parse() {
    // 判断当前Mapper是否已经加载过
    if (!configuration.isResourceLoaded(resource)) {
      // 解析<mapper/>节点
      configurationElement(parser.evalNode("/mapper"));
      // 标记当前Mapper已经加载过
      configuration.addLoadedResource(resource);
      // 绑定mapper
      bindMapperForNamespace();
    }

    // 解析待定的<resultMap />节点
    parsePendingResultMaps();
    // 解析待定的<cache-ref />节点
    parsePendingCacheRefs();
    // 解析待定的SQL语句节点
    parsePendingStatements();
  }

  public XNode getSqlFragment(String refid) {
    return sqlFragments.get(refid);
  }

  /**
   * 解析<mapper/>节点
   * @param context
   */
  private void configurationElement(XNode context) {
    try {
      // 获取namespace属性
      String namespace = context.getStringAttribute("namespace");
      if (namespace == null || namespace.equals("")) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      // 在构造器助手中设置namespace
      builderAssistant.setCurrentNamespace(namespace);
      // 解析<cache-ref />节点
      cacheRefElement(context.evalNode("cache-ref"));
      // 解析<cache/>节点
      cacheElement(context.evalNode("cache"));
      // 用的很少
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      // <resultMap />节点
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      // <sql /> 节点
      sqlElement(context.evalNodes("/mapper/sql"));
      //解析CRUD
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }

  /**
   * 解析CRUD
   * @param list
   */
  private void buildStatementFromContext(List<XNode> list) {

    // 如果DatabaseId存在，则使用该id代表的数据库厂商来解析，不存在则使用默认的
    if (configuration.getDatabaseId() != null) {
      buildStatementFromContext(list, configuration.getDatabaseId());
    }
    buildStatementFromContext(list, null);
  }

  /**
   * 解析CRUD
   *
   * @param list  节点内容
   * @param requiredDatabaseId 数据库厂商id
   */
  private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
    // 遍历遍历 <select /> <insert /> <update /> <delete />节点
    for (XNode context : list) {
      // 创建XMLStatementBuilder对象，执行解析 XMLStatementBuilder该类是重点掌握内容
      final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
      try {
        // 执行Statement的解析
        statementParser.parseStatementNode();
      } catch (IncompleteElementException e) {
        // 解析失败，添加到configuration中
        configuration.addIncompleteStatement(statementParser);
      }
    }
  }

  /**      一下三个解析待定的，即使在解析失败以后，放入在configuration.getIncompleteXX()方法里面的，对于解析失败的语句，会再进行一次解析*/


  /** 解析待定的<resultMap />节点 */
  private void parsePendingResultMaps() {
    // 获取ResultMapResolver集合，并遍历进行处理
    Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
    synchronized (incompleteResultMaps) {
      Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
      while (iter.hasNext()) {
        try {
          // 执行解析
          iter.next().resolve();
          // 移除
          iter.remove();
        } catch (IncompleteElementException e) {
          // 解析失败不抛出异常
          // ResultMap is still missing a resource...
        }
      }
    }
  }

  /**  解析待定的<cache-ref />节点 */
  private void parsePendingCacheRefs() {
    // 获得CacheRefResolver集合，并遍历进行处理
    Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
    synchronized (incompleteCacheRefs) {
      Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
      while (iter.hasNext()) {
        try {
          // 执行解析
          iter.next().resolveCacheRef();
          // 移除
          iter.remove();
        } catch (IncompleteElementException e) {
          // Cache ref is still missing a resource...
        }
      }
    }
  }
/** 解析待定的SQL语句节点 */
  private void parsePendingStatements() {
    Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
    synchronized (incompleteStatements) {
      Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().parseStatementNode();
          iter.remove();
        } catch (IncompleteElementException e) {
          // Statement is still missing a resource...
        }
      }
    }
  }

  /**
   * 解析<cache-ref />节点
   * @param context 节点内容
   */
  private void cacheRefElement(XNode context) {
    if (context != null) {
      /**
       * 获得指向的namespace名字，并添加到configuration的cacheRefMap中
       * eg：
       * <cache-ref namespace="com.someone.application.data.SomeMapper"/>
       * */
      configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
      // 创建CacheRefResolver对象，并执行解析
      CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
      try {
        // 解析cache
        cacheRefResolver.resolveCacheRef();
      } catch (IncompleteElementException e) {
        // 解析失败，添加到configuration的IncompleteCacheRefs中
        configuration.addIncompleteCacheRef(cacheRefResolver);
      }
    }
  }

  /**
   * 解析<cache />节点
   * @param context 节点内容
   * @throws Exception 异常
   *
   * 使用自定义缓存
   * <cache type="com.domain.something.MyCustomCache">
   *   <property name="cacheFile" value="/tmp/my-custom-cache.tmp"/>
   * </cache>
   */
  private void cacheElement(XNode context) throws Exception {
    if (context != null) {
      // 获得负责存储的Cache实现类
      String type = context.getStringAttribute("type", "PERPETUAL");
      Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
      // TODO 获得负责过去的Cache实现类  过期策略应该是有四个的，为什么这里直接使用的是LRU ？？？？
      String eviction = context.getStringAttribute("eviction", "LRU");
      Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
      // 获得flushInterval、size、readWrite、blocking属性
      Long flushInterval = context.getLongAttribute("flushInterval");
      Integer size = context.getIntAttribute("size");
      boolean readWrite = !context.getBooleanAttribute("readOnly", false);
      boolean blocking = context.getBooleanAttribute("blocking", false);
      // 获取Properties属性
      Properties props = context.getChildrenAsProperties();
      // 创建Cache对象
      builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    }
  }

  // 这个可能要被废弃，暂时先不解析说明
  private void parameterMapElement(List<XNode> list) throws Exception {
    for (XNode parameterMapNode : list) {
      String id = parameterMapNode.getStringAttribute("id");
      String type = parameterMapNode.getStringAttribute("type");
      Class<?> parameterClass = resolveClass(type);
      List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      for (XNode parameterNode : parameterNodes) {
        String property = parameterNode.getStringAttribute("property");
        String javaType = parameterNode.getStringAttribute("javaType");
        String jdbcType = parameterNode.getStringAttribute("jdbcType");
        String resultMap = parameterNode.getStringAttribute("resultMap");
        String mode = parameterNode.getStringAttribute("mode");
        String typeHandler = parameterNode.getStringAttribute("typeHandler");
        Integer numericScale = parameterNode.getIntAttribute("numericScale");
        ParameterMode modeEnum = resolveParameterMode(mode);
        Class<?> javaTypeClass = resolveClass(javaType);
        JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
        @SuppressWarnings("unchecked")
        Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
        ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property, javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
        parameterMappings.add(parameterMapping);
      }
      builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
    }
  }

  /** 解析<resultMap /> 节点 */
  private void resultMapElements(List<XNode> list) throws Exception {
    // 遍历<resultMap />节点
    for (XNode resultMapNode : list) {
      try {
        // 处理单个<resultMap />节点
        resultMapElement(resultMapNode);
      } catch (IncompleteElementException e) {
        // ignore, it will be retried
        // 在内部实现中将未完成的元素添加到configuration.incomplete中了
      }
    }
  }

  /**
   * 处理单个resultMap节点
   *
   * @param resultMapNode resultMap节点
   * @return ResultMap
   * @throws Exception 异常
   */
  private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
    return resultMapElement(resultMapNode, Collections.<ResultMapping> emptyList());
  }

  /** 解析 <resultMap /> 节点  */
  private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings) throws Exception {
    ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
    // 获取id属性
    String id = resultMapNode.getStringAttribute("id",
        resultMapNode.getValueBasedIdentifier());
    // 获取type属性
    String type = resultMapNode.getStringAttribute("type",
        resultMapNode.getStringAttribute("ofType",
            resultMapNode.getStringAttribute("resultType",
                resultMapNode.getStringAttribute("javaType"))));
    // 获取extends属性
    String extend = resultMapNode.getStringAttribute("extends");
    // 获取autoMapping属性
    Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
    // 解析type对应的类
    Class<?> typeClass = resolveClass(type);
    Discriminator discriminator = null;
    // 创建ResultMapping集合
    List<ResultMapping> resultMappings = new ArrayList<>();
    resultMappings.addAll(additionalResultMappings);
    // 遍历<resultMap />的子节点
    List<XNode> resultChildren = resultMapNode.getChildren();
    for (XNode resultChild : resultChildren) {
      // 处理<constructor />节点
      if ("constructor".equals(resultChild.getName())) {
        processConstructorElement(resultChild, typeClass, resultMappings);
      // 处理 <discriminator/>节点
      } else if ("discriminator".equals(resultChild.getName())) {
        discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
      // 处理其他节点
      } else {
        List<ResultFlag> flags = new ArrayList<>();
        if ("id".equals(resultChild.getName())) {
          flags.add(ResultFlag.ID);
        }
        resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
      }
    }
    // 创建ResultMapResolver对象
    ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
    try {
      return resultMapResolver.resolve();
    } catch (IncompleteElementException  e) {
      // 解析失败，添加到configuration中
      configuration.addIncompleteResultMap(resultMapResolver);
      throw e;
    }
  }

  /**
   * 处理resultMap中的<constructor/>
   * @param resultChild resultMap中的子节点
   * @param resultType 类型
   * @param resultMappings resultMap的映射resultMappings
   * @throws Exception 异常
   */
  private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    // 遍历<constructor/>的子节点
    List<XNode> argChildren = resultChild.getChildren();
    for (XNode argChild : argChildren) {
      // 获得ResultFlag 集合
      List<ResultFlag> flags = new ArrayList<>();
      flags.add(ResultFlag.CONSTRUCTOR);
      if ("idArg".equals(argChild.getName())) {
        flags.add(ResultFlag.ID);
      }
      // 将当前子节点构建成resultMapping对象，并添加到resultMappings中
      resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
    }
  }

  private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    // 解析各种属性
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String typeHandler = context.getStringAttribute("typeHandler");
    // 解析各种属性对应的类
    Class<?> javaTypeClass = resolveClass(javaType);
    @SuppressWarnings("unchecked")
    Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    // 遍历<Discriminator />的子节点，解析成discriminatorMap集合
    Map<String, String> discriminatorMap = new HashMap<>();
    for (XNode caseChild : context.getChildren()) {
      String value = caseChild.getStringAttribute("value");
      String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings));
      discriminatorMap.put(value, resultMap);
    }
    // 创建Discriminator对象
    return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
  }

  /** 解析<sql />节点*/
  private void sqlElement(List<XNode> list) throws Exception {
    if (configuration.getDatabaseId() != null) {
      sqlElement(list, configuration.getDatabaseId());
    }
    sqlElement(list, null);
  }

  private void sqlElement(List<XNode> list, String requiredDatabaseId) throws Exception {
    // 遍历所有的<sql />节点
    for (XNode context : list) {
      // 获得databaseId属性
      String databaseId = context.getStringAttribute("databaseId");
      // 获得完整的id属性，格式为${namespace}.${id}
      String id = context.getStringAttribute("id");
      id = builderAssistant.applyCurrentNamespace(id, false);
      // 判断databaseId是否匹配
      if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
        // 添加到 sqlFragments 中
        sqlFragments.put(id, context);
      }
    }
  }

  /**
   * 判断databaseId是否匹配
   * @param id  完整的id属性
   * @param databaseId   databaseId属性
   * @param requiredDatabaseId 提供的databaseId
   * @return
   */
  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    // 不匹配则返回false
    if (requiredDatabaseId != null) {
      if (!requiredDatabaseId.equals(databaseId)) {
        return false;
      }
    } else {
      // 如果未设置requiredDatabaseId，但是databaseId存在，返回false
      if (databaseId != null) {
        return false;
      }
      // skip this fragment if there is a previous one with a not null databaseId
      if (this.sqlFragments.containsKey(id)) {
        XNode context = this.sqlFragments.get(id);
        // 若存在，则判断原有的 sqlFragment 是否 databaseId 为空。
        // 因为，当前 databaseId 为空，这样两者才能匹配。
        if (context.getStringAttribute("databaseId") != null) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * 将对象构建成resultMapping对象
   * @param context 需要构建的节点
   * @param resultType 类型
   * @param flags  ID/CONSTRUCTOR
   * @return ResultMapping
   * @throws Exception 异常
   */
  private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) throws Exception {
    // 获得各种属性
    String property;
    if (flags.contains(ResultFlag.CONSTRUCTOR)) {
      property = context.getStringAttribute("name");
    } else {
      property = context.getStringAttribute("property");
    }
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String nestedSelect = context.getStringAttribute("select");
    String nestedResultMap = context.getStringAttribute("resultMap",
        processNestedResultMappings(context, Collections.<ResultMapping> emptyList()));
    String notNullColumn = context.getStringAttribute("notNullColumn");
    String columnPrefix = context.getStringAttribute("columnPrefix");
    String typeHandler = context.getStringAttribute("typeHandler");
    String resultSet = context.getStringAttribute("resultSet");
    String foreignColumn = context.getStringAttribute("foreignColumn");
    boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
    Class<?> javaTypeClass = resolveClass(javaType);
    @SuppressWarnings("unchecked")
    // 获得各种属性对应的类
    Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    // 构建ResultMapping 对象
    return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
  }

  /** 解析内嵌的ResultMap */
  private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings) throws Exception {
    if ("association".equals(context.getName())
        || "collection".equals(context.getName())
        || "case".equals(context.getName())) {
      if (context.getStringAttribute("select") == null) {
        // 解析返回resultMap
        ResultMap resultMap = resultMapElement(context, resultMappings);
        return resultMap.getId();
      }
    }
    return null;
  }

  /** 绑定Mapper */
  private void bindMapperForNamespace() {
    // 获取命名空间
    String namespace = builderAssistant.getCurrentNamespace();
    if (namespace != null) {
      // 获得Mapper映射配置文件对应的Mapper接口，实际上类名就是namespace
      Class<?> boundType = null;
      try {
        boundType = Resources.classForName(namespace);
      } catch (ClassNotFoundException e) {
        //ignore, bound type is not required
      }
      if (boundType != null) {
        // 不窜在该Mapper接口，则进行添加
        if (!configuration.hasMapper(boundType)) {
          // Spring may not know the real resource name so we set a flag
          // to prevent loading again this resource from the mapper interface
          // look at MapperAnnotationBuilder#loadXmlResource
          // 标价namespace已经添加，避免MapperAnnotationBuilder#loadXmlResource(）重复添加
          configuration.addLoadedResource("namespace:" + namespace);
          // 添加到configuration
          configuration.addMapper(boundType);
        }
      }
    }
  }

}
