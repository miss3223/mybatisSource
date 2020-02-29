package com.tujia.train.mybatis.po;

/**
 * @author jianhong.li Date: 2017-07-17 Time: 7:33 PM
 * @version $Id$
 */
public class Worker {
    private String name;
    private String company;
    private int age;
    private Integer workNo;
    private long addressId;
    private Address address;

    public long getAddressId() {
        return addressId;
    }

    public void setAddressId(long addressId) {
        this.addressId = addressId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Integer getWorkNo() {
        return workNo;
    }

    public void setWorkNo(Integer workNo) {
        this.workNo = workNo;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
