package com.lianli.train.mybatis.param;

import com.lianli.train.mybatis.annotation.Column;
import java.io.Serializable;
import java.util.List;

/**
 * @author lianli Date: 2019-06-05 Time: 5:23 PM
 * @version $Id$
 */
public class NewsSearchParam implements Serializable {

    private static final long serialVersionUID = 4135311459179818913L;

    @Column(field = "id" ,in = "id")
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }


    @Override
    public String toString() {
        return "NewsSearchParam{" +
            "ids=" + ids +
            '}';
    }
}
