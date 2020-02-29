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

import java.util.List;
import java.util.Locale;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 *
 * StatementXML配置构建器，主要负责解析Statement配置，即crud标签的
 */
public class XMLStatementBuilder extends BaseBuilder {

  /** Mapper构造器的小助手 */
  private final MapperBuilderAssistant builderAssistant;

  /** 当前xml节点 */
  private final XNode context;

  /** 当前的 databaseId */
  private final String requiredDatabaseId;

  public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context) {
    this(configuration, builderAssistant, context, null);
  }

  public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context, String databaseId) {
    super(configuration);
    this.builderAssistant = builderAssistant;
    this.context = context;
    this.requiredDatabaseId = databaseId;
  }

  /**
   * 执行Statement解析
   */
  public void parseStatementNode() {
    // 获得id属性，编号
    String id = context.getStringAttribute("id");
    // 获得databaseId，判断databaseId是否匹配
    String databaseId = context.getStringAttribute("databaseId");

    if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
      return;
    }

    // 获得各种属性
    Integer fetchSize = context.getIntAttribute("fetchSize");
    Integer timeout = context.getIntAttribute("timeout");
    String parameterMap = context.getStringAttribute("parameterMap");
    String parameterType = context.getStringAttribute("parameterType");
    Class<?> parameterTypeClass = resolveClass(parameterType);
    String resultMap = context.getStringAttribute("resultMap");
    String resultType = context.getStringAttribute("resultType");
    String lang = context.getStringAttribute("lang");
    // 获得lang对应的LanguageDriver
    LanguageDriver langDriver = getLanguageDriver(lang);

    // 获得resultType对应的类
    Class<?> resultTypeClass = resolveClass(resultType);

    // 获得resultSet对应的枚举
    String resultSetType = context.getStringAttribute("resultSetType");
    StatementType statementType = StatementType.valueOf(context.getStringAttribute("statementType", StatementType.PREPARED.toString()));
    // 获得statementType对应的枚举值
    ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);

    // 获得SQL对应的SqlCommandType枚举值
    String nodeName = context.getNode().getNodeName();
    SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
    // 获得各种属性
    boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
    boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
    boolean useCache = context.getBooleanAttribute("useCache", isSelect);
    boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);

    // Include Fragments before parsing
    // 创建XMLIncludeTransformer对象，并替换<include/>标签相关的内容
    XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
    includeParser.applyIncludes(context.getNode());

    // Parse selectKey after includes and remove them.
    //selectKey节点用于支持数据库比如Oracle不支持自动生成主键，或者可能JDBC驱动不支持自动生成主键时的情况
    // 解析<selectKey />标签
    processSelectKeyNodes(id, parameterTypeClass, langDriver);
    
    // Parse the SQL (pre: <selectKey> and <include> were parsed and removed)
    // 创建SqlSource
    SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass);
    // 获得resultSets，resultSets，keyColumn属性的值
    String resultSets = context.getStringAttribute("resultSets");
    String keyProperty = context.getStringAttribute("resultSets");
    String keyColumn = context.getStringAttribute("keyColumn");
    // 获得KeyGenerator对象
    KeyGenerator keyGenerator;
    // 优先，从 configuration 中获得 KeyGenerator 对象。如果存在，意味着是 <selectKey /> 标签配置的
    String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
    keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);
    if (configuration.hasKeyGenerator(keyStatementId)) {
      keyGenerator = configuration.getKeyGenerator(keyStatementId);
    } else {
      // 其次，根据标签属性的情况，判断是否使用对应的Jdbc3KeyGenerator或者NoKeyGenerator对象
      keyGenerator = context.getBooleanAttribute("useGeneratedKeys",
          configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType))
          ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
    }

    // 创建MappedStatement对象
    builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
        fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
        resultSetTypeEnum, flushCache, useCache, resultOrdered, 
        keyGenerator, keyProperty, keyColumn, databaseId, langDriver, resultSets);
  }

  /** 解析<selectKey /> 标签 */
  private void processSelectKeyNodes(String id, Class<?> parameterTypeClass, LanguageDriver langDriver) {
    // 获得<selectKey />节点
    List<XNode> selectKeyNodes = context.evalNodes("selectKey");
    if (configuration.getDatabaseId() != null) {
      // 执行解析<selectKey />
      parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, configuration.getDatabaseId());
    }
    parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, null);
    // 移除<selectKey />节点
    removeSelectKeyNodes(selectKeyNodes);
  }

  /** 执行解析<selectKey /> */
  private void parseSelectKeyNodes(String parentId, List<XNode> list, Class<?> parameterTypeClass, LanguageDriver langDriver, String skRequiredDatabaseId) {
    // 遍历<selectKey />节点
    for (XNode nodeToHandle : list) {
      // 获得完整id，格式是"${id}!selectKey"
      String id = parentId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
      // 获得databaseId，判断databaseId是否匹配
      String databaseId = nodeToHandle.getStringAttribute("databaseId");
      if (databaseIdMatchesCurrent(id, databaseId, skRequiredDatabaseId)) {
        // 执行单个<selectKey />节点
        parseSelectKeyNode(id, nodeToHandle, parameterTypeClass, langDriver, databaseId);
      }
    }
  }

  /**
   *
   * @param id id属性
   * @param nodeToHandle 节点
   * @param parameterTypeClass 参数类型
   * @param langDriver languageDriver
   * @param databaseId 厂商id
   * 解析的过程
   * 1、加载相关属性
   * 2、使用语言驱动器创建SqlSource
   * 3、SelectKey在实现上内部和其他的CRUD一样，被当做一个MappedStatement进行存储；
   */

  private void parseSelectKeyNode(String id, XNode nodeToHandle, Class<?> parameterTypeClass, LanguageDriver langDriver, String databaseId) {
   // 加载相关属性以及获得对应的类
    String resultType = nodeToHandle.getStringAttribute("resultType");
    Class<?> resultTypeClass = resolveClass(resultType);
    StatementType statementType = StatementType.valueOf(nodeToHandle.getStringAttribute("statementType", StatementType.PREPARED.toString()));
    String keyProperty = nodeToHandle.getStringAttribute("keyProperty");
    String keyColumn = nodeToHandle.getStringAttribute("keyColumn");
    boolean executeBefore = "BEFORE".equals(nodeToHandle.getStringAttribute("order", "AFTER"));

    //defaults
    // 创建MappedStatement 需要用到的默认值
    boolean useCache = false;
    boolean resultOrdered = false;
    KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
    Integer fetchSize = null;
    Integer timeout = null;
    boolean flushCache = false;
    String parameterMap = null;
    String resultMap = null;
    ResultSetType resultSetTypeEnum = null;

    // 使用语言驱动器创建SqlSource
    SqlSource sqlSource = langDriver.createSqlSource(configuration, nodeToHandle, parameterTypeClass);
    SqlCommandType sqlCommandType = SqlCommandType.SELECT;

    //SelectKey在实现上内部和其他的CRUD一样，被当做一个MappedStatement进行存储
    builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
        fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
        resultSetTypeEnum, flushCache, useCache, resultOrdered,
        keyGenerator, keyProperty, keyColumn, databaseId, langDriver, null);

    // 获得SelectKeyGenerator的编号，格式为 `${namespace}.${id}`
    id = builderAssistant.applyCurrentNamespace(id, false);

    // 创建语句参数映射
    MappedStatement keyStatement = configuration.getMappedStatement(id, false);
    // 创建 SelectKeyGenerator 对象，并添加到 configuration 中
    configuration.addKeyGenerator(id, new SelectKeyGenerator(keyStatement, executeBefore));
  }

  private void removeSelectKeyNodes(List<XNode> selectKeyNodes) {
    for (XNode nodeToHandle : selectKeyNodes) {
      nodeToHandle.getParent().getNode().removeChild(nodeToHandle.getNode());
    }
  }

  /** 判断databaseId是否匹配  在xmlMapperBuilder中已经解析过了*/
  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    if (requiredDatabaseId != null) {
      if (!requiredDatabaseId.equals(databaseId)) {
        return false;
      }
    } else {
      if (databaseId != null) {
        return false;
      }
      // skip this statement if there is a previous one with a not null databaseId
      id = builderAssistant.applyCurrentNamespace(id, false);
      if (this.configuration.hasStatement(id, false)) {
        MappedStatement previous = this.configuration.getMappedStatement(id, false); // issue #2
        if (previous.getDatabaseId() != null) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * 获得LanguageDriver
   * @param lang 属性
   * @return  LanguageDriver
   */
  private LanguageDriver getLanguageDriver(String lang) {
    // 解析lang对应的类
    Class<? extends LanguageDriver> langClass = null;
    if (lang != null) {
      langClass = resolveClass(lang);
    }
    // 获得LanguageDriver对象
    return builderAssistant.getLanguageDriver(langClass);
  }

}
