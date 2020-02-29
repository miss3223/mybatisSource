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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;
import org.apache.ibatis.reflection.property.PropertyNamer;

/**
 * This class represents a cached set of class definition information that
 * allows for easy mapping between property names and getter/setter methods.
 *
 * 这个类表示一组缓存的类定义信息，允许在属性名和getter/setter方法之间进行简单的映射。
 * @author Clinton Begin
 */
public class Reflector {

  /**
   * 对应的类
   */
  private final Class<?> type;
  /**
   * 可读属性集合
   */
  private final String[] readablePropertyNames;
  /**
   * 可写属性集合
   */
  private final String[] writeablePropertyNames;
  /**
   * 属性对应的setting方法映射
   *
   * key为属性名称
   * value为Invoker对象
   */
  private final Map<String, Invoker> setMethods = new HashMap<>();
  /**
   * 属性对应的getting方法的映射
   *
   * key为属性名称
   * value为Invoker对象
   */
  private final Map<String, Invoker> getMethods = new HashMap<>();
  /**
   * 属性对应的setting方法的方法参数类型映射
   *
   * key为属性名称
   * value为方法参数类型
   */
  private final Map<String, Class<?>> setTypes = new HashMap<>();
  /**
   * 属性对应的getting方法的方法参数类型映射
   *
   * key为属性名称
   * value为方法参数类型
   */
  private final Map<String, Class<?>> getTypes = new HashMap<>();
  /**
   * 默认构造函数
   */
  private Constructor<?> defaultConstructor;

  /**
   * 不区分大小写的属性集合
   */
  private Map<String, String> caseInsensitivePropertyMap = new HashMap<>();

  /**
   * 构造方法
   * @param clazz 反射的类
   */
  public Reflector(Class<?> clazz) {
    // 设置对应的类
    type = clazz;
    // 初始化默认构造方法
    addDefaultConstructor(clazz);
    // 初始化getMethods和getTypes，通过遍历getting方法
    addGetMethods(clazz);
    // 初始化setMethods和setTypes，通过遍历setting方法
    addSetMethods(clazz);
    // 初始化setMethods+setType和getMethods+getTypes，通过遍历fields属性
    addFields(clazz);
    // 初始化可读、可写、不区分大小写属性集合
    readablePropertyNames = getMethods.keySet().toArray(new String[getMethods.keySet().size()]);
    writeablePropertyNames = setMethods.keySet().toArray(new String[setMethods.keySet().size()]);
    for (String propName : readablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
    for (String propName : writeablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
  }

  /**
   * 初始化默认构造函数
   * @param clazz 反射的类
   */
  private void addDefaultConstructor(Class<?> clazz) {
    // 获取构造函数
    Constructor<?>[] consts = clazz.getDeclaredConstructors();
    // 遍历所有的构造函数
    for (Constructor<?> constructor : consts) {
      // 将构造函数赋值给defaultConstructor
      if (constructor.getParameterTypes().length == 0) {
          this.defaultConstructor = constructor;
      }
    }
  }
  /**
   * 初始化getMethods和getTypes，通过遍历getting方法
   * @param cls 反射的类
   */
  private void addGetMethods(Class<?> cls) {
    // 属性与其getting方法的映射
    Map<String, List<Method>> conflictingGetters = new HashMap<>();
    // 获取所有的方法
    Method[] methods = getClassMethods(cls);
    // 遍历所有的方法
    for (Method method : methods) {
      // 参数大于0说明不是getting方法
      if (method.getParameterTypes().length > 0) {
        continue;
      }
      // 获取方法名称
      String name = method.getName();
      // 以get或者is开头的，说明是getting方法
      if ((name.startsWith("get") && name.length() > 3)
          || (name.startsWith("is") && name.length() > 2)) {
        // 获取属性
        name = PropertyNamer.methodToProperty(name);
        // 添加conflictingGetters中
        addMethodConflict(conflictingGetters, name, method);
      }
    }
    // 解决getting冲突
    resolveGetterConflicts(conflictingGetters);
  }

  private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
    // 遍历每个属性，查找其最匹配的方法，因为子类可以赋写父类的方法，所以一个属性可以对应多个getting方法
    for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
      Method winner = null;//最匹配的方法
      String propName = entry.getKey();
      //winner为空，说明candidate为最匹配的方法
      for (Method candidate : entry.getValue()) {
        if (winner == null) {
          winner = candidate;
          continue;
        }
        // 基于返回类型比较
        Class<?> winnerType = winner.getReturnType();
        Class<?> candidateType = candidate.getReturnType();
        // 类型相同
        if (candidateType.equals(winnerType)) {
          // 返回值不相同，则抛出异常
          if (!boolean.class.equals(candidateType)) {
            throw new ReflectionException(
                "Illegal overloaded getter method with ambiguous type for property "
                    + propName + " in class " + winner.getDeclaringClass()
                    + ". This breaks the JavaBeans specification and can cause unpredictable results.");
            // 选择boolean类型的is方法
          } else if (candidate.getName().startsWith("is")) {
            winner = candidate;
          }
          // 不符合选择子类
        } else if (candidateType.isAssignableFrom(winnerType)) {
          // OK getter type is descendant
          // 符合选择子类，因为子类可以修改放大返回值。例如匪类的一个方法返回的值是List，子类对该方法的返回值可以覆写为ArrayList
        } else if (winnerType.isAssignableFrom(candidateType)) {
          winner = candidate;
          // 返回类型冲突抛出异常
        } else {
          throw new ReflectionException(
              "Illegal overloaded getter method with ambiguous type for property "
                  + propName + " in class " + winner.getDeclaringClass()
                  + ". This breaks the JavaBeans specification and can cause unpredictable results.");
        }
      }
      // 添加到getMethods和getTypes中
      addGetMethod(propName, winner);
    }
  }

  /**
   *
   * @param name
   * @param method
   */
  private void addGetMethod(String name, Method method) {
    // 如果名称是有效的
    if (isValidPropertyName(name)) {
      // 添加到getMethods
      getMethods.put(name, new MethodInvoker(method));
      Type returnType = TypeParameterResolver.resolveReturnType(method, type);
      getTypes.put(name, typeToClass(returnType));
    }
  }

  /**
   *
   * @param cls 回调类
   */
  private void addSetMethods(Class<?> cls) {
    // 属性和setting方法的映射
    Map<String, List<Method>> conflictingSetters = new HashMap<>();
    // 获得所有方法
    Method[] methods = getClassMethods(cls);
    // 遍历所有方法
    for (Method method : methods) {
      // 获取类的名称
      String name = method.getName();
      // 方法以set开头
      // 方法里面有一个参数
      if (name.startsWith("set") && name.length() > 3) {
        if (method.getParameterTypes().length == 1) {
          // 获得属性
          name = PropertyNamer.methodToProperty(name);
          // 添加到conflictingSetters中
          addMethodConflict(conflictingSetters, name, method);
        }
      }
    }
    // 解决setting冲突
    resolveSetterConflicts(conflictingSetters);
  }

  // 将方法添加到冲突list中
  private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
    List<Method> list = conflictingMethods.computeIfAbsent(name, k -> new ArrayList<>());
    list.add(method);
  }

  // 解决冲突
  private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
    // 遍历每个属性，查找其最匹配的方法。
    for (String propName : conflictingSetters.keySet()) {
      List<Method> setters = conflictingSetters.get(propName);
      Class<?> getterType = getTypes.get(propName);
      Method match = null;
      ReflectionException exception = null;
      // 遍历属性对应的setting方法
      for (Method setter : setters) {
        Class<?> paramType = setter.getParameterTypes()[0];
        if (paramType.equals(getterType)) {
          // should be the best match
          match = setter;
          break;
        }
        if (exception == null) {
          try {
            // 选择一个更加匹配的
            match = pickBetterSetter(match, setter, propName);
          } catch (ReflectionException e) {
            // there could still be the 'best match'
            match = null;
            exception = e;
          }
        }
      }
      if (match == null) {
        throw exception;
      } else {
        // 添加到setMethods和setTypes中
        addSetMethod(propName, match);
      }
    }
  }

  /**
   * 选择一个更加匹配的
   * @param setter1  方法1
   * @param setter2  方法2
   * @param property 属性
   * @return 返回的方法
   */
  private Method pickBetterSetter(Method setter1, Method setter2, String property) {
    if (setter1 == null) {
      return setter2;
    }
    Class<?> paramType1 = setter1.getParameterTypes()[0];
    Class<?> paramType2 = setter2.getParameterTypes()[0];
    if (paramType1.isAssignableFrom(paramType2)) {
      return setter2;
    } else if (paramType2.isAssignableFrom(paramType1)) {
      return setter1;
    }
    throw new ReflectionException("Ambiguous setters defined for property '" + property + "' in class '"
        + setter2.getDeclaringClass() + "' with types '" + paramType1.getName() + "' and '"
        + paramType2.getName() + "'.");
  }

  private void addSetMethod(String name, Method method) {
    if (isValidPropertyName(name)) {
      // 将方法添加到setMethods
      setMethods.put(name, new MethodInvoker(method));
      // 添加到setTypes中
      Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
      setTypes.put(name, typeToClass(paramTypes[0]));
    }
  }

  private Class<?> typeToClass(Type src) {
    Class<?> result = null;
    if (src instanceof Class) {
      result = (Class<?>) src;
    } else if (src instanceof ParameterizedType) {
      result = (Class<?>) ((ParameterizedType) src).getRawType();
    } else if (src instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) src).getGenericComponentType();
      if (componentType instanceof Class) {
        result = Array.newInstance((Class<?>) componentType, 0).getClass();
      } else {
        Class<?> componentClass = typeToClass(componentType);
        result = Array.newInstance((Class<?>) componentClass, 0).getClass();
      }
    }
    if (result == null) {
      result = Object.class;
    }
    return result;
  }

  /**
   * 是对addGetMethods和addSetMethods方法的补充
   * 因为有些Field不存在对应的setting和getting方法，所有直接使用对应的field
   * @param clazz 反射的类
   */
  private void addFields(Class<?> clazz) {
    // 获取所有的Fields
    Field[] fields = clazz.getDeclaredFields();
    // 循环遍历fields
    for (Field field : fields) {
      // 如果未添加到setMethods中
      if (!setMethods.containsKey(field.getName())) {
        // issue #379 - removed the check for final because JDK 1.5 allows
        // modification of final fields through reflection (JSR-133). (JGB)
        // pr #16 - final static can only be set by the classloader
        // 获取属性的修饰符
        int modifiers = field.getModifiers();
        // 修饰符不是final的和static的
        if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
          // 添加到setMethods和setTypes中
          addSetField(field);
        }
      }
      // 如果未添加到getMethods中
      if (!getMethods.containsKey(field.getName())) {
        // 添加到getMethods和getMethods中
        addGetField(field);
      }
    }
    // 递归处理父类
    if (clazz.getSuperclass() != null) {
      addFields(clazz.getSuperclass());
    }
  }

  // 添加到setMethods和setTypes中
  private void addSetField(Field field) {
    if (isValidPropertyName(field.getName())) {
      setMethods.put(field.getName(), new SetFieldInvoker(field));
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      setTypes.put(field.getName(), typeToClass(fieldType));
    }
  }

  // 添加到getMethods和getTypes中
  private void addGetField(Field field) {
    if (isValidPropertyName(field.getName())) {
      getMethods.put(field.getName(), new GetFieldInvoker(field));
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      getTypes.put(field.getName(), typeToClass(fieldType));
    }
  }

  /**
   * 判断是否是有效的属性名
   * @param name 属性名称
   * @return 返回true/false
   */
  private boolean isValidPropertyName(String name) {
    // 如果属性名称是以"$"开头，或者等于"serialVersionUID"，或者是class则返回false
    return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
  }

  /**
   * This method returns an array containing all methods
   * declared in this class and any superclass.
   * We use this method, instead of the simpler Class.getMethods(),
   * because we want to look for private methods as well.
   *
   * @param cls The class
   * @return An array containing all methods in this class
   */
  private Method[] getClassMethods(Class<?> cls) {
    // 每个方法签名与该方法的映射
    Map<String, Method> uniqueMethods = new HashMap<>();
    // 循环类，类的父类，类的父类的父类，直到父类的object
    Class<?> currentClass = cls;
    while (currentClass != null && currentClass != Object.class) {
      // 记录当前类定义的方法
      addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());

      // we also need to look for interface methods -
      // because the class may be abstract
      // 定义接口中的方法
      Class<?>[] interfaces = currentClass.getInterfaces();
      for (Class<?> anInterface : interfaces) {
        addUniqueMethods(uniqueMethods, anInterface.getMethods());
      }

      // 获得父类
      currentClass = currentClass.getSuperclass();
    }

    // 转换成Method数组返回
    Collection<Method> methods = uniqueMethods.values();

    return methods.toArray(new Method[methods.size()]);
  }

  /**
   * 记录当前类定义的方法
   * @param uniqueMethods
   * @param methods
   */
  private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
    for (Method currentMethod : methods) {
      // 判断该方法是否是桥接方法
      if (!currentMethod.isBridge()) {
        // 获取方法签名
        String signature = getSignature(currentMethod);
        // check to see if the method is already known
        // if it is known, then an extended class must have
        // overridden a method
       // 当uniqueMethods方法不存在时，添加方法签名
        if (!uniqueMethods.containsKey(signature)) {
          uniqueMethods.put(signature, currentMethod);
        }
      }
    }
  }

  /**
   * 获取方法签名
   * @param method 方法
   * @return 签名
   *
   * 格式：returnType#方法名：参数名1，参数名2，参数名3
   * 例如：void#checkPackageAccess:java.lang.ClassLoader,boolean
   */
  private String getSignature(Method method) {
    StringBuilder sb = new StringBuilder();
    Class<?> returnType = method.getReturnType();
    if (returnType != null) {
      sb.append(returnType.getName()).append('#');
    }
    // 添加方法名
    sb.append(method.getName());
    // 获取方法参数
    Class<?>[] parameters = method.getParameterTypes();
    for (int i = 0; i < parameters.length; i++) {
      if (i == 0) {
        sb.append(':');
      } else {
        sb.append(',');
      }
      sb.append(parameters[i].getName());
    }
    return sb.toString();
  }

  /**
   * Checks whether can control member accessible.
   *
   * 检查是否有权限
   *
   * @return If can control member accessible, it return {@literal true}
   * @since 3.5.0
   */
  public static boolean canControlMemberAccessible() {
    try {
      SecurityManager securityManager = System.getSecurityManager();
      if (null != securityManager) {
        securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
      }
    } catch (SecurityException e) {
      return false;
    }
    return true;
  }

  /**
   * Gets the name of the class the instance provides information for
   * 获取实例提供信息的类的名称
   * @return The class name
   */
  public Class<?> getType() {
    return type;
  }

  public Constructor<?> getDefaultConstructor() {
    if (defaultConstructor != null) {
      return defaultConstructor;
    } else {
      throw new ReflectionException("There is no default constructor for " + type);
    }
  }

  // 是否有默认构造器
  public boolean hasDefaultConstructor() {
    return defaultConstructor != null;
  }

  /**
   * 获取调用者
   */
  public Invoker getSetInvoker(String propertyName) {
    Invoker method = setMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
    }
    return method;
  }

    /**
     * 属性的getter方法
     * @param propertyName 属性
     * @return 方法
     */
  public Invoker getGetInvoker(String propertyName) {
    Invoker method = getMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
    }
    return method;
  }

  /**
   * Gets the type for a property setter
   *
   * @param propertyName - the name of the property
   * @return The Class of the property setter
   */
  public Class<?> getSetterType(String propertyName) {
    Class<?> clazz = setTypes.get(propertyName);
    if (clazz == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
    }
    return clazz;
  }

  /**
   * Gets the type for a property getter
   *
   * @param propertyName - the name of the property
   * @return The Class of the property getter
   */
  public Class<?> getGetterType(String propertyName) {
    Class<?> clazz = getTypes.get(propertyName);
    if (clazz == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
    }
    return clazz;
  }

  /**
   * Gets an array of the readable properties for an object
   * 获取对象的可读属性数组
   * @return The array
   */
  public String[] getGetablePropertyNames() {
    return readablePropertyNames;
  }

  /**
   * Gets an array of the writable properties for an object
   * 获取对象的可写属性数组
   * @return The array
   */
  public String[] getSetablePropertyNames() {
    return writeablePropertyNames;
  }

  /**
   * Check to see if a class has a writable property by name
   *
   * @param propertyName - the name of the property to check
   * @return True if the object has a writable property by the name
   */
  public boolean hasSetter(String propertyName) {
    return setMethods.keySet().contains(propertyName);
  }

  /**
   * Check to see if a class has a readable property by name
   *
   * @param propertyName - the name of the property to check
   * @return True if the object has a readable property by the name
   *
   * 检查类是否具有按名称可读的属性
   */
  public boolean hasGetter(String propertyName) {
    return getMethods.keySet().contains(propertyName);
  }

  // 查找属性名称
  public String findPropertyName(String name) {
    // 不区分大小写的属性结合
    return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
  }
}
