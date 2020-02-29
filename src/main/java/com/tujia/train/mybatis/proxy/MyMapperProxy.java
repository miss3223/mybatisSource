package com.tujia.train.mybatis.proxy;

import com.tujia.train.mybatis.po.News;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.ss.usermodel.DateUtil;


/**
 * @author lianli Date: 2019-05-30 Time: 3:43 PM
 * @version $Id$
 */
public class MyMapperProxy implements InvocationHandler {

    public <T> T newInstance(Class<T> cls){
        return (T) Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(Object.class.equals(method.getDeclaringClass())){
            return method.invoke(this,args);
        }
        return new News((Long)args[0], 1L, "体育新闻", "科比",
                        DateUtil.parseYYYYMMDDDate("2019-05-30"),  DateUtil.parseYYYYMMDDDate("2019-05-30"));
    }
}
