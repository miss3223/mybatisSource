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
package org.apache.ibatis.reflection.property;

import java.lang.reflect.Field;

import org.apache.ibatis.reflection.Reflector;

/**
 *
 * @author Clinton Begin
 *
 * 属性复制器
 */
public final class PropertyCopier {

  //注意这里的构造方法是private的， 是为了防止静态方法实例话
  private PropertyCopier() {
    // Prevent Instantiation of Static Class
  }

  /**
   * 将sourceBean的属性复制到destinationBean中
   * @param type 指定类
   * @param sourceBean 来源Bean对象
   * @param destinationBean 目标Bean对象
   */
  public static void copyBeanProperties(Class<?> type, Object sourceBean, Object destinationBean) {
    Class<?> parent = type;
    // 循环，从当前类开始，不断复制到父类，直到父类不存在
    while (parent != null) {
      // 获取parent类定义的属性
      final Field[] fields = parent.getDeclaredFields();
      for(Field field : fields) {
        try {
          try {
            // 从sourceBean中复制到destinationBean去
            field.set(destinationBean, field.get(sourceBean));
          } catch (IllegalAccessException e) {
            // 如果属性不可访问
            if (Reflector.canControlMemberAccessible()) {
              // 设置为可访问
              field.setAccessible(true);
              // 从sourceBean中复制到destinationBean去
              field.set(destinationBean, field.get(sourceBean));
            } else {
              throw e;
            }
          }
        } catch (Exception e) {
          // Nothing useful to do, will only fail on final fields, which will be ignored.
        }
      }
      // 获得父类
      parent = parent.getSuperclass();
    }
  }

}
