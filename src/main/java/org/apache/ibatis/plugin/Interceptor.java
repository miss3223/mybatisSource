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
package org.apache.ibatis.plugin;

import java.util.Properties;

/**
 * @author Clinton Begin
 */
public interface Interceptor {

  /**
   * 直接覆盖拦截对象原有的方法，是插件的核心方法
   * @param invocation 反射调度原来对象的方法
   * @return 返回对象
   * @throws Throwable 异常信息
   */
  Object intercept(Invocation invocation) throws Throwable;

  /**
   * 给被拦截对象生成一个代理对象，并返回它
   * @param target 被拦截的对象
   * @return 返回的拦截对象
   */
  Object plugin(Object target);

  /**
   * 允许在plugin元素中配置所需参数，方法在插件初始化时就被调用了一次，然后把插件对象存入到配置中，以便后面再取出
   * @param properties  参数
   */
  void setProperties(Properties properties);

}
