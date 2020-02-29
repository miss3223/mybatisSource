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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 *
 * mybtais-config文件的解析过程
 */
public class XMLConfigBuilder extends BaseBuilder {

  /** 是否已经解析 */
  private boolean parsed;

  /** 基于XPathParser 解析器  用来解析mybatis-config 和**Mapper.xml文件 */
  private final XPathParser parser;

  /** 环境  */
  private String environment;

  /** ReflectorFactory对象  Reflector工厂接口，用于创建和缓存Reflector对象 */
  private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

  public XMLConfigBuilder(Reader reader) {
    this(reader, null, null);
  }

  public XMLConfigBuilder(Reader reader, String environment) {
    this(reader, environment, null);
  }

  public XMLConfigBuilder(Reader reader, String environment, Properties props) {
    this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  public XMLConfigBuilder(InputStream inputStream) {
    this(inputStream, null, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment) {
    this(inputStream, environment, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
    this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
    // 创建Configuration对象
    super(new Configuration());
    ErrorContext.instance().resource("SQL Mapper Configuration");
    // 设置Variables属性 settings下的properties属性
    this.configuration.setVariables(props);
    this.parsed = false;
    this.environment = environment;
    this.parser = parser;
  }

  /**
   * 解析xml对象
   * @return Configuration对象
   */
  public Configuration parse() {
    // 如果已经解析了，则抛出BuilderException异常
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    // 标记为已解析
    parsed = true;
    // 对xml文件进行解析 解析<configuration/>节点
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }

  /** 对xml文件的<configuration/>节点进行解析 **/
  private void parseConfiguration(XNode root) {
    try {
      //issue #117 read properties first
      //<properties/>节点 元素
      propertiesElement(root.evalNode("properties"));
      // <settings/>元素
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      // 加载自定义VFS实现类
      loadCustomVfs(settings);
      //<typeAliases/>元素
      typeAliasesElement(root.evalNode("typeAliases"));
      // <plugins/>插件
      pluginElement(root.evalNode("plugins"));
      // <objectFactory/>对象工厂
      objectFactoryElement(root.evalNode("objectFactory"));
      // <objectWrapperFactory/>对象包装工厂
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      // <reflectorFactory/>反射工厂
      reflectorFactoryElement(root.evalNode("reflectorFactory"));
      // 将settings保存到configuration中
      settingsElement(settings);
      // read it after objectFactory and objectWrapperFactory issue #631
      // 解析<environments/>标签 环境
      environmentsElement(root.evalNode("environments"));
      // 解析<databaseIdProvider/>标签 厂商标签
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));
      //类型转换器
      typeHandlerElement(root.evalNode("typeHandlers"));
      // 映射器
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }

  /**
   * 将settings标签解析成Properties对象
   * @param context settings标签的内容
   * @return Properties对象
   */
  private Properties settingsAsProperties(XNode context) {
    // 如果为null，则创建一个Properties对象返回
    if (context == null) {
      return new Properties();
    }
    // 将子标签，解析成Properties对象
    Properties props = context.getChildrenAsProperties();
    // Check that all settings are known to the configuration class
    // 校验每个属性，在Configuration中，有相应的setting方法，否则抛出异常
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
    for (Object key : props.keySet()) {
      if (!metaConfig.hasSetter(String.valueOf(key))) {
        throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    return props;
  }

  /**
   * 加载自定义VFS实现类
   * @param props Properties对象
   * @throws ClassNotFoundException 异常
   */
  private void loadCustomVfs(Properties props) throws ClassNotFoundException {
    // 获取vfsImpl属性
    String value = props.getProperty("vfsImpl");
    if (value != null) {
      // 使用，作为分隔符，拆成 VFS 类名的数组
      String[] clazzes = value.split(",");
      // 遍历VFS类名的数组
      for (String clazz : clazzes) {
        if (!clazz.isEmpty()) {
          @SuppressWarnings("unchecked")
          Class<? extends VFS> vfsImpl = (Class<? extends VFS>)Resources.classForName(clazz);
          // 设置到Configuration中
          configuration.setVfsImpl(vfsImpl);
        }
      }
    }
  }

  /**
   * 解析<typeAliases/>标签，将配置类注册到typeAliasRegistry中
   *
   * @param parent
   */
  private void typeAliasesElement(XNode parent) {
    if (parent != null) {
      // 提供两种别名设置的方法package和typeAlias
      //1、具体类的别名 2、包的别名设置
      // 所有的别名，无论是内置的还是自定义的，都一开始被保存在configuration.typeAliasRegistry中

      // 遍历子节点
      for (XNode child : parent.getChildren()) {
        // 指定为包的情况下，注册包下的每个类
        if ("package".equals(child.getName())) {
          String typeAliasPackage = child.getStringAttribute("name");
          configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
          // 指定为类的情况下，直接注册类和别名
        } else {
          String alias = child.getStringAttribute("alias");
          String type = child.getStringAttribute("type");
          try {
            // 获得类是否存在
            Class<?> clazz = Resources.classForName(type);
            // 注册到typeAliasRegistry中
            if (alias == null) {
              typeAliasRegistry.registerAlias(clazz);
            } else {
              typeAliasRegistry.registerAlias(alias, clazz);
            }
          } catch (ClassNotFoundException e) {
            throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
          }
        }
      }
    }
  }

  /**
   * 解析<plugins />标签，添加到configuration的interceptorChain中
   * @param parent
   * @throws Exception
   */
  private void pluginElement(XNode parent) throws Exception {
    if (parent != null) {
      // 遍历<plugins/> 标签
      for (XNode child : parent.getChildren()) {
        // 获取"interceptor"属性的值
        String interceptor = child.getStringAttribute("interceptor");
        Properties properties = child.getChildrenAsProperties();
        //将interceptor指定的名称解析为Interceptor类型
        Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
        interceptorInstance.setProperties(properties);
        // 插件在Configuration对象中的保存
        configuration.addInterceptor(interceptorInstance);
      }
    }
  }

  /**
   * 解析 <objectFactory /> 节点
   * @param context 节点内容
   * @throws Exception 异常
   */
  private void objectFactoryElement(XNode context) throws Exception {
    if (context != null) {
      // 获得objectFactory的实现类
      String type = context.getStringAttribute("type");
      // 获得Properties属性
      Properties properties = context.getChildrenAsProperties();
      // 创建ObjectFactory对象
      ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
      factory.setProperties(properties);
      // 设置 configuration 的ObjectFactory对象
      configuration.setObjectFactory(factory);
    }
  }

  /**
   * 解析 <objectWrapperFactory /> 节点
   * @param context  节点内容
   * @throws Exception 异常
   */
  private void objectWrapperFactoryElement(XNode context) throws Exception {
    if (context != null) {
      // 获取objectFactory的实现类
      String type = context.getStringAttribute("type");
      // 创建ObjectWrapperFactory对象
      ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
      // 设置configuration的objectWrapperFactory属性
      configuration.setObjectWrapperFactory(factory);
    }
  }

  /**
   * 解析 <reflectorFactory /> 节点
   * @param context 节点内容
   * @throws Exception 异常
   */
  private void reflectorFactoryElement(XNode context) throws Exception {
    if (context != null) {
      // 获得ReflectorFactory的实现类
       String type = context.getStringAttribute("type");
       // 创建ReflectorFactory对象
       ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
       // 设置configuration的reflectorFactory属性
       configuration.setReflectorFactory(factory);
    }
  }

  /**
   * 解析<properties/>标签成Properties对象
   * @param context  properties标签的内容
   * @throws Exception 异常
   */
  private void propertiesElement(XNode context) throws Exception {
    if (context != null) {
      // 将property的属性name和value值保存
      Properties defaults = context.getChildrenAsProperties();
      String resource = context.getStringAttribute("resource");
      String url = context.getStringAttribute("url");
      // 不能同时包含resource和url
      if (resource != null && url != null) {
        throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
      }
      // 读取本地Properties配置文件到defaults中
      if (resource != null) {
        defaults.putAll(Resources.getResourceAsProperties(resource));
      // 读取远程  Properties 配置文件到defaults中
      } else if (url != null) {
        defaults.putAll(Resources.getUrlAsProperties(url));
      }
      // 覆盖configuration中的Properties对象到defaults中
      Properties vars = configuration.getVariables();
      if (vars != null) {
        defaults.putAll(vars);
      }
      // 设置defaults到parser和configuration中
      parser.setVariables(defaults);
      configuration.setVariables(defaults);
    }
  }

  /** 设置<settins />到configuration属性中 */
  private void settingsElement(Properties props) throws Exception {
    configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
    configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
    configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
    configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
    configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
    configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
    configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
    configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
    configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
    configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
    configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
    configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
    configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
    configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
    configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
    configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
    configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
    configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
    @SuppressWarnings("unchecked")
    Class<? extends TypeHandler> typeHandler = (Class<? extends TypeHandler>)resolveClass(props.getProperty("defaultEnumTypeHandler"));
    configuration.setDefaultEnumTypeHandler(typeHandler);
    configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
    configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
    configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
    configuration.setLogPrefix(props.getProperty("logPrefix"));
    @SuppressWarnings("unchecked")
    Class<? extends Log> logImpl = (Class<? extends Log>)resolveClass(props.getProperty("logImpl"));
    configuration.setLogImpl(logImpl);
    configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
  }

  /** 解析<environments/> 标签 */
  private void environmentsElement(XNode context) throws Exception {
    if (context != null) {
      // 如果environment存在就从"default"获取默认的值
      if (environment == null) {
        environment = context.getStringAttribute("default");
      }
      // 遍历xnode节点
      for (XNode child : context.getChildren()) {
        String id = child.getStringAttribute("id");
        //查找匹配的environment
        if (isSpecifiedEnvironment(id)) {
          // 事务配置并创建事务工厂  解析<transactionManager />标签
          TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
          // 数据源配置加载并实例化数据源, 数据源是必备的 解析<dataSource />标签
          DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
          DataSource dataSource = dsFactory.getDataSource();
          // 创建Environment.Builder
          Environment.Builder environmentBuilder = new Environment.Builder(id)
              .transactionFactory(txFactory)
              .dataSource(dataSource);
          // 构造 Environment 对象，并设置到 configuration 中
          configuration.setEnvironment(environmentBuilder.build());
        }
      }
    }
  }

  /**
   * 解析<databaseIdProvider /> 标签
   * @param context 节点内容
   * @throws Exception 异常
   */
  private void databaseIdProviderElement(XNode context) throws Exception {
    DatabaseIdProvider databaseIdProvider = null;
    if (context != null) {
      // 获取db厂商信息
      String type = context.getStringAttribute("type");
      // awful patch to keep backward compatibility
      if ("VENDOR".equals(type)) {
          type = "DB_VENDOR";
      }
      // Properties对象
      Properties properties = context.getChildrenAsProperties();
      // 创建DatabaseIdProvider对象
      databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
      // 设置属性
      databaseIdProvider.setProperties(properties);
    }
    Environment environment = configuration.getEnvironment();
    //当environment和databaseIdProvider都不为null时
    if (environment != null && databaseIdProvider != null) {
      // 获取数据库标识
      String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
      // 将数据库标识存到configuration中
      configuration.setDatabaseId(databaseId);
    }
  }

  /**
   * 解析<transactionManager />标签
   * @param context 标签内容
   * @return TransactionFactory对象
   * @throws Exception 异常
   */
  private TransactionFactory transactionManagerElement(XNode context) throws Exception {
    if (context != null) {
      // 获取事务类型
      String type = context.getStringAttribute("type");
      // 获得Properties属性
      Properties props = context.getChildrenAsProperties();
      // 创建TransactionFactory对象
      TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
      // 设置Properties属性
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a TransactionFactory.");
  }

  /**
   *  解析<dataSource />标签
   * @param context 节点内容
   * @return DataSourceFactory
   * @throws Exception 异常
   */
  private DataSourceFactory dataSourceElement(XNode context) throws Exception {
    if (context != null) {
      // 获得datasource的类型
      String type = context.getStringAttribute("type");
      // 获得Properties属性
      Properties props = context.getChildrenAsProperties();
      // 创建DataSourceFactory对象
      DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
      // 设置Properties属性
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a DataSourceFactory.");
  }

  /**
   * 解析<typeHandler />标签
   * @param parent 节点内容
   * @throws Exception 异常
   */
  private void typeHandlerElement(XNode parent) throws Exception {
    if (parent != null) {
      // 遍历子节点
      for (XNode child : parent.getChildren()) {
        // 如果是package标签，则扫描该包
        if ("package".equals(child.getName())) {
          String typeHandlerPackage = child.getStringAttribute("name");
          typeHandlerRegistry.register(typeHandlerPackage);
          // 如果是typeHandler标签，则注册该typeHandler信息
        } else {
          // 获得javaType，jdbcType和handler
          String javaTypeName = child.getStringAttribute("javaType");
          String jdbcTypeName = child.getStringAttribute("jdbcType");
          String handlerTypeName = child.getStringAttribute("handler");
          Class<?> javaTypeClass = resolveClass(javaTypeName);
          JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
          Class<?> typeHandlerClass = resolveClass(handlerTypeName);
          // 注册typeHandler
          if (javaTypeClass != null) {
            if (jdbcType == null) {
              typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
            } else {
              typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
            }
          } else {
            typeHandlerRegistry.register(typeHandlerClass);
          }
        }
      }
    }
  }

  /**
   * 解析<mappers />标签
   * @param parent 节点内容
   * @throws Exception 异常
   */
  private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        // 如果要同时使用package自动扫描和通过mapper明确指定要加载的mapper，一定要确保package自动扫描的范围不包含明确指定的mapper，
        // 否则在通过package扫描的interface的时候，尝试加载对应xml文件的loadXmlResource()的逻辑中出现判重出错，
        // 报org.apache.ibatis.binding.BindingException异常，即使xml文件中包含的内容和mapper接口中包含的语句不重复也会出错，
        // 包括加载mapper接口时自动加载的xml mapper也一样会出错。

        //mybatis提供了两类配置mapper的方法：
        // 第一类是使用package自动搜索的模式，这样指定package下所有接口都会被注册为mapper
        if ("package".equals(child.getName())) {
          String mapperPackage = child.getStringAttribute("name");
          configuration.addMappers(mapperPackage);
        } else {
          //第二种 明确指定mapper，这又可以通过resource、url或者class进行细分
          String resource = child.getStringAttribute("resource");
          String url = child.getStringAttribute("url");
          String mapperClass = child.getStringAttribute("class");
          // 使用相对于类路径的资源引用
          if (resource != null && url == null && mapperClass == null) {
            ErrorContext.instance().resource(resource);
            InputStream inputStream = Resources.getResourceAsStream(resource);
            //创建XMLMapperBuilder对象,解析**Mapper.xml文件
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            // 调用parse（）进行具体的解析
            mapperParser.parse();
            // 使用完全限定资源定位符（URL）
          } else if (resource == null && url != null && mapperClass == null) {
            ErrorContext.instance().resource(url);
            InputStream inputStream = Resources.getUrlAsStream(url);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
            mapperParser.parse();
            // 使用映射器接口实现类的完全限定类名
          } else if (resource == null && url == null && mapperClass != null) {
            Class<?> mapperInterface = Resources.classForName(mapperClass);
            configuration.addMapper(mapperInterface);
          } else {
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }
  }

  /**
   * 判断environment和id是否匹配
   * @param id id属性值
   * @return
   */
  private boolean isSpecifiedEnvironment(String id) {
    if (environment == null) {
      throw new BuilderException("No environment specified.");
    } else if (id == null) {
      throw new BuilderException("Environment requires an id attribute.");
    } else if (environment.equals(id)) {
      return true;
    }
    return false;
  }

}
