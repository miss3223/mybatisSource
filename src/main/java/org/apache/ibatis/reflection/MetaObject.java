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
package org.apache.ibatis.reflection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.apache.ibatis.reflection.wrapper.CollectionWrapper;
import org.apache.ibatis.reflection.wrapper.MapWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * @author Clinton Begin
 *
 * 对象元数据，提供了对象的属性值的获取和设置
 */
public class MetaObject {

  // 原始的Object对象
  private final Object originalObject;
  // 封装过的Object对象
  private final ObjectWrapper objectWrapper;
  // 原始的Object工厂
  private final ObjectFactory objectFactory;
  // 封装过的Object工厂
  private final ObjectWrapperFactory objectWrapperFactory;
  // 反射工厂
  private final ReflectorFactory reflectorFactory;

  private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    this.originalObject = object;
    this.objectFactory = objectFactory;
    this.objectWrapperFactory = objectWrapperFactory;
    this.reflectorFactory = reflectorFactory;

    /**
     * 根据Object类型的不同，来创建对应的ObjectWrapper对象
     */
    if (object instanceof ObjectWrapper) {
      this.objectWrapper = (ObjectWrapper) object;
    } else if (objectWrapperFactory.hasWrapperFor(object)) {
      // 创建ObjectWrapper对象
      this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
    } else if (object instanceof Map) {
      // 创建MapWrapper对象
      this.objectWrapper = new MapWrapper(this, (Map) object);
    } else if (object instanceof Collection) {
      // 创建CollectionWrapper对象
      this.objectWrapper = new CollectionWrapper(this, (Collection) object);
    } else {
      // 创建BeanWrapper对象
      this.objectWrapper = new BeanWrapper(this, object);
    }
  }

  /**
   * 创建MetaObject对象
   * @param object 原始Object对象
   * @param objectFactory object工厂
   * @param objectWrapperFactory 封装的object工厂
   * @param reflectorFactory 反射工厂
   * @return  MetaObject对象
   */
  public static MetaObject forObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    if (object == null) {
      // 如果object为null，则返回SystemMetaObject.NULL_META_OBJECT;
      return SystemMetaObject.NULL_META_OBJECT;
    } else {
      return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
    }
  }

  public ObjectFactory getObjectFactory() {
    return objectFactory;
  }

  public ObjectWrapperFactory getObjectWrapperFactory() {
    return objectWrapperFactory;
  }

  public ReflectorFactory getReflectorFactory() {
	return reflectorFactory;
  }

  public Object getOriginalObject() {
    return originalObject;
  }

  public String findProperty(String propName, boolean useCamelCaseMapping) {
    return objectWrapper.findProperty(propName, useCamelCaseMapping);
  }

  public String[] getGetterNames() {
    return objectWrapper.getGetterNames();
  }

  public String[] getSetterNames() {
    return objectWrapper.getSetterNames();
  }

  public Class<?> getSetterType(String name) {
    return objectWrapper.getSetterType(name);
  }

  public Class<?> getGetterType(String name) {
    return objectWrapper.getGetterType(name);
  }

  /** 判断是否有setter方法 */
  public boolean hasSetter(String name) {
    return objectWrapper.hasSetter(name);
  }

  public boolean hasGetter(String name) {
    return objectWrapper.hasGetter(name);
  }

  /**
   * 获取指定属性的值
   *
   * @param name
   * @return
   */
  public Object getValue(String name) {
    // 创建PropertyTokenizer对象
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 是否有子表达式
    if (prop.hasNext()) {
      // 创建MetaObject对象
      MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
      // 递归判断子表达式children，获取值
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return null;
      } else {
        return metaValue.getValue(prop.getChildren());
      }
      //无子表达式
    } else {
      // 获取值
      return objectWrapper.get(prop);
    }
  }

  /**
   * 设置指定属性的指定值
   *
   * @param name 属性名称
   * @param value 值
   */
  public void setValue(String name, Object value) {
    // 创建PropertyTokenizer对象，对name分词
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 是否有子表达式
    if (prop.hasNext()) {
      // 创建MetaObject对象
      MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
      // 递归判断子表达式children，设置值
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        if (value == null) {
          // don't instantiate child path if value is null如果值为空，则不要实例化子路径
          return;
        } else {
          // 创建值
          metaValue = objectWrapper.instantiatePropertyValue(name, prop, objectFactory);
        }
      }
      // 设置值
      metaValue.setValue(prop.getChildren(), value);
    // 无子表达式
    } else {
      // 设置值
      objectWrapper.set(prop, value);
    }
  }

  /**
   * 创建指定属性的MetaObject对象
   *
   * @param name
   * @return
   */
  public MetaObject metaObjectForProperty(String name) {
    // 获取属性值
    Object value = getValue(name);
    // 创建MetaObject对象
    return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
  }

  public ObjectWrapper getObjectWrapper() {
    return objectWrapper;
  }

  // 判断是否为集合
  public boolean isCollection() {
    return objectWrapper.isCollection();
  }

  // 添加元素到集合
  public void add(Object element) {
    objectWrapper.add(element);
  }

  // 添加多个元素到集合
  public <E> void addAll(List<E> list) {
    objectWrapper.addAll(list);
  }

}
