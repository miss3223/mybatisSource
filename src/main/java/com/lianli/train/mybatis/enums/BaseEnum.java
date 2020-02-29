package com.lianli.train.mybatis.enums;

import java.io.Serializable;

/**
 * @author lianli Date: 2019-06-04 Time: 4:53 PM
 * @version $Id$
 */
public interface BaseEnum extends Serializable {

    public abstract int getCode();

    public abstract String getName();

    public abstract String getLabel();
}
