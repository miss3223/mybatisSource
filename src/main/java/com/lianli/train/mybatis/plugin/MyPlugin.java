package com.lianli.train.mybatis.plugin;


import com.lianli.train.mybatis.core.MyTypeHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Properties;

/**
 * @author lianli Date: 2019-04-16 Time: 4:28 PM
 * @version $Id$
 *
 * @Intercepts 说明它是一个拦截器
 * @Signature 是注册拦截器签名的地方，只有签名满足条件才能拦截
 * type:四大对象中的一个。
 * 四大对象：
 * Executor（执行器） StatementHandler（数据会话器） ParameterHandler（参数处理器）ResultSetHandler（结果处理器）
 * method：要拦截的四大对象中的某一种方法
 * args:拦截的方法中参数
 */
@Intercepts({@Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class,Integer.class})})
public class MyPlugin implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger(MyTypeHandler.class);
    private Properties pros = null;

    /**
     * 插件方法，它将代替StatementHandler的prepare方法
     * @param invocation 入参
     * @return 返回以后的PreparedStatement
     * @throws Throwable 异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        // 进行绑定
        MetaObject metaStatementHandler = SystemMetaObject.forObject(statementHandler);
        Object object = null;
        /**
         * 分离代理对象链（由于目标类可能被多个拦截器拦截，从而形成多次代理，通过循环可以分离出最原始的目标类）
         */
        while(metaStatementHandler.hasGetter("h")){
            object = metaStatementHandler.getValue("h");
            metaStatementHandler = SystemMetaObject.forObject(object);
        }
        statementHandler = (StatementHandler) object;
        /**
         * delegate.boundSql.sql
         * 在StatementHandler下有一个属性boundSql，boundSql有一个属性sql，
         * 固通过路径delegate.boundSql.sql去获取或者修改运行时的sql
         */
        String sql = (String) metaStatementHandler.getValue("delegate.boundSql.sql");
      //  Long parameterObject = (Long) metaStatementHandler.getValue("delegate.boundSql.parameterObject");
        logger.info("执行的SQL：【"+sql+"】");
      //  logger.info("参数：【"+parameterObject+"】");
        logger.info("before ......");
        Object proceed = invocation.proceed();
        logger.info("after ......");
        return proceed;
    }

    @Override
    public Object plugin(Object target) {
        // 采用系统默认的Plugin.wrap方法生成
        return Plugin.wrap(target,this);
    }

    /**
     * 设置参数，Mybatis初始化时，就会生成插件实例，并且调用这个方法
     * @param properties
     */
    @Override
    public void setProperties(Properties properties) {

        this.pros = properties;
        logger.info("dbType = " + this.pros.get("dbType"));

    }
}
