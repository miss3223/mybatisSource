package com.lianli.train.mybatis.po;

import com.lianli.train.mybatis.annotation.Column;
import java.io.Serializable;
import java.util.Date;

/**
 * @author lianli Date: 2019-06-03 Time: 9:47 PM
 * @version $Id$
 */
public class Authors implements Serializable {

    private static final long serialVersionUID = 7466277930718238919L;


    @Column(field = "id")
    private Long id;
    @Column(field = "author")
    private String author;
    @Column(field = "create_time")
    private Date createTime;
    @Column(field = "sex")
    private Integer sex;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    @Override
    public String toString() {
        return "Authors{" +
            "id=" + id +
            ", author='" + author + '\'' +
            ", createTime=" + createTime +
            ", sex=" + sex +
            '}';
    }
}
