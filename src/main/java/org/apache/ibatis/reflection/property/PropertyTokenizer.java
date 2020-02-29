/**
 *    Copyright 2009-2017 the original author or authors.
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
package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * @author Clinton Begin
 *
 * 属性分词器，支持迭代器的访问方式
 */
public class PropertyTokenizer implements Iterator<PropertyTokenizer> {
  // 当前字符串
  private String name;
  // 索引名称
  private final String indexedName;
  /**
   * 索引编号
   *
   * 对于数组name[0],则index = 0
   * 对于Map map[key],则，index = key
   */
  private String index;
  // 剩余字符串
  private final String children;

  public PropertyTokenizer(String fullname) {
    // 初始化name，children字符串，使用.作为分隔
    int delim = fullname.indexOf('.');
    // 如果有.分隔的，说明有子串
    if (delim > -1) {
      //分隔的前面为名称
      name = fullname.substring(0, delim);
      // 分隔的后面为子串的名称
      children = fullname.substring(delim + 1);
    } else {
      name = fullname;
      children = null;
    }
    // 记录当前的名称
    indexedName = name;
    // 若存在[,则获得index，并修改name
    delim = name.indexOf('[');
    if (delim > -1) {
      // 获取[]里面的内容
      index = name.substring(delim + 1, name.length() - 1);
      // 获取名称
      name = name.substring(0, delim);
    }
  }

  public String getName() {
    return name;
  }

  public String getIndex() {
    return index;
  }

  public String getIndexedName() {
    return indexedName;
  }

  public String getChildren() {
    return children;
  }

  @Override
  public boolean hasNext() {
    return children != null;
  }

  // 迭代获取下一个PropertyTokenizer对象
  @Override
  public PropertyTokenizer next() {
    return new PropertyTokenizer(children);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
  }
}
