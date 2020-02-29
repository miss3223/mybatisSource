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
package org.apache.ibatis.scripting.xmltags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Clinton Begin
 *
 * XML 动态语句( SQL )构建器，负责将 SQL 解析成 SqlSource 对象。
 */
public class XMLScriptBuilder extends BaseBuilder {

  /** 当前SQL的XNode对象*/
  private final XNode context;

  /** 是否为动态语句 */
  private boolean isDynamic;

  /** SQL 方法类型 */
  private final Class<?> parameterType;

  /** NodeHandler的映射 */
  private final Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();

  public XMLScriptBuilder(Configuration configuration, XNode context) {
    this(configuration, context, null);
  }

  public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType) {
    super(configuration);
    this.context = context;
    this.parameterType = parameterType;
    // 初始化NodeHandlerMap属性
    initNodeHandlerMap();
  }


  private void initNodeHandlerMap() {
    nodeHandlerMap.put("trim", new TrimHandler());
    nodeHandlerMap.put("where", new WhereHandler());
    nodeHandlerMap.put("set", new SetHandler());
    nodeHandlerMap.put("foreach", new ForEachHandler());
    nodeHandlerMap.put("if", new IfHandler());
    nodeHandlerMap.put("choose", new ChooseHandler());
    nodeHandlerMap.put("when", new IfHandler());
    nodeHandlerMap.put("otherwise", new OtherwiseHandler());
    nodeHandlerMap.put("bind", new BindHandler());
  }

  /** 负责将SQL解析成SqlSource对象 */
  public SqlSource parseScriptNode() {
    //解析动态标签
    MixedSqlNode rootSqlNode = parseDynamicTags(context);
    // 创建SqlSource对象
    SqlSource sqlSource = null;
    //加载动态SQL
    if (isDynamic) {
      sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
    } else {
      //加载静态SQL
      sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType);
    }
    return sqlSource;
  }

  /** 解析 SQL 解析成 MixedSqlNode 对象 */
  protected MixedSqlNode parseDynamicTags(XNode node) {
    // 创建SqlNode数组
    List<SqlNode> contents = new ArrayList<>();
    // 遍历SQL节点的所有子节点
    NodeList children = node.getNode().getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      // 当前子节点
      XNode child = node.newXNode(children.item(i));
      // 如果类型是Node.CDATA_SECTION_NODE 或者 Node.TEXT_NODE 时
      if (child.getNode().getNodeType() == Node.CDATA_SECTION_NODE || child.getNode().getNodeType() == Node.TEXT_NODE) {
        // 得到内容
        String data = child.getStringBody("");
        // 创建TextSqlNode对象
        TextSqlNode textSqlNode = new TextSqlNode(data);
        // 判断文本节点中是否包含了${}，如果包含则为动态文本节点，否则为静态文本节点，静态文本节点在运行时不需要二次处理
        /**
         * 在如下代码中做的判断
         * private GenericTokenParser createParser(TokenHandler handler) {
         *     return new GenericTokenParser("${", "}", handler);
         *   }
         */
        // 如果是动态TextSqlNode对象
        if (textSqlNode.isDynamic()) {
          // 添加到contents中
          contents.add(textSqlNode);
          // 标记为动态Sql
          isDynamic = true;
          // 如果是非动态的TextSqlNode对象
        } else {
          //  创建 StaticTextSqlNode 添加到 contents 中
          contents.add(new StaticTextSqlNode(data));
        }
        // 如果节点类型是ELEMENT_NODE的
      } else if (child.getNode().getNodeType() == Node.ELEMENT_NODE) { // issue #628 标签节点
        // 根据子节点的标签，获得对应的NodeHandler对象
        String nodeName = child.getNode().getNodeName();
        // 首先根据节点名称获取到对应的节点处理器
        NodeHandler handler = nodeHandlerMap.get(nodeName);
        // 获得不到，说明是未知的标签，抛出 BuilderException 异常
        if (handler == null) {
          throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement.");
        }
        // 使用对应的节点处理器处理本节点
        handler.handleNode(child, contents);
        isDynamic = true;
      }
    }
    return new MixedSqlNode(contents);
  }

  /** Node处理器接口 */
  private interface NodeHandler {

    /**
     * 处理Node
     *
     * @param nodeToHandle 要处理的XNode节点
     * @param targetContents 目标SqlNode数组
     */
    void handleNode(XNode nodeToHandle, List<SqlNode> targetContents);
  }

  /**
   * 实现NodeHandler接口，<bind />标签的处理器
   * */
  private class BindHandler implements NodeHandler {
    public BindHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      // 解析name和value
      final String name = nodeToHandle.getStringAttribute("name");
      final String expression = nodeToHandle.getStringAttribute("value");
      // 创建VarDeclSqlNode对象
      final VarDeclSqlNode node = new VarDeclSqlNode(name, expression);
      // 添加到targetContents中
      targetContents.add(node);
    }
  }

  /**
   * <trim />标签的处理器
   *
   *   trim使用最多的情况是截掉where条件中的前置OR和AND，update的set子句中的后置”,”，同时在内容不为空的时候加上where和set。
   */

  private class TrimHandler implements NodeHandler {
    public TrimHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      //包含的子节点解析后SQL文本不为空时要添加的前缀内容
      String prefix = nodeToHandle.getStringAttribute("prefix");
      // 要覆盖的子节点解析后SQL文本前缀内容
      String prefixOverrides = nodeToHandle.getStringAttribute("prefixOverrides");
      // 包含的子节点解析后SQL文本不为空时要添加的后缀内容
      String suffix = nodeToHandle.getStringAttribute("suffix");
      // 要覆盖的子节点解析后SQL文本后缀内容
      String suffixOverrides = nodeToHandle.getStringAttribute("suffixOverrides");
      TrimSqlNode trim = new TrimSqlNode(configuration, mixedSqlNode, prefix, prefixOverrides, suffix, suffixOverrides);
      targetContents.add(trim);
    }
  }

  /**
   * <where />标签的处理器
   */
  private class WhereHandler implements NodeHandler {
    public WhereHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      // 解析内部的SQL节点，成mixedSqlNode对象
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      // 创建WhereSqlNode对象
      WhereSqlNode where = new WhereSqlNode(configuration, mixedSqlNode);
      // 添加到targetContents中
      targetContents.add(where);
    }
  }

  /**
   * <set />标签的处理器
   */
  private class SetHandler implements NodeHandler {
    public SetHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      SetSqlNode set = new SetSqlNode(configuration, mixedSqlNode);
      targetContents.add(set);
    }
  }

  /**
   * <foreach />标签的处理器
   */
  private class ForEachHandler implements NodeHandler {
    public ForEachHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);

      String collection = nodeToHandle.getStringAttribute("collection");
      //本次迭代获取的元素
      String item = nodeToHandle.getStringAttribute("item");
      //当前迭代的次数
      String index = nodeToHandle.getStringAttribute("index");
      String open = nodeToHandle.getStringAttribute("open");
      String close = nodeToHandle.getStringAttribute("close");
      String separator = nodeToHandle.getStringAttribute("separator");
      ForEachSqlNode forEachSqlNode = new ForEachSqlNode(configuration, mixedSqlNode, collection, index, item, open, close, separator);
      targetContents.add(forEachSqlNode);
    }
  }

  /**
   *  <if />标签的处理器
   */
  private class IfHandler implements NodeHandler {
    public IfHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      String test = nodeToHandle.getStringAttribute("test");
      IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
      targetContents.add(ifSqlNode);
    }
  }
  /**
   * otherwise标签可以说并不是一个真正有意义的标签，它不做任何处理，用在choose标签的最后默认分支
   * choose节点应该说和switch是等价的，其中的when就是各种条件判断
   *  <otherwise />标签的处理器
   */
  private class OtherwiseHandler implements NodeHandler {
    public OtherwiseHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      targetContents.add(mixedSqlNode);
    }
  }

  /**
   * <choose />标签的处理器
   */
  private class ChooseHandler implements NodeHandler {
    public ChooseHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      List<SqlNode> whenSqlNodes = new ArrayList<>();
      List<SqlNode> otherwiseSqlNodes = new ArrayList<>();
      // 拆分出when 和 otherwise 节点
      handleWhenOtherwiseNodes(nodeToHandle, whenSqlNodes, otherwiseSqlNodes);
      SqlNode defaultSqlNode = getDefaultSqlNode(otherwiseSqlNodes);
      ChooseSqlNode chooseSqlNode = new ChooseSqlNode(whenSqlNodes, defaultSqlNode);
      targetContents.add(chooseSqlNode);
    }

    private void handleWhenOtherwiseNodes(XNode chooseSqlNode, List<SqlNode> ifSqlNodes, List<SqlNode> defaultSqlNodes) {
      List<XNode> children = chooseSqlNode.getChildren();
      for (XNode child : children) {
        String nodeName = child.getNode().getNodeName();
        NodeHandler handler = nodeHandlerMap.get(nodeName);
        if (handler instanceof IfHandler) {
          handler.handleNode(child, ifSqlNodes);
        } else if (handler instanceof OtherwiseHandler) {
          handler.handleNode(child, defaultSqlNodes);
        }
      }
    }

    private SqlNode getDefaultSqlNode(List<SqlNode> defaultSqlNodes) {
      SqlNode defaultSqlNode = null;
      if (defaultSqlNodes.size() == 1) {
        defaultSqlNode = defaultSqlNodes.get(0);
      } else if (defaultSqlNodes.size() > 1) {
        throw new BuilderException("Too many default (otherwise) elements in choose statement.");
      }
      return defaultSqlNode;
    }
  }

}
