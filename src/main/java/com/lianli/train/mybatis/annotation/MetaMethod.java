package com.lianli.train.mybatis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author lianli Date: 2019-06-06 Time: 5:43 PM
 * @version $Id$
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface MetaMethod {

    String tableName() default "";

    String[] like() default "";

    String group() default "";

    String having() default "";

    String order() default "";

    String orderColumn() default "";


}
