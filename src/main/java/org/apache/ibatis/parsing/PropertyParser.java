/**
 *    Copyright 2009-2016 the original author or authors.
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
package org.apache.ibatis.parsing;

import java.util.Properties;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class PropertyParser {

  private static final String KEY_PREFIX = "org.apache.ibatis.parsing.PropertyParser.";
  /**
   * The special property key that indicate whether enable a default value on placeholder.
   * <p>
   *   The default value is {@code false} (indicate disable a default value on placeholder)
   *   If you specify the {@code true}, you can specify key and default value on placeholder (e.g. {@code ${db.username:postgres}}).
   * </p>
   * @since 3.4.2
   */
  public static final String KEY_ENABLE_DEFAULT_VALUE = KEY_PREFIX + "enable-default-value";

  /**
   * The special property key that specify a separator for key and default value on placeholder.
   * <p>
   *   The default separator is {@code ":"}.
   * </p>
   * @since 3.4.2
   */
  public static final String KEY_DEFAULT_VALUE_SEPARATOR = KEY_PREFIX + "default-value-separator";

  private static final String ENABLE_DEFAULT_VALUE = "false";
  private static final String DEFAULT_VALUE_SEPARATOR = ":";

  private PropertyParser() {
    // Prevent Instantiation
  }

  /**
   * 将动态值替换为属性变量
   * @param string  动态值
   * @param variables 属性变量
   * @return
   */
  public static String parse(String string, Properties variables) {
    // 创建 VariableTokenHandler对象
    VariableTokenHandler handler = new VariableTokenHandler(variables);
    // 创建GenericTokenParser对象
    GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
    // 执行解析
    return parser.parse(string);
  }

  /**
   * TokenHandler有四个实x现方法，只有VariableTokenHandler是有解析器模块相关的
   *
   */
  private static class VariableTokenHandler implements TokenHandler {
    // Properties对象
    private final Properties variables;
    // 是否开启默认值功能
    private final boolean enableDefaultValue;
    // 默认值的分隔符
    private final String defaultValueSeparator;

    private VariableTokenHandler(Properties variables) {
      this.variables = variables;
      // 设置是否开启默认值功能
      /**
       * 默认情况下是不开启的，如果想要开启则需要设置
       * <properties resource="org/mybatis/example/config.properties">
       *   <property name="org.apache.ibatis.parsing.PropertyParser.enable-default-value" value="true"/>
       * </properties>
        */
      this.enableDefaultValue = Boolean.parseBoolean(getPropertyValue(KEY_ENABLE_DEFAULT_VALUE, ENABLE_DEFAULT_VALUE));
      /**
       * 设置是否使用默认的分隔符"："，如果需要自己重新设置则为
       * <properties resource="org/mybatis/example/config.properties">
       *   <property name="org.apache.ibatis.parsing.PropertyParser.default-value-separator" value="?:"/>
       * </properties>
        */
      this.defaultValueSeparator = getPropertyValue(KEY_DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR);
    }

    /**
     *  如果Properties对象为null,则不开启
     *  如果Properties对象不为null，则获取Properties对象中给定key的value值，如果value值为null，则使用默认值
      */


    private String getPropertyValue(String key, String defaultValue) {
      return (variables == null) ? defaultValue : variables.getProperty(key, defaultValue);
    }

    /***
     * 一个在properties中的例子
     * @param content Token字符串
     * @return 返回的值
     */

    @Override
    public String handleToken(String content) {
      if (variables != null) {
        String key = content;
        // 开启默认值功能
        if (enableDefaultValue) {
          // 查找默认值
          final int separatorIndex = content.indexOf(defaultValueSeparator);
          String defaultValue = null;
          if (separatorIndex >= 0) {
            key = content.substring(0, separatorIndex);
            defaultValue = content.substring(separatorIndex + defaultValueSeparator.length());
          }
          // 有默认值优先替换，不存在则返回默认值
          if (defaultValue != null) {
            // 如果存在默认值的话，则根据key值去查看value，如果value==null,就使用默认值，如果value！= null，使用返回的value值。
            return variables.getProperty(key, defaultValue);
          }
        }
        // 未开启默认值功能，直接替换
        if (variables.containsKey(key)) {
          return variables.getProperty(key);
        }
      }
      // 无variables直接返回
      return "${" + content + "}";
    }
  }

}
