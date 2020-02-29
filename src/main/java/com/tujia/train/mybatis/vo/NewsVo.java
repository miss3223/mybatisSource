package com.tujia.train.mybatis.vo;

import com.tujia.train.mybatis.annotation.Column;
import java.io.Serializable;
import java.util.Date;


/**
 * @author lianli Date: 2019-06-05 Time: 3:34 PM
 * @version $Id$
 */
public class NewsVo  implements Serializable {

    private static final long serialVersionUID = 3762216141135364436L;

    private Long id;

    private String title;

    private Long authorId;
    private String content;
    private Date createTime;
    private Date updateTime;
    private String image;
    private String imageName;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Override
    public String toString() {
        return "NewsVo{" +
            "id=" + id +
            ", title='" + title + '\'' +
            ", authorId=" + authorId +
            ", content='" + content + '\'' +
            ", createTime=" + createTime +
            ", updateTime=" + updateTime +
            ", image='" + image + '\'' +
            ", imageName='" + imageName + '\'' +
            '}';
    }
}
