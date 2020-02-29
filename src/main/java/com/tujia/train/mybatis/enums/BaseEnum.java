package com.tujia.train.mybatis.enums;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
