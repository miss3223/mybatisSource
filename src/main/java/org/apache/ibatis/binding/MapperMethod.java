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
package org.apache.ibatis.binding;

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 * @author Kazuki Shimizu
 *
 * Mapper方法，在Mapper接口中，每个定义的方法，对应一个MapperMethod对象
 * MapperMethod的作用：
 * 1、解析Mapper接口的方法，并封装成MapperMethod对象
 * 2、将SQL命令，正确路由到恰当的SqlSession的方法上
 */
public class MapperMethod {

  /**
   * sqlCommand对象
   * 保存了Sql命令的类型和键id
   * */
  private final SqlCommand command;

  /**
   * MethodSignature对象
   * 保存了Mapper接口方法的解析信息
   * */
  private final MethodSignature method;

  public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
    this.command = new SqlCommand(config, mapperInterface, method);
    this.method = new MethodSignature(config, mapperInterface, method);
  }

  /**
   * sql的执行过程
   *
   * 根据解析结果，路由到恰当的SqlSession方法上
   *
   * @param sqlSession sqlSession对象
   * @param args 参数
   * @return Object对象
   */
  public Object execute(SqlSession sqlSession, Object[] args) {
    Object result;
    switch (command.getType()) {
      case INSERT: {
    	Object param = method.convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.insert(command.getName(), param));
        break;
      }
      case UPDATE: {
        Object param = method.convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.update(command.getName(), param));
        break;
      }
      case DELETE: {
        Object param = method.convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.delete(command.getName(), param));
        break;
      }
      case SELECT:
        if (method.returnsVoid() && method.hasResultHandler()) {
          executeWithResultHandler(sqlSession, args);
          result = null;
        } else if (method.returnsMany()) {
          result = executeForMany(sqlSession, args);
        } else if (method.returnsMap()) {
          result = executeForMap(sqlSession, args);
        } else if (method.returnsCursor()) {
          result = executeForCursor(sqlSession, args);
        } else {
          Object param = method.convertArgsToSqlCommandParam(args);
          result = sqlSession.selectOne(command.getName(), param);
          if (method.returnsOptional() &&
              (result == null || !method.getReturnType().equals(result.getClass()))) {
            result = Optional.ofNullable(result);
          }
        }
        break;
      case FLUSH:
        result = sqlSession.flushStatements();
        break;
      default:
        throw new BindingException("Unknown execution method for: " + command.getName());
    }
    if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
      throw new BindingException("Mapper method '" + command.getName() 
          + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
    }
    return result;
  }

  private Object rowCountResult(int rowCount) {
    final Object result;
    if (method.returnsVoid()) {
      result = null;
    } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
      result = rowCount;
    } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
      result = (long)rowCount;
    } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
      result = rowCount > 0;
    } else {
      throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
    }
    return result;
  }

  private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
    MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
    if (!StatementType.CALLABLE.equals(ms.getStatementType())
        && void.class.equals(ms.getResultMaps().get(0).getType())) {
      throw new BindingException("method " + command.getName() 
          + " needs either a @ResultMap annotation, a @ResultType annotation," 
          + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
    }
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
    } else {
      sqlSession.select(command.getName(), param, method.extractResultHandler(args));
    }
  }

  private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
    List<E> result;
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.<E>selectList(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.<E>selectList(command.getName(), param);
    }
    // issue #510 Collections & arrays support
    if (!method.getReturnType().isAssignableFrom(result.getClass())) {
      if (method.getReturnType().isArray()) {
        return convertToArray(result);
      } else {
        return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
      }
    }
    return result;
  }

  private <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
    Cursor<T> result;
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.<T>selectCursor(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.<T>selectCursor(command.getName(), param);
    }
    return result;
  }

  private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
    Object collection = config.getObjectFactory().create(method.getReturnType());
    MetaObject metaObject = config.newMetaObject(collection);
    metaObject.addAll(list);
    return collection;
  }

  @SuppressWarnings("unchecked")
  private <E> Object convertToArray(List<E> list) {
    Class<?> arrayComponentType = method.getReturnType().getComponentType();
    Object array = Array.newInstance(arrayComponentType, list.size());
    if (arrayComponentType.isPrimitive()) {
      for (int i = 0; i < list.size(); i++) {
        Array.set(array, i, list.get(i));
      }
    return array;
    } else {
      return list.toArray((E[])array);
    }
  }

  private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
    Map<K, V> result;
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey(), rowBounds);
    } else {
      result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey());
    }
    return result;
  }

  public static class ParamMap<V> extends HashMap<String, V> {

    private static final long serialVersionUID = -2212268410512043556L;

    @Override
    public V get(Object key) {
      if (!super.containsKey(key)) {
        throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
      }
      return super.get(key);
    }

  }

  /** sql命令 */
  public static class SqlCommand {

    /**
     * MappedStatement#getId()方法获得的标识
     * */
    private final String name;
    /** SQL命令类型
     *  UNKNOWN, INSERT, UPDATE, DELETE, SELECT, FLUSH;
     * */
    private final SqlCommandType type;

    /**
     *
     * @param configuration config配置文件的配置信息
     * @param mapperInterface mapper接口
     * @param method 接口方法
     */
    public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
      // 获取方法名称
      final String methodName = method.getName();
      //  得到目标方法所在类对应的Class对象
      final Class<?> declaringClass = method.getDeclaringClass();
      // 获得MappedStatement对象
      MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass,
          configuration);
      if (ms == null) {
        if(method.getAnnotation(Flush.class) != null){
          name = null;
          type = SqlCommandType.FLUSH;
        } else {
          throw new BindingException("Invalid bound statement (not found): "
              + mapperInterface.getName() + "." + methodName);
        }
      } else {
        name = ms.getId();
        type = ms.getSqlCommandType();
        if (type == SqlCommandType.UNKNOWN) {
          throw new BindingException("Unknown execution method for: " + name);
        }
      }
    }

    public String getName() {
      return name;
    }

    public SqlCommandType getType() {
      return type;
    }

    /**
     * 获得MappedStatement对象
     *
     * @param mapperInterface mapper接口
     * @param methodName 方法名称
     * @param declaringClass 方法所在的类
     * @param configuration 配置信息
     * @return MappedStatement对象
     */
    private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
        Class<?> declaringClass, Configuration configuration) {
      // 获得编号
      String statementId = mapperInterface.getName() + "." + methodName;
      // 如果在配置文件中存在该编号，
      if (configuration.hasStatement(statementId)) {
        // 获得MappedStatement对象
        return configuration.getMappedStatement(statementId);
        // 如果没有。并且当前方法就是declaringClass声明的，则说明真的找不到了，则返回null
      } else if (mapperInterface.equals(declaringClass)) {
        return null;
      }
      // 遍历父接口，继续获得MappedStatement对象
      for (Class<?> superInterface : mapperInterface.getInterfaces()) {
        /**
         * class1.isAssignableFrom(class2)：class2是不是class1的子类或接口
         * 判定此 Class 对象所表示的类或接口与指定的 Class 参数所表示的类或接口是否相同，或是否是其超类或超接口
         * true:是
         * false：不是
         * */
        if (declaringClass.isAssignableFrom(superInterface)) {
          // 如果是的话，则递归继续获得MappedStatement对象，有可能该方法是定义在父类中的
          MappedStatement ms = resolveMappedStatement(superInterface, methodName,
              declaringClass, configuration);
          if (ms != null) {
            return ms;
          }
        }
      }
      return null;
    }
  }

  /** 方法签名 */
  public static class MethodSignature {

    /** 返回类型是否为集合 */
    private final boolean returnsMany;

    /** 返回类型是否为Map */
    private final boolean returnsMap;

    /**  返回类型是否为void */
    private final boolean returnsVoid;

    /** 返回类型是否为Cursor */
    private final boolean returnsCursor;

    /** 返回类型是否为Optional */
    private final boolean returnsOptional;

    /** 返回类型 */
    private final Class<?> returnType;

    /** 返回方法上的key,返回类型为Map才有*/
    private final String mapKey;

    /** 获得在方法参数中的位置 null 说明不存在这个类型 */
    private final Integer resultHandlerIndex;

    /** 获得在方法参数上的位置 null 说明不存在这个类型*/
    private final Integer rowBoundsIndex;

    /** ParamNameResolver 对象 --> 参数名解析器*/
    private final ParamNameResolver paramNameResolver;

    /**
     * 构造方法
     *
     * @param configuration 配置文件
     * @param mapperInterface mapper接口
     * @param method 方法
     */
    public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
      // 初始化returnType属性  解析方法返回类型
      Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
      if (resolvedReturnType instanceof Class<?>) {
        this.returnType = (Class<?>) resolvedReturnType;
      } else if (resolvedReturnType instanceof ParameterizedType) {
        this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
      } else {
        this.returnType = method.getReturnType();
      }
      // 初始化returnsVoid属性
      this.returnsVoid = void.class.equals(this.returnType);
      // 初始化returnsMany属性
      this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
      // 初始化returnsCursor属性
      this.returnsCursor = Cursor.class.equals(this.returnType);
      // 初始化returnsOptional属性
      this.returnsOptional = Optional.class.equals(this.returnType);
      // 初始化mapKey属性
      this.mapKey = getMapKey(method);
      //初始化returnsMap属性
      this.returnsMap = this.mapKey != null;
      // 初始化rowBoundsIndex属性
      this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
      // 初始化resultHandlerIndex 属性
      this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
      // 初始化paramNameResolver属性
      this.paramNameResolver = new ParamNameResolver(configuration, method);
    }

    /**
     * 获得SQL通用参数
     *
     * @param args 参数
     * @return Object
     */
    public Object convertArgsToSqlCommandParam(Object[] args) {
      return paramNameResolver.getNamedParams(args);
    }

    public boolean hasRowBounds() {
      return rowBoundsIndex != null;
    }

    public RowBounds extractRowBounds(Object[] args) {
      return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
    }

    public boolean hasResultHandler() {
      return resultHandlerIndex != null;
    }

    public ResultHandler extractResultHandler(Object[] args) {
      return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
    }

    public String getMapKey() {
      return mapKey;
    }

    public Class<?> getReturnType() {
      return returnType;
    }

    public boolean returnsMany() {
      return returnsMany;
    }

    public boolean returnsMap() {
      return returnsMap;
    }

    public boolean returnsVoid() {
      return returnsVoid;
    }

    public boolean returnsCursor() {
      return returnsCursor;
    }

    /**
     * return whether return type is {@code java.util.Optional}
     * @return return {@code true}, if return type is {@code java.util.Optional}
     * @since 3.5.0
     */
    public boolean returnsOptional() {
      return returnsOptional;
    }

    /**
     * 获得指定参数类型在方法中的位置
     * @param method 方法
     * @param paramType 参数类型
     * @return 方法在参数中的位置
     */
    private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
      Integer index = null;
      // 遍历方法参数
      final Class<?>[] argTypes = method.getParameterTypes();
      for (int i = 0; i < argTypes.length; i++) {
        // 如果类型符合
        if (paramType.isAssignableFrom(argTypes[i])) {
          if (index == null) {
            // 获取第一次的位置
            index = i;
            // 如果类型重复类，则抛出BindingException异常
          } else {
            throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
          }
        }
      }
      return index;
    }

    /**
     *
     * @param method 方法
     * @return 获得Map的键值
     */
    private String getMapKey(Method method) {
      String mapKey = null;
      // 如果返回的类型为map
      if (Map.class.isAssignableFrom(method.getReturnType())) {
        // 使用@MapKey的注解
        final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
        // 获得@MapKey注解的键值
        if (mapKeyAnnotation != null) {
          mapKey = mapKeyAnnotation.value();
        }
      }
      return mapKey;
    }
  }

}
