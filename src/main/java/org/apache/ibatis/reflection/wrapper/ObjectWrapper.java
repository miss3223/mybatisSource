/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.reflection.wrapper;

import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * @author Clinton Begin
 *
 * 对象包装器接口，基于MetaClass工具类，定义对指定对象的各种操作
 * ObjectWrapper是MetaClass的指定类的具体化
 * 从接口可以看出主要是对MetaObject方法的调用
 */
public interface ObjectWrapper {

  /**
   * 获取值
   *
   * @param prop  PropertyTokenizer对象，相当于键
   * @return
   */
  Object get(PropertyTokenizer prop);

  /**
   * 设置值
   *
   * @param prop PropertyTokenizer对象，相当于键
   * @param value 值
   */
  void set(PropertyTokenizer prop, Object value);

  /**
   * 获得属性
   *
   * @param name 名称
   * @param useCamelCaseMapping 是否是驼峰
   * @return 返回的属性
   */
  String findProperty(String name, boolean useCamelCaseMapping);

  /**
   * 获取对象的可读属性数组
   * @return 数组
   */
  String[] getGetterNames();

  /**
   * 获取对象的可写属性数组
   * @return 数组
   */
  String[] getSetterNames();

  /**
   *获得指定属性的setting方法属性的类型
   *
   * @param name 属性名称
   * @return 类型
   */
  Class<?> getSetterType(String name);

  /**
   * 获得指定属性的getting方法属性的类型
   *
   * @param name 属性名称
   * @return 类型
   */
  Class<?> getGetterType(String name);

  /**
   * 判断指定的属性是否有setting方法
   *
   * @param name 属性名称
   * @return 是否有
   */
  boolean hasSetter(String name);

  /**
   *  判断指定的属性是否有getting方法
   *
   * @param name 属性名称
   * @return 是否有
   */
  boolean hasGetter(String name);

  /**
   * 创建指定属性的MetaObject对象
   *
   * @param name  属性名称
   * @param prop 属性分词器
   * @param objectFactory Object对象工厂
   * @return
   */
  MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

  // 是否为集合
  boolean isCollection();

  // 添加元素到集合
  void add(Object element);

  // 添加多个元素到集合
  <E> void addAll(List<E> element);

}
