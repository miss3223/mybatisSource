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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Frank D. Martinez [mnesarco]
 *
 * xml中的<include />标签的转换器，负责将SQL中的<include />标签转换成对应的<sql />的内容
 */
public class XMLIncludeTransformer {

  /** Configuration对象*/
  private final Configuration configuration;
  /** mappe构造器助手r*/
  private final MapperBuilderAssistant builderAssistant;

  public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
    this.configuration = configuration;
    this.builderAssistant = builderAssistant;
  }

  /**
   * 将<include />标签，替换成引用的<sql />
   * @param source 节点内容
   */
  public void applyIncludes(Node source) {
    // 创建Properties对象，并从configuration对象里面的Variables提取出来
    Properties variablesContext = new Properties();
    Properties configurationVariables = configuration.getVariables();
    if (configurationVariables != null) {
      variablesContext.putAll(configurationVariables);
    }
    applyIncludes(source, variablesContext, false);
  }

  /**
   * Recursively apply includes through all SQL fragments.
   * @param source Include node in DOM tree
   * @param variablesContext Current context for static variables with values
   *  将节点分为include、非include 和文本节点
   *
   *
   * 子递归逻辑，对照如下的例子，进行解析，可以更加的清楚
   *
   *  // mybatis-config.xml
   *
   * <properties>
   *     <property name="cpu" value="16c" />
   *     <property name="target_sql" value="123" />
   * </properties>
   *
   * // Mapper.xml
   *
   * <sql id="123" lang="${cpu}">
   *     ${cpu}
   *     aoteman
   *     qqqq
   * </sql>
   *
   * <select id="testForInclude">
   *     SELECT * FROM subject
   *     <include refid="${target_sql}" />
   * </select>
   *
   */
  private void applyIncludes(Node source, final Properties variablesContext, boolean included) {
    /**
     * 如果包含include
     * 则将sql替换掉include
     *  <sql id=”userColumns”> id,username,password </sql>
     *     <select id=”selectUsers” parameterType=”int” resultType=”hashmap”>
     *         select <include refid=”userColumns”/>
     *         from some_table
     *         where id = #{id}
     *     </select>
     *     替换成
     * <select id=”selectUsers” parameterType=”int” resultType=”hashmap”>
     *         select id,username,password
     *         from some_table
     *         where id = #{id}
     *     </select>
     */
    // 如果是<include />标签
    if (source.getNodeName().equals("include")) {
      // 获得<sql />对应的节点
      Node toInclude = findSqlFragment(getStringAttribute(source, "refid"), variablesContext);
      // 获得包含<include />标签内的属性
      Properties toIncludeContext = getVariablesContext(source, variablesContext);
      // 递归调用applyIncludes(...),继续替换
      applyIncludes(toInclude, toIncludeContext, true);
      if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
        toInclude = source.getOwnerDocument().importNode(toInclude, true);
      }
      // 将<include />节点替换成<sql />节点的内容
      source.getParentNode().replaceChild(toInclude, source);
      // 将<sql />子节点添加到<sql />节点的后面
      while (toInclude.hasChildNodes()) {
        toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
      }
      // 移除<include />标签自身
      toInclude.getParentNode().removeChild(toInclude);
      // 如果节点类型为Node.ELEMENT_NODE
    } else if (source.getNodeType() == Node.ELEMENT_NODE) {
      //首先判断是否为根节点，如果是非根且变量上下文不为空，则先解析属性值上的占位符。
      //然后对于子节点，递归进行调用直到所有节点都为文本节点为止。
      if (included && !variablesContext.isEmpty()) {
        // replace variables in attribute values
        // 如果在<include /> 标签中存在这变量值${value}.则将该变量值替换了
        NamedNodeMap attributes = source.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
          Node attr = attributes.item(i);
          attr.setNodeValue(PropertyParser.parse(attr.getNodeValue(), variablesContext));
        }
      }
      // 遍历子节点,递归替换
      NodeList children = source.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        applyIncludes(children.item(i), variablesContext, included);
      }
      //如果在处理 <include /> 标签中，并且节点类型为 Node.TEXT_NODE ，并且变量非空，则进行变量的替换，并修改原节点 source
    } else if (included && source.getNodeType() == Node.TEXT_NODE
        && !variablesContext.isEmpty()) {
      // replace variables in text node
      //如果是文本节点，并且变量不为空，就将变量赋值
      source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
    }
  }


  /**
   *  查找sql片段 即<sql />节点里面的内容
   */

  private Node findSqlFragment(String refid, Properties variables) {
    // 因为refid可能是动态变量，所以要进行替换
    refid = PropertyParser.parse(refid, variables);
    // 获得完整的refid,格式是"${namespace}.${refid}"
    refid = builderAssistant.applyCurrentNamespace(refid, true);
    try {
      // 获得对应的<sql />节点
      XNode nodeToInclude = configuration.getSqlFragments().get(refid);
      // 获得node节点，进行克隆
      return nodeToInclude.getNode().cloneNode(true);
    } catch (IllegalArgumentException e) {
      throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
    }
  }

  private String getStringAttribute(Node node, String name) {
    return node.getAttributes().getNamedItem(name).getNodeValue();
  }

  /**
   * Read placeholders and their values from include node definition. 
   * @param node Include node instance
   * @param inheritedVariablesContext Current context used for replace variables in new variables values
   * @return variables context from include instance (no inherited values)
   *
   * 获得<include />标签内的属性值
   */
  private Properties getVariablesContext(Node node, Properties inheritedVariablesContext) {
    // 获得<include />d标签的属性集合
    Map<String, String> declaredProperties = null;
    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        String name = getStringAttribute(n, "name");
        // Replace variables inside
        String value = PropertyParser.parse(getStringAttribute(n, "value"), inheritedVariablesContext);
        if (declaredProperties == null) {
          declaredProperties = new HashMap<>();
        }
        if (declaredProperties.put(name, value) != null) {
          throw new BuilderException("Variable " + name + " defined twice in the same include definition");
        }
      }
    }
    // 如果标签内没有属性，直接使用inheritedVariablesContext
    if (declaredProperties == null) {
      return inheritedVariablesContext;
      // 如果标签内有属性，则重新创建一个Properties对象，包含inheritedVariablesContext+declaredProperties
    } else {
      Properties newProperties = new Properties();
      newProperties.putAll(inheritedVariablesContext);
      newProperties.putAll(declaredProperties);
      return newProperties;
    }
  }
}
