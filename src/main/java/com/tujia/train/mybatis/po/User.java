package com.tujia.train.mybatis.po;

import java.util.List;

/**
 * @author jianhong.li Date: 2017-07-16 Time: 6:00 PM
 * @version $Id$
 */
public class User {

    private Long id;
    private String name;
    private int age;
    private int userStatus;
    private Object isDelete;
    private int isDeleteInt;
    private Boolean isDeleteBool;
    private List<String> course;

    public int getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(int userStatus) {
        this.userStatus = userStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Object getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Object isDelete) {
        this.isDelete = isDelete;
    }

    public int getIsDeleteInt() {
        return isDeleteInt;
    }

    public void setIsDeleteInt(int isDeleteInt) {
        this.isDeleteInt = isDeleteInt;
    }

    public Boolean getDeleteBool() {
        return isDeleteBool;
    }

    public void setDeleteBool(Boolean deleteBool) {
        isDeleteBool = deleteBool;
    }

    public List<String> getCourse() {
        return course;
    }

    public void setCourse(List<String> course) {
        this.course = course;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", userStatus=" + userStatus +
                ", isDelete=" + isDelete +
                ", isDeleteInt=" + isDeleteInt +
                ", isDeleteBool=" + isDeleteBool +
                ", course=" + course +
                '}';
    }
}
