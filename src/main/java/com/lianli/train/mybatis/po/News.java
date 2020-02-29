package com.lianli.train.mybatis.po;

import com.lianli.train.mybatis.annotation.Column;
import java.io.Serializable;
import java.util.Date;

/**
 * @author lianli Date: 2019-05-29 Time: 3:53 PM
 * @version $Id$
 */
public class News implements Serializable {


    private static final long serialVersionUID = 4089163849888922269L;

    @Column(field = "id")
    private Long id;

    @Column(field = "author_id",table = "news")
    private Long authorId;

    @Column(field = "title")
    private String title;

    @Column(field = "content")
    private String content;

    @Column(field = "create_time")
    private Date createTime;

    @Column(field = "update_time")
    private Date updateTime;

    public News() {
    }

    public News(Long id, Long authorId, String title, String content, Date createTime, Date updateTime) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }


    @Override
    public String toString() {
        return "News{" +
            "id=" + id +
            ", authorId=" + authorId +
            ", title='" + title + '\'' +
            ", content='" + content + '\'' +
            ", createTime=" + createTime +
            ", updateTime=" + updateTime +
            '}';
    }
}
