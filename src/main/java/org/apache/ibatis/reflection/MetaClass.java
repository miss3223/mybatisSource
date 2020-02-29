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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * @author Clinton Begin
 *
 * 提供对指定类的各种特殊操作
 */
public class MetaClass {

  private final ReflectorFactory reflectorFactory;
  private final Reflector reflector;

  /**
   * 从该方法中可以看出一个MetaClass对应一个Reflector对象
   * @param type 类
   * @param reflectorFactory 反射工厂
   */
  private MetaClass(Class<?> type, ReflectorFactory reflectorFactory) {
    this.reflectorFactory = reflectorFactory;
    // 获取ReflectorFactory对象
    this.reflector = reflectorFactory.findForClass(type);
  }

  // 创建指定类的MataClass对象
  public static MetaClass forClass(Class<?> type, ReflectorFactory reflectorFactory) {
    return new MetaClass(type, reflectorFactory);
  }

  // 创建类的只从属性的类的MataClass对象
  public MetaClass metaClassForProperty(String name) {
    Class<?> propType = reflector.getGetterType(name);
    return MetaClass.forClass(propType, reflectorFactory);
  }

  // 根据表达式获得属性
  public String findProperty(String name) {
    // 构建属性
    StringBuilder prop = buildProperty(name, new StringBuilder());
    return prop.length() > 0 ? prop.toString() : null;
  }

  /**
   * 根据表达式，获得属性
   * @param name 表达式
   * @param useCamelCaseMapping 是否要下划线转驼峰
   * @return 属性名
   */
  public String findProperty(String name, boolean useCamelCaseMapping) {
    if (useCamelCaseMapping) {
      // 下划线转驼峰
      name = name.replace("_", "");
    }
    // 获得属性
    return findProperty(name);
  }

    /**
     * 获取对象的可读属性数组
     * @return 可读数组
     */
  public String[] getGetterNames() {
    return reflector.getGetablePropertyNames();
  }

    /**
     * 获取对象的可写属性数组
     * @return 可写数组
     */
  public String[] getSetterNames() {
    return reflector.getSetablePropertyNames();
  }

    /**
     * 获得指定属性的setting方法属性的类型
     * @param name 属性值
     * @return 类型
     */
  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaClass metaProp = metaClassForProperty(prop.getName());
      return metaProp.getSetterType(prop.getChildren());
    } else {
      return reflector.getSetterType(prop.getName());
    }
  }

    /**
     * 获得指定属性的getting方法的返回值的类型
     * @param name 属性名
     * @return  返回值类型
     */
  public Class<?> getGetterType(String name) {
    // 创建PropertyTokenizer对象，对name进行分词
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 是否有子表达式
    if (prop.hasNext()) {
      // 创建MetaClass对象
      MetaClass metaProp = metaClassForProperty(prop);
      // 递归判断子表达式children，获得返回值的类型
      return metaProp.getGetterType(prop.getChildren());
    }
    // issue #506. Resolve the type inside a Collection Object
    // 直接获得返回值的类型
    return getGetterType(prop);
  }

    /**
     * 创建MetaClass对象
     * @param prop  属性分词器
     * @return 返回的MetaClass对象
     */
  private MetaClass metaClassForProperty(PropertyTokenizer prop) {
    // 获取getting方法返回的类型
    Class<?> propType = getGetterType(prop);
    // 创建MetaClass对象
    return MetaClass.forClass(propType, reflectorFactory);
  }

    /**
     * 获取getting方法返回的类型
     * @param prop 属性分词器
     * @return
     */
  private Class<?> getGetterType(PropertyTokenizer prop) {
    // 获得返回类型
    Class<?> type = reflector.getGetterType(prop.getName());
    // 如果获取数组的某个位置的元素，则获取其泛型。er：list[0].filed,那么会解析list是什么类型，这样才好通过该类型，继续获得field
    if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {
      // 获得返回的类型
      Type returnType = getGenericGetterType(prop.getName());
      // 如果是泛型，进行解析真正的类型
      if (returnType instanceof ParameterizedType) {
        Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          returnType = actualTypeArguments[0];
          if (returnType instanceof Class) {
            type = (Class<?>) returnType;
          } else if (returnType instanceof ParameterizedType) {
            type = (Class<?>) ((ParameterizedType) returnType).getRawType();
          }
        }
      }
    }
    return type;
  }

    /**
     * 获得返回的类型
     * @param propertyName 属性名称
     * @return 类型
     */
  private Type getGenericGetterType(String propertyName) {
    try {
      // 获得Invoker对象
      Invoker invoker = reflector.getGetInvoker(propertyName);
      // 如果MethodInvoker对象，则说明是getting方法，解析方法返回类型
      if (invoker instanceof MethodInvoker) {
        Field _method = MethodInvoker.class.getDeclaredField("method");
        _method.setAccessible(true);
        Method method = (Method) _method.get(invoker);
        return TypeParameterResolver.resolveReturnType(method, reflector.getType());
      // 如果是GetFieldInvoker对象，则说明是field，直接访问
      } else if (invoker instanceof GetFieldInvoker) {
        Field _field = GetFieldInvoker.class.getDeclaredField("field");
        _field.setAccessible(true);
        Field field = (Field) _field.get(invoker);
        return TypeParameterResolver.resolveFieldType(field, reflector.getType());
      }
    } catch (NoSuchFieldException | IllegalAccessException ignored) {
    }
    return null;
  }

    /**
     * 判断指定的属性是否有setting方法
     * @param name 属性
     * @return 是否有getting属性
     */
  public boolean hasSetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      if (reflector.hasSetter(prop.getName())) {
        MetaClass metaProp = metaClassForProperty(prop.getName());
        return metaProp.hasSetter(prop.getChildren());
      } else {
        return false;
      }
    } else {
      return reflector.hasSetter(prop.getName());
    }
  }

    /**
     * 判断指定属性是否有getting方法
     * @param name 属性名
     * @return  返回是否有
     */
  public boolean hasGetter(String name) {
    // 创建PropertyTokenizer对象，对name进行分词
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 是否有子表达式
    if (prop.hasNext()) {
      // 检查类是否具有按名称可读的属性
      if (reflector.hasGetter(prop.getName())) {
        // 创建MetaClass对象
        MetaClass metaProp = metaClassForProperty(prop);
        // 递归判断子表达式children，是否有getting方法
        return metaProp.hasGetter(prop.getChildren());
      } else {
        return false;
      }
    // 无子表达式
    } else {
      // 判断是否有该属性的getting方法
      return reflector.hasGetter(prop.getName());
    }
  }

    /**
     * 获取属性的getter方法
     * @param name 属性
     * @return 方法
     */
  public Invoker getGetInvoker(String name) {
    return reflector.getGetInvoker(name);
  }

    /**
     * 获取属性的setter方法
     * @param name 属性名
     * @return 方法
     */
  public Invoker getSetInvoker(String name) {
    return reflector.getSetInvoker(name);
  }

  /**
   * 构造属性
   * @param name  名称
   * @param builder 构造
   * @return 属性构造器
   */
  private StringBuilder buildProperty(String name, StringBuilder builder) {
    // 创建PropertyTokenizer对属性进行分词
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 如果有子表达式
    if (prop.hasNext()) {
      // 获得属性名，并添加到builder中
      String propertyName = reflector.findPropertyName(prop.getName());
      if (propertyName != null) {
        // 拼接属性到builder中
        builder.append(propertyName);
        builder.append(".");
        // 创建MetaClass对象
        MetaClass metaProp = metaClassForProperty(propertyName);
        // 递归解析子表达式children，并将结果添加到builder中
        metaProp.buildProperty(prop.getChildren(), builder);
      }
    } else {
      // 获取属性名，并添加到builder中
      String propertyName = reflector.findPropertyName(name);
      if (propertyName != null) {
        builder.append(propertyName);
      }
    }
    return builder;
  }

  public boolean hasDefaultConstructor() {
    return reflector.hasDefaultConstructor();
  }

}
